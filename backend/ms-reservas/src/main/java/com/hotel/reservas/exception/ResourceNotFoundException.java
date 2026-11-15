package com.hotel.reservas.exception;

/**
 * Excepción lanzada cuando una entidad solicitada no existe.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

