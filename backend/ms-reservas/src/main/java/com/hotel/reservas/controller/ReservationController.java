package com.hotel.reservas.controller;

import com.hotel.reservas.dto.request.CreateReservationRequest;
import com.hotel.reservas.dto.response.ReservationResponse;
import com.hotel.reservas.service.command.ReservationCommandService;
import com.hotel.reservas.service.query.ReservationQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controlador RESTful Stateless con arquitectura CQRS desacoplada.
 * 
 * ¿CÓMO aplica CQRS?
 * - Las mutaciones (POST) se delegan exclusivamente a ReservationCommandService.
 * - Las consultas (GET) se delegan exclusivamente a ReservationQueryService.
 * 
 * ¿POR QUÉ este diseño?
 * Aísla los requerimientos de alta disponibilidad y baja latencia de lectura (< 5ms)
 * de las reglas de concurrencia y transacciones ACID pesadas requeridas en escritura.
 */
@RestController
@RequestMapping("/api/v1/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Reservations", description = "Endpoints CQRS para la gestión de reservas hoteleras")
public class ReservationController {

    private final ReservationCommandService reservationCommandService;
    private final ReservationQueryService reservationQueryService;

    /**
     * CQRS Command: Creación de reserva con control de concurrencia pesimista en PostgreSQL.
     */
    @Operation(summary = "Crear una nueva reserva y encolar orden de cobro en SQS")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Reserva encolada exitosamente y en proceso de pago (PENDING)"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Conflicto por sobreventa de fechas (Overbooking)")
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationCommandService.createReservation(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * CQRS Query: Consulta de estado de reserva por ID.
     */
    @Operation(summary = "Consultar estado de una reserva por su identificador único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
        @ApiResponse(responseCode = "404", description = "Reserva no encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable Long id) {
        return ResponseEntity.ok(reservationQueryService.getReservationById(id));
    }

    /**
     * CQRS Query: Consulta ultrarrápida de disponibilidad con patrón Cache-Aside.
     */
    @Operation(summary = "Consultar disponibilidad de una habitación (Cache-Aside en Redis con fallback a DB)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consulta de disponibilidad ejecutada con éxito")
    })
    @GetMapping("/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @RequestParam Long roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        return ResponseEntity.ok(reservationQueryService.checkAvailability(roomId, checkInDate, checkOutDate));
    }
}

