package com.hotel.reservas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de solicitud para la creación de una nueva reserva.
 * 
 * ¿POR QUÉ validaciones declarativas?
 * Aseguran que las peticiones que no cumplan con el contrato sean rechazadas
 * inmediatamente en la capa de entrada (Controller) con HTTP 400 Bad Request,
 * antes de consumir recursos de base de datos o lógica de negocio.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    @NotNull(message = "roomId is required")
    @Positive(message = "roomId must be a positive integer")
    private Long roomId;

    @NotBlank(message = "guestName is required")
    @Size(min = 2, max = 120, message = "guestName must be between 2 and 120 characters")
    private String guestName;

    @NotBlank(message = "guestEmail is required")
    @Email(message = "guestEmail must be a valid email format")
    private String guestEmail;

    @NotNull(message = "checkInDate is required")
    @FutureOrPresent(message = "checkInDate cannot be in the past")
    private LocalDate checkInDate;

    @NotNull(message = "checkOutDate is required")
    @Future(message = "checkOutDate must be in the future")
    private LocalDate checkOutDate;

    @NotNull(message = "totalPrice is required")
    @DecimalMin(value = "0.01", message = "totalPrice must be greater than zero")
    private BigDecimal totalPrice;
}

