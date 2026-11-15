package com.hotel.reservas.exception;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global centralizado de excepciones bajo el estándar RFC 7807 (Problem Details).
 * 
 * ¿CÓMO funciona?
 * Intercepta cualquier excepción surgida durante el ciclo de vida de la petición HTTP
 * y la transforma en un payload estructurado ProblemDetail sin filtrar stacktraces ni datos sensibles.
 * 
 * ¿POR QUÉ es indispensable en arquitecturas cloud-native?
 * Brinda un contrato de error estándar y predecible que cualquier cliente (frontend, apps móviles,
 * pasarelas de pago o microservicios terceros) puede parsear de forma consistente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.hotel.com/errors/not-found"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler({OverbookingConflictException.class, OptimisticLockingFailureException.class})
    public ProblemDetail handleOverbookingConflict(Exception ex) {
        String detailMessage = ex instanceof OptimisticLockingFailureException
                ? "The room was reserved simultaneously by another transaction. Please retry."
                : ex.getMessage();

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detailMessage);
        problemDetail.setTitle("Booking Overlap or Concurrency Conflict");
        problemDetail.setType(URI.create("https://api.hotel.com/errors/overbooking-conflict"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problemDetail.setTitle("Bad Request");
        problemDetail.setType(URI.create("https://api.hotel.com/errors/validation-failed"));

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        problemDetail.setProperty("invalidParams", errors);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }


    @ExceptionHandler({CannotAcquireLockException.class, ConcurrencyFailureException.class})
    public ProblemDetail handleLockExceptions(Exception ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Concurrency Conflict");
        pd.setType(URI.create("https://api.hotel.com/errors/concurrency-conflict"));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
    
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal error occurred. Please contact system support."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.hotel.com/errors/internal-error"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}

