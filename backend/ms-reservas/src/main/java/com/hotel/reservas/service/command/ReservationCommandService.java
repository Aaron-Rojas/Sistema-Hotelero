package com.hotel.reservas.service.command;

import com.hotel.reservas.dto.event.ReservationPaymentEvent;
import com.hotel.reservas.dto.request.CreateReservationRequest;
import com.hotel.reservas.dto.response.ReservationResponse;
import com.hotel.reservas.entity.Reservation;
import com.hotel.reservas.entity.ReservationStatus;
import com.hotel.reservas.exception.OverbookingConflictException;
import com.hotel.reservas.repository.ReservationRepository;
import com.hotel.reservas.service.RedisCacheService;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Servicio del Lado Command (CQRS - Escritura y Mutación de Estado).
 * 
 * ¿CÓMO funciona?
 * Ejecuta exclusivamente mutaciones de estado sobre la base de datos relacional (PostgreSQL).
 * Aplica control de concurrencia pesimista (@Lock), actualiza atómicamente el inventario en caché (DECR)
 * y emite el evento de dominio hacia Amazon SQS para procesamiento asíncrono.
 * 
 * ¿POR QUÉ este aislamiento?
 * Al separar los comandos de las consultas, garantizamos transacciones ACID estrictas
 * para prevenir el overbooking sin sobrecargar ni interferir con las consultas de lectura masiva.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCommandService {

    private final ReservationRepository reservationRepository;
    private final RedisCacheService redisCacheService;
    private final SqsTemplate sqsTemplate;

    @Value("${cloud.aws.sqs.queue.reservation-events:${aws.sqs.queue-name:hotel-reservation-events-queue}}")
    private String reservationEventsQueue;

    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        log.info("[CQRS Command] Initiating reservation for roomId: {}, guest: {}", request.getRoomId(), request.getGuestEmail());

        /*
         * 1. Consulta Rápida en Redis (Fast-Reject < 5ms):
         *    Si la caché reporta stock agotado (Boolean.FALSE), se rechaza de inmediato
         *    sin consumir conexiones de HikariCP ni bloquear filas en PostgreSQL.
         */
        Boolean isAvailableInCache = redisCacheService.checkAvailabilityInCache(
                request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

        if (Boolean.FALSE.equals(isAvailableInCache)) {
            log.warn("[CQRS Command] Fast-reject via Redis cache (< 5ms): roomId: {} is unavailable", request.getRoomId());
            throw new OverbookingConflictException("Room " + request.getRoomId() + " is already booked for the selected dates (cache hit)");
        }

        /*
         * 2. Bloqueo Pesimista en PostgreSQL (SELECT ... FOR UPDATE):
         *    Garantiza 0% Overbooking bloqueando filas colisionantes hasta el commit de la transacción.
         */
        List<Reservation> overlapping = reservationRepository.findOverlappingReservationsWithLock(
                request.getRoomId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                ReservationStatus.CANCELLED
        );

        if (!overlapping.isEmpty()) {
            redisCacheService.setRoomStock(request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate(), 0);
            log.warn("[CQRS Command] Overbooking conflict detected via pessimistic lock for roomId: {}", request.getRoomId());
            throw new OverbookingConflictException("Room " + request.getRoomId() + " is already booked for the selected dates");
        }

        /*
         * 3. Persistencia en estado PENDING:
         *    La reserva se almacena con estado PENDING para el posterior cobro asíncrono.
         */
        Reservation reservation = Reservation.builder()
                .roomId(request.getRoomId())
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .totalPrice(request.getTotalPrice())
                .status(ReservationStatus.PENDING)
                .build();

        Reservation savedReservation = reservationRepository.save(reservation);
        log.info("[CQRS Command] Reservation saved with ID: {} and status: {}", savedReservation.getId(), savedReservation.getStatus());

        /*
         * 4. Operación Atómica DECR en Redis:
         *    Actualiza inmediatamente el inventario en caché para reflejar el bloqueo temporal.
         */
        redisCacheService.decrementStock(request.getRoomId(), request.getCheckInDate(), request.getCheckOutDate());

        /*
         * 5. Despacho Asíncrono a Amazon SQS:
         *    Se publica el evento para que el PaymentSqsWorker procese el pago desacoplado.
         */
        ReservationPaymentEvent paymentEvent = ReservationPaymentEvent.builder()
                .reservationId(savedReservation.getId())
                .roomId(savedReservation.getRoomId())
                .amount(savedReservation.getTotalPrice())
                .checkInDate(savedReservation.getCheckInDate())
                .checkOutDate(savedReservation.getCheckOutDate())
                .paymentToken("TOK-" + UUID.randomUUID())
                .timestamp(Instant.now())
                .build();

        try {
            sqsTemplate.send(to -> to.queue(reservationEventsQueue).payload(paymentEvent));
            log.info("[CQRS Command] Event published to SQS: {}", reservationEventsQueue);
        } catch (Exception ex) {
            log.error("[CQRS Command] Failed to publish event to SQS queue {}. Error: {}", reservationEventsQueue, ex.getMessage());
        }

        return mapToResponse(savedReservation);
    }

    private ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .roomId(reservation.getRoomId())
                .guestName(reservation.getGuestName())
                .guestEmail(reservation.getGuestEmail())
                .checkInDate(reservation.getCheckInDate())
                .checkOutDate(reservation.getCheckOutDate())
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus())
                .version(reservation.getVersion())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}

