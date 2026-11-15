package com.hotel.reservas.service.query;

import com.hotel.reservas.dto.response.ReservationResponse;
import com.hotel.reservas.entity.Reservation;
import com.hotel.reservas.entity.ReservationStatus;
import com.hotel.reservas.exception.ResourceNotFoundException;
import com.hotel.reservas.repository.ReservationRepository;
import com.hotel.reservas.service.RedisCacheService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Servicio del Lado Query (CQRS - Consultas y Lectura Optimizada).
 * 
 * ¿CÓMO funciona?
 * Implementa el patrón Cache-Aside priorizando la lectura desde memoria en Redis (< 5ms).
 * Si ocurre un Cache Miss, consulta la base de datos relacional de forma estrictamente
 * de solo lectura (@Transactional(readOnly = true)), repoblando Redis de forma transparente.
 * 
 * ¿POR QUÉ este diseño?
 * Las consultas nunca deben adquirir cerrojos (Locks) en la base de datos ni interferir
 * con las transacciones de escritura, permitiendo atender miles de consultas por segundo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservationRepository;
    private final RedisCacheService redisCacheService;
    private final EntityManager entityManager;

    /**
     * Consulta una reserva por su identificador único.
     */
    public ReservationResponse getReservationById(Long id) {
        log.info("[CQRS Query] Fetching reservation ID: {}", id);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation with ID " + id + " was not found"));
        return mapToResponse(reservation);
    }

    /**
     * Consulta ultrarrápida de disponibilidad de una habitación.
     * 1. Consulta Redis en memoria (< 5ms).
     * 2. Si hay Cache Miss, consulta PostgreSQL sin bloqueos y repuebla la caché.
     */
    public boolean checkAvailability(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info("[CQRS Query] Checking availability for roomId: {} between {} and {}", roomId, checkInDate, checkOutDate);

        // 1. Lectura en memoria desde Redis (< 5ms)
        Boolean cachedAvailability = redisCacheService.checkAvailabilityInCache(roomId, checkInDate, checkOutDate);
        if (cachedAvailability != null) {
            log.info("[CQRS Query] Cache Hit in Redis: roomId {} available={}", roomId, cachedAvailability);
            return cachedAvailability;
        }

        // 2. Cache Miss: Fallback de solo lectura en PostgreSQL (SIN bloqueos)
        log.info("[CQRS Query] Cache Miss. Fallback query to PostgreSQL for roomId: {}", roomId);
        Long overlappingCount = entityManager.createQuery("""
            SELECT COUNT(r) FROM Reservation r
            WHERE r.roomId = :roomId
              AND r.status != :cancelledStatus
              AND r.checkInDate < :checkOutDate
              AND r.checkOutDate > :checkInDate
        """, Long.class)
                .setParameter("roomId", roomId)
                .setParameter("cancelledStatus", ReservationStatus.CANCELLED)
                .setParameter("checkInDate", checkInDate)
                .setParameter("checkOutDate", checkOutDate)
                .getSingleResult();

        boolean isAvailable = (overlappingCount == 0);

        // 3. Repoblación de Redis (Patrón Cache-Aside)
        redisCacheService.setRoomStock(roomId, checkInDate, checkOutDate, isAvailable ? 1 : 0);
        log.info("[CQRS Query] Populated Redis cache: roomId {} available={}", roomId, isAvailable);

        return isAvailable;
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

