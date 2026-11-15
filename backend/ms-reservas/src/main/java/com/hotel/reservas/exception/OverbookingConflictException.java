package com.hotel.reservas.exception;

/**
 * Excepción lanzada cuando se detecta un intento de sobreventa (conflicto de fechas o versión).
 */
public class OverbookingConflictException extends RuntimeException {
    public OverbookingConflictException(String message) {
        super(message);
    }
}

