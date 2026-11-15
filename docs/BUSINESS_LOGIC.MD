# Lógica de Negocio y Reglas de Dominio - Sistema de Reservas Hotelero

## 1. Reglas Core de Negocio
- **Cero Sobreventa (0% Overbooking):** Dos clientes no pueden reservar la misma habitación en el mismo rango de fechas bajo ninguna circunstancia (IDEMPOTENCIA, usar patrones de diseño)
- **Consulta Ultrarrápida (Latencia < 5ms):** La disponibilidad de habitaciones se lee primero desde Amazon ElastiCache (Redis). Si no existe la llave en caché, se consulta a PostgreSQL y se repuebla Redis.
- **Respuesta Síncrona Inmediata (< 500ms):** El usuario no espera a que el banco valide su tarjeta. La API bloquea la habitación, emite una orden a Amazon SQS y responde `202 Accepted` ("Procesando pago").

## 2. Ciclo de Vida de una Reserva (Transición de Estados)
Las reservas avanzan bajo los siguientes estados strictly controlados:

1. `PENDING`: La habitación ha sido validada y bloqueada en PostgreSQL mediante bloqueo pesimista (`SELECT FOR UPDATE`). El evento de cobro ha sido publicado en Amazon SQS.
2. `CONFIRMED`: El *Worker* consumió el mensaje de SQS, la pasarela bancaria aprobó el pago y se confirmó la transacción.
3. `CANCELLED`: Ocurrió un error en la pasarela bancaria o un timeout. El *Worker* libera la habitación en PostgreSQL y ejecuta `INCR` en Redis para devolver el stock a la caché.

## 3. Matriz de Componentes y Lógica
| Componente | Rol en el Dominio | Estrategia de Persistencia / Concurrencia |
| :--- | :--- | :--- |
| `ReservationController` | Punto de entrada RESTful Stateless | Validación DTO (Jakarta Validation) |
| `ReservationService` | Coordinador transaccional | `@Transactional`, bloqueo de fechas y publicación en SQS |
| `ReservationRepository` | Consultas de concurrencia a DB | `@Lock(LockModeType.PESSIMISTIC_WRITE)` o query nativo |
| `RedisCacheService` | Espejo de stock en tiempo real | Operaciones atómicas (`DECR` al reservar, `INCR` al cancelar) |
| `PaymentSqsWorker` | Consumidor asíncrono en background | Lectura de SQS, simulación bancaria y cierre de transacción |

## 4. Manejo Estándar de Errores (RFC 7807)
- Si la habitación está ocupada: `OverbookingConflictException` -> HTTP 409 Conflict.
- Si la habitación no existe: `ResourceNotFoundException` -> HTTP 404 Not Found.
- Si el payload es inválido: `MethodArgumentNotValidException` -> HTTP 400 Bad Request.