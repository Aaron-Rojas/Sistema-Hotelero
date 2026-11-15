# Operational Agent Rules & Architecture Principles

Este documento establece el marco normativo y operativo de ingeniería para el agente y los desarrolladores que colaboren en el proyecto del **Sistema de Reservas Hoteleras Cloud-Native**.

---

## 1. Flujo de Trabajo Iterativo (Roadmap First)
- **Actualización Continua:** Antes de implementar cualquier cambio estructural, componente o módulo de código, se debe consultar y actualizar el archivo `docs/ROADMAP.md`.
- **Visibilidad del Progreso:** Cada hito o tarea completada debe reflejarse inmediatamente con su respectivo estado (`[x]` Completado, `[-]` En Progreso, `[ ]` Pendiente).
- **Desarrollo Incremental:** Los cambios deben implementarse en incrementos verificables, garantizando la estabilidad de la rama principal.

---

## 2. Principios de Arquitectura Cloud-Native y APIs Stateless
- **Stateless Backend:** Las APIs no deben almacenar estado de sesión ni contexto de usuario en memoria local o sistema de archivos del servidor. Todo estado transitorio se delega a tokens criptográficos (JWT) o cachés distribuidas (Redis/ElastiCache), permitiendo escalabilidad horizontal elástica tras un Application Load Balancer (ALB).
- **12-Factor App:** Respetar los principios de configuración mediante variables de entorno, procesos sin estado, aislamiento de dependencias y logs canalizados hacia flujos estándar (stdout/stderr).
- **Desacoplamiento Asíncrono:** Toda operación intensiva o susceptible a cuellos de botella (confirmación de pagos, notificaciones por correo/SMS, reconciliación de reservas) debe desacoplarse mediante colas de mensajes (Amazon SQS) y workers dedicados.
- **Transaccionalidad ACID y Concurrencia:** La gestión del inventario hotelero (bloqueo y confirmación de habitaciones) debe prevenir sobreventa (*overbooking*) utilizando transacciones con nivel de aislamiento adecuado o bloqueos optimistas/pesimistas en el motor de base de datos relacional (Amazon RDS).

---

## 3. Estándar de Control de Versiones (Conventional Commits)
- Todos los commits deben redactarse estrictamente en **inglés** siguiendo la convención de [Conventional Commits v1.0.0](https://www.conventionalcommits.org/):
  - `feat: <description>` para nuevas funcionalidades.
  - `fix: <description>` para corrección de bugs.
  - `docs: <description>` para adición o actualización de documentación.
  - `refactor: <description>` para refactorización sin alteración del comportamiento externo.
  - `test: <description>` para pruebas unitarias, de integración o de carga.
  - `chore: <description>` para tareas de configuración de dependencias, builds o linters.
  - `ci: <description>` para canalizaciones de CI/CD.

---

## 4. Calidad de Código, Tipado Estricto y Manejo de Errores
- **Tipado Estricto:** Prohibido el uso de tipos ambiguos (e.g., `any` en TypeScript, `interface{}` sin validación en Go, variables sin anotación en Python). Se deben definir DTOs, interfaces y Value Objects para todas las entradas y salidas.
- **Manejo Global de Excepciones:** La API debe interceptar todas las excepciones no controladas mediante un middleware o filtro centralizado, retornando respuestas estructuradas bajo el estándar **RFC 7807 (Problem Details for HTTP APIs)**, garantizando que jamás se filtren stack traces o datos sensibles al cliente.
- **Validación de Entradas:** Validación declarativa estricta en la capa de entrada (Payloads, Query Params, Headers) antes de alcanzar la capa de dominio o servicios.

---

## 5. Estándar Lingüístico Híbrido
- **Artefactos de Código (Inglés):** Variables, funciones, clases, interfaces, endpoints, tablas de base de datos, nombres de archivos y mensajes de Git deben estar 100% en inglés.
- **Comunicación y Documentación (Español):** Todas las explicaciones, análisis de arquitectura, respuestas en chat, documentación (`README.md`, `ROADMAP.md`) y comentarios explicativos dentro del código fuente deben redactarse en español.

---

## 6. Enfoque Pedagógico y Análisis Crítico
- **Comentarios Explicativos:** Al implementar lógica no trivial o patrones de diseño (e.g., Circuit Breaker, Outbox Pattern, Optimistic Locking), incluir comentarios concisos explicando el **QUÉ**, el **CÓMO** y el **POR QUÉ**.
- **Refactorización Transparente:** Antes de corregir un antipatrón, vulnerabilidad o fallo de concurrencia en código preexistente, documentar analíticamente el problema identificado y la justificación de la solución propuesta.

---

## 7. Regla Obligatoria: Patrón CQRS (Command Query Responsibility Segregation)
- **Prohibición Inquebrantable:** Queda estrictamente prohibido mezclar operaciones de escritura (Commands) y lectura (Queries) en un mismo Service o Controller.
- **Lado Command (Escritura):** Toda mutación de estado (`POST`, `PUT`, `DELETE`, `PATCH`) debe residir exclusivamente en paquetes `*.command` (e.g., `service.command`). Ataca exclusivamente a PostgreSQL (transacciones ACID con bloqueo pesimista `@Lock(LockModeType.PESSIMISTIC_WRITE)` para asegurar 0% Overbooking), ejecuta operaciones atómicas previas en caché y emite eventos de dominio hacia Amazon SQS.
- **Lado Query (Lectura):** Toda consulta (`GET`) debe residir exclusivamente en paquetes `*.query` (e.g., `service.query`). Prioriza la lectura ultrarrápida (< 5ms) desde Redis (patrón Cache-Aside con fallback a PostgreSQL) y tiene estrictamente prohibido adquirir bloqueos en base de datos (`@Transactional(readOnly = true)`).
- **Sincronización Asíncrona (Sync):** Los Workers en segundo plano (`worker.*`) son los responsables de mantener sincronizado el modelo de lectura (Redis) tras confirmar o cancelar transacciones en el modelo de escritura (PostgreSQL).


