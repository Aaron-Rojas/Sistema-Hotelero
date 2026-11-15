package com.hotel.reservas.repository;

import com.hotel.reservas.entity.Reservation;
import com.hotel.reservas.entity.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad Reservation.
 * Gestiona consultas relacionales con control de concurrencia pesimista sobre PostgreSQL.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * ANÁLISIS CRÍTICO DEL FALLO ANTERIOR:
     * La consulta previa usaba un simple 'SELECT COUNT(r) > 0'. En motores MVCC como PostgreSQL,
     * un COUNT no adquiere bloqueos de fila exclusivos. Si dos transacciones leían a la vez
     * cuando no existían reservas, ambas obtenían 0 y procedían a insertar, produciendo sobreventa.
     * 
     * CÓMO Y POR QUÉ:
     * Se aplica @Lock(LockModeType.PESSIMISTIC_WRITE), que genera 'SELECT ... FOR UPDATE' en SQL.
     * Esto serializa las transacciones concurrentes a nivel de fila en la base de datos,
     * garantizando la regla de '0% Overbooking'.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.roomId = :roomId
          AND r.status != :cancelledStatus
          AND r.checkInDate < :checkOutDate
          AND r.checkOutDate > :checkInDate
    """)
    List<Reservation> findOverlappingReservationsWithLock(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("cancelledStatus") ReservationStatus cancelledStatus
    );
}

