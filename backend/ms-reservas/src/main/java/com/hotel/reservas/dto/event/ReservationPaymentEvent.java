package com.hotel.reservas.dto.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Evento de dominio para la orden de cobro y confirmación asíncrona de reserva.
 * 
 * ¿POR QUÉ este DTO?
 * Desacopla la API del consumidor SQS, transportando exclusivamente los datos
 * requeridos para procesar el pago sin exponer la entidad relacional JPA ni
 * proxies perezosos (Lazy Loading) de Hibernate.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReservationPaymentEvent {

    private Long reservationId;
    private Long roomId;
    private BigDecimal amount;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private String paymentToken;
    private Instant timestamp;
}
