-- ============================================================================
-- V1: Esquema Relacional de Reservas con Control de Concurrencia e Integridad
-- Compatible con: PostgreSQL 15+ / Amazon RDS Multi-AZ (Flyway DB)
-- ============================================================================

CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL,
    guest_name VARCHAR(120) NOT NULL,
    guest_email VARCHAR(120) NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_reservation_dates CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_reservation_price CHECK (total_price > 0)
);

-- ============================================================================
-- Índices de Alto Rendimiento para Arquitectura CQRS
-- ============================================================================

-- 1. Lado Command: Optimización para bloqueo pesimista y detección atómica de solapamientos
-- Acelera: WHERE room_id = :id AND status != 'CANCELLED' AND check_in_date < :out AND check_out_date > :in
CREATE INDEX IF NOT EXISTS idx_reservations_command_overlap 
    ON reservations (room_id, status, check_in_date, check_out_date);

-- 2. Lado Query: Búsqueda rápida por estado y cliente (Fallback de solo lectura en Cache Miss)
CREATE INDEX IF NOT EXISTS idx_reservations_query_status 
    ON reservations (status);

CREATE INDEX IF NOT EXISTS idx_reservations_query_email 
    ON reservations (guest_email);

CREATE INDEX IF NOT EXISTS idx_reservations_query_created 
    ON reservations (created_at DESC);

