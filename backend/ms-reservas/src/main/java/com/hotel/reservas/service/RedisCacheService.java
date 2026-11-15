package com.hotel.reservas.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Servicio de caché distribuida en memoria (Redis / Amazon ElastiCache).
 * 
 * ¿CÓMO funciona?
 * Implementa el patrón Cache-Aside con operaciones atómicas (DECR / INCR)
 * para mantener un espejo del stock de habitaciones y responder consultas
 * de disponibilidad en latencias inferiores a 5 milisegundos.
 * 
 * ¿POR QUÉ tolerancia a fallos?
 * Si el servidor Redis no está disponible, el servicio captura la excepción
 * y permite la degradación elegante (Fallback hacia PostgreSQL) sin abortar
 * el flujo del usuario.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "hotel:room:stock:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    /**
     * Genera la llave canónica de Redis para el inventario de una habitación por rango de fechas.
     */
    public String buildStockKey(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        return KEY_PREFIX + roomId + ":" + checkIn + ":" + checkOut;
    }

    /**
     * Consulta ultrarrápida (< 5ms) del estado de disponibilidad en caché.
     * Retorna:
     * - Boolean.TRUE: Habitación disponible en caché (stock > 0).
     * - Boolean.FALSE: Habitación ocupada en caché (stock <= 0).
     * - null: Cache Miss (la llave no existe; se debe consultar PostgreSQL).
     */
    public Boolean checkAvailabilityInCache(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        String key = buildStockKey(roomId, checkIn, checkOut);
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                log.debug("Cache miss for key: {}", key);
                return null;
            }
            long stock = Long.parseLong(value);
            return stock > 0;
        } catch (Exception ex) {
            log.warn("Redis unavailable during availability check. Falling back to DB. Error: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Repuebla la llave en Redis con el stock actual y un tiempo de expiración (TTL).
     */
    public void setRoomStock(Long roomId, LocalDate checkIn, LocalDate checkOut, long stock) {
        String key = buildStockKey(roomId, checkIn, checkOut);
        try {
            redisTemplate.opsForValue().set(key, String.valueOf(stock), DEFAULT_TTL);
            log.debug("Cache populated for key: {} with stock: {}", key, stock);
        } catch (Exception ex) {
            log.warn("Failed to set stock in Redis for key {}. Error: {}", key, ex.getMessage());
        }
    }

    /**
     * Operación atómica DECR al registrar una reserva en estado PENDING.
     * Decrementa el stock en memoria de forma inmediata.
     */
    public Long decrementStock(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        String key = buildStockKey(roomId, checkIn, checkOut);
        try {
            Long newStock = redisTemplate.opsForValue().decrement(key);
            log.info("Atomic DECR executed for key: {}. Remaining stock: {}", key, newStock);
            return newStock;
        } catch (Exception ex) {
            log.warn("Failed to execute atomic DECR in Redis for key {}. Error: {}", key, ex.getMessage());
            return null;
        }
    }

    /**
     * Operación atómica INCR al cancelar o expirar una reserva.
     * Devuelve el stock a la caché para que esté disponible instantáneamente para otros clientes.
     */
    public Long incrementStock(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        String key = buildStockKey(roomId, checkIn, checkOut);
        try {
            Long newStock = redisTemplate.opsForValue().increment(key);
            log.info("Atomic INCR executed for key: {}. Restored stock: {}", key, newStock);
            return newStock;
        } catch (Exception ex) {
            log.warn("Failed to execute atomic INCR in Redis for key {}. Error: {}", key, ex.getMessage());
            return null;
        }
    }

    /**
     * Invalida explícitamente la llave de caché de una habitación.
     */
    public void evictRoomStock(Long roomId, LocalDate checkIn, LocalDate checkOut) {
        String key = buildStockKey(roomId, checkIn, checkOut);
        try {
            redisTemplate.delete(key);
            log.debug("Cache evicted for key: {}", key);
        } catch (Exception ex) {
            log.warn("Failed to evict Redis key {}. Error: {}", key, ex.getMessage());
        }
    }
}

