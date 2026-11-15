package com.hotel.reservas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad principal que representa una Reserva Hotelera en la base de datos relacional (Amazon RDS).
 * 
 * ¿CÓMO y POR QUÉ el campo '@Version'?
 * Implementa el patrón Optimistic Locking (Bloqueo Optimista). Cuando dos transacciones
 * intentan reservar o modificar concurrentemente la misma habitación para el mismo rango,
 * Hibernate detecta el conflicto de versión e interrumpe la segunda transacción mediante
 * una excepción de concurrencia optimista, evitando condiciones de carrera y sobreventa (overbooking).
 */
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_room_dates", columnList = "roomId, checkInDate, checkOutDate"),
    @Index(name = "idx_reservation_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long roomId;

    @Column(nullable = false, length = 120)
    private String guestName;

    @Column(nullable = false, length = 120)
    private String guestEmail;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    /**
     * Campo de control de concurrencia optimista.
     * Hibernate incrementa automáticamente este número en cada actualización.
     */
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ReservationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

