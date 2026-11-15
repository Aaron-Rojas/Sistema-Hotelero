package com.hotel.reservas.worker;

import com.hotel.reservas.dto.event.ReservationPaymentEvent;
import com.hotel.reservas.entity.Reservation;
import com.hotel.reservas.entity.ReservationStatus;
import com.hotel.reservas.repository.ReservationRepository;
import com.hotel.reservas.service.RedisCacheService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Worker asíncrono consumidor de eventos de Amazon SQS.
 * 
 * ¿CÓMO funciona el aislamiento con @Profile("!api")?
 * Permite separar los workloads en contenedores independientes usando la misma imagen:
 * - Contenedores API (SPRING_PROFILES_ACTIVE=api): No activan este listener, dedicando 100% de CPU/RAM a peticiones HTTP.
 * - Contenedores Worker (SPRING_PROFILES_ACTIVE=worker): Activan el listener y procesan colas en background.
 * 
 * ¿POR QUÉ es crítica la Idempotencia aquí?
 * Amazon SQS opera bajo el modelo 'at-least-once delivery' (entrega al menos una vez).
 * Si un mensaje se reintenta o duplica por la red, la verificación del estado PENDING
 * evita cobros dobles o corrupciones en el inventario de Redis.
 */
@Component
@Profile("!api")
@RequiredArgsConstructor
@Slf4j
public class PaymentSqsWorker {

    private final ReservationRepository reservationRepository;
    private final RedisCacheService redisCacheService;

    @SqsListener("${cloud.aws.sqs.queue.reservation-events:${aws.sqs.queue-name:hotel-reservation-events-queue}}")
    @Transactional
    public void processPaymentEvent(ReservationPaymentEvent event) {
        log.info("SQS Worker received payment event: {}", event);

        // 1. Verificación de Idempotencia
        Optional<Reservation> optionalReservation = reservationRepository.findById(event.getReservationId());
        if (optionalReservation.isEmpty()) {
            log.error("Integrity check failed: Reservation ID {} does not exist in PostgreSQL. Discarding message.",
                    event.getReservationId());
            return;
        }

        Reservation reservation = optionalReservation.get();

        // Si la reserva ya no está en PENDING, es un mensaje duplicado o reprocesado: se descarta
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            log.warn("Idempotency guard: Reservation ID {} is already processed with status {}. Skipping event.",
                    reservation.getId(), reservation.getStatus());
            return;
        }

        // 2. Simulación de Pasarela Bancaria
        boolean isApproved = simulateBankPaymentGateway(event);

        if (isApproved) {
            // 3.A Transición Exitosa: CONFIRMED
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.save(reservation);
            log.info("Payment APPROVED for Reservation ID: {}. Status transitioned to CONFIRMED.", reservation.getId());
        } else {
            // 3.B Transición Fallida / Rechazo: CANCELLED + Compensación de Inventario
            reservation.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(reservation);
            log.warn("Payment REJECTED for Reservation ID: {}. Status transitioned to CANCELLED.", reservation.getId());

            /*
             * Transacción Compensatoria (Patrón Saga):
             * Al cancelarse el pago, se ejecuta 'incrementStock' (INCR) en Redis para
             * devolver el stock al espejo de caché en tiempo real de forma inmediata.
             */
            redisCacheService.incrementStock(event.getRoomId(), event.getCheckInDate(), event.getCheckOutDate());
            log.info("Compensating transaction completed: Room ID {} stock restored in Redis.", event.getRoomId());
        }
    }

    /**
     * Simulación determinística y probabilística de la pasarela de pagos:
     * - Tokens que contengan 'FAIL' o montos superiores a $5,000 simulan rechazo bancario.
     * - Cualquier otro caso aprueba el pago exitosamente (90%+ tasa de éxito).
     */
    private boolean simulateBankPaymentGateway(ReservationPaymentEvent event) {
        if (event.getPaymentToken() != null && event.getPaymentToken().contains("FAIL")) {
            log.info("Simulated bank rejection: token explicitly marked as FAIL.");
            return false;
        }

        if (event.getAmount() != null && event.getAmount().compareTo(BigDecimal.valueOf(5000.00)) > 0) {
            log.info("Simulated bank rejection: amount exceeds allowed transactional limit.");
            return false;
        }

        return true;
    }
}

