# Roadmap de Desarrollo: Sistema de Reservas Hoteleras Cloud-Native

Este documento define la planificación técnica organizada en **microtareas incrementales del ~15% de avance**. Cada microtarea requiere verificación y aprobación explícita antes de avanzar al siguiente bloque.

**Regla Operativa:** Máximo 2 a 3 archivos creados o modificados por prompt.

---

## 📊 Estado General de Progreso
- **Progreso Actual:** `60% - Migraciones Determinísticas Flyway DB (Completado) ➔ 75% En Preparación`
- **Siguiente Microtarea:** `75% - Pruebas Automatizadas de Concurrencia y Contratos RFC 7807`

---

## 🎯 Desglose de Microtareas (Iteraciones del ~15%) con Alineación CQRS

### [15%] Microtarea 1: Core Domain, Persistencia y Concurrencia Pesimista (PostgreSQL)
> **Impacto Arquitectónico:** `Lado Command` (Escritura y Control de Concurrencia).
- [x] Análisis de coherencia entre código generado y reglas de negocio.
- [x] Implementación en `ReservationRepository` con `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`).
- [x] Retorno síncrono inmediato `202 Accepted` con estado `PENDING`.
- [x] *Criterio de Aceptación:* Transacciones concurrentes serializadas a nivel de fila; 0% Overbooking.

---

### [30%] Microtarea 2: Capa de Caché y Consulta Rápida con Redis
> **Impacto Arquitectónico:** `Lado Query` (Lectura < 5ms) y `Lado Command` (Fast-Reject & Atomic DECR).
- [x] Configurar cliente y plantilla de Redis (`StringRedisTemplate`).
- [x] Implementar `RedisCacheService` con operaciones atómicas (`DECR` al reservar, `INCR` al liberar).
- [x] Implementar Fast-Reject (< 5ms) antes de bloquear en PostgreSQL.
- [x] *Criterio de Aceptación:* Consultas resueltas en < 5ms en memoria con Graceful Fallback a base de datos.

---

### [45%] Microtarea 3: Desacoplamiento Asíncrono y Worker de Pagos (Amazon SQS)
> **Impacto Arquitectónico:** `Lado Sync / Sincronización Event-Driven`.
- [x] Diseñar DTO de evento inmutable `ReservationPaymentEvent`.
- [x] Implementar `PaymentSqsWorker` con `@SqsListener` para procesar cobros en background.
- [x] Control de Idempotencia verificando estado estricto `PENDING`.
- [x] Transición de estados y Compensación (Saga): Si el pago es exitoso pasa a `CONFIRMED`; si falla pasa a `CANCELLED` y ejecuta `INCR` en Redis para restaurar el modelo de lectura.
- [x] *Criterio de Aceptación:* Respuesta HTTP < 500ms al cliente y sincronización asíncrona del modelo de lectura.

---

### [60%] Microtarea 4: Migraciones Determinísticas de Base de Datos (Flyway DB)
> **Impacto Arquitectónico:** `Lado Command` (Persistencia Relacional) y `Lado Query` (Optimización de Índices de Lectura).
- [x] **Lado Command:** Definir esquema estricto en PostgreSQL (`V1__init_schema.sql`) con restricciones `CHECK (check_out_date > check_in_date)` y `CHECK (total_price > 0)` para asegurar integridad en mutaciones.
- [x] **Lado Query:** Crear índices compuestos optimizados sobre `(room_id, status, check_in_date, check_out_date)` para el bloqueo pesimista y sobre `(status)` y `(guest_email)` para acelerar el fallback del Query Model.
- [x] **Gobierno de Esquema:** Configurar `ddl-auto: validate` en `application.yml` garantizando que Flyway sea la única autoridad sobre el esquema relacional en PostgreSQL / Amazon RDS.
- [x] *Criterio de Aceptación:* Base de datos levantada con `ddl-auto: validate` pasando validaciones de Flyway.

---

### [75%] Microtarea 5: Pruebas Automatizadas de Concurrencia y Contratos RFC 7807
> **Impacto Arquitectónico:** Validación cruzada `Command` (Zero Overbooking) y `Query` (Consistencia eventual).
- [x] **Lado Command:** Prueba de estrés multihilo (`ExecutorService`) lanzando 10 hilos simultáneos sobre la misma habitación. Exactamente 1 pasa a `PENDING` y 9 son rechazadas con HTTP 409 Conflict.
- [x] **Lado Query:** Verificar que la consulta de disponibilidad en caché refleje stock en cero inmediatamente tras la reserva.
- [x] **Lado Sync:** Simular procesamiento del worker en test validando la transición a `CONFIRMED` o `CANCELLED` con rollback de stock en Redis.
- [x] *Criterio de Aceptación:* Suite de pruebas automatizadas verdes al 100%.

---

### [90%] Microtarea 6: Frontend React - Flujo Asíncrono de Reserva y CQRS
> **Impacto Arquitectónico:** Consumo desacoplado de `Command Endpoint` (POST /reservations) y `Query Endpoints` (GET /reservations/{id}).
- [x] **Lado Command:** Envío de mutación y recepción de HTTP `202 Accepted` mostrando estado transitorio ("Procesando pago...").
- [x] **Lado Query:** Polling reactivo o consulta rápida de estado hacia `ReservationQueryService`.
- [x] **Manejo de Errores:** Renderizado amigable de excepciones RFC 7807 (409 Overbooking).
- [x] *Criterio de Aceptación:* Flujo E2E desde cliente web con retroalimentación en tiempo real.

---

### [100%] Microtarea 7: Orquestación E2E y Verificación de Entorno
> **Impacto Arquitectónico:** Infraestructura Multi-AZ que hospeda el Command Store (Postgres RDS), el Query Store (Redis ElastiCache) y el Sync Bus (Amazon SQS).
- [ ] Validación de extremo a extremo con `docker compose` (Postgres + Redis + LocalStack SQS + ms-reservas + Frontend).
- [ ] Actualización final de `README.md` con arquitectura CQRS documentada y manual operativo.
