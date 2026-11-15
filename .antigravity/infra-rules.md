# Infra & Cloud Operations Rules (Rama: `infra`)

Este documento establece el marco normativo y operativo inquebrantable para el agente y los ingenieros que operen en la rama **`infra`** del proyecto **Sistema de Reservas Hoteleras Cloud-Native**.

---

## 1. Prohibición Estricta de Modificar Lógica de Negocio
- **Aislamiento Total:** Queda estrictamente prohibido modificar clases Java de dominio (`com.hotel.reservas.entity.*`), repositorios, servicios CQRS (`service.command.*`, `service.query.*`), controladores REST o componentes de interfaz en React (`frontend/src/components/*`).
- **Alcance Permitido:** Solo se autorizan modificaciones en archivos de configuración de despliegue (`application.yml`, `nginx.conf`, descriptores de build o Dockerfiles) cuando los requerimientos de empaquetado, observabilidad o cloud lo exijan explícitamente.

---

## 2. Seguridad Cloud y Principios 12-Factor App
- **Cero Secretos Hardcodeados:** Prohibido escribir contraseñas, tokens, llaves de acceso, IPs estáticas o cadenas fijas de conexión (`localhost`) en el código o manifiestos versionados.
- **Configuración por Entorno:** Toda dependencia externa (RDS PostgreSQL, ElastiCache Redis, Amazon SQS, Prometheus) debe inyectarse a través de variables de entorno con sintaxis estándar de fallback:
  ```yaml
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/hotel_reservas_db}
  ```
- **Principio de Mínimo Privilegio:** Los contenedores en producción deben ejecutarse bajo usuarios sin privilegios (`non-root user`).

---

## 3. Optimización de Contenedores (Docker Multi-Stage Builds)
- **Multi-Stage Obligatorio:** Toda imagen de Docker debe separar la etapa de compilación (`build stage`) de la etapa de ejecución (`runtime stage`).
- **Footprint Mínimo:** Utilizar exclusivamente imágenes base ligeras (`alpine`, `eclipse-temurin:21-jre-alpine`, `nginx:alpine`), descartando SDKs, compiladores y herramientas innecesarias en el contenedor final para minimizar la superficie de vulnerabilidades (CVEs) y optimizar tiempos de despliegue/autoescalado.

---

## 4. Regla de Iteración Controlada y Microtareas
- **Límite Estricto:** Máximo 2 a 3 archivos creados o modificados por prompt.
- **Validación Continua:** No se avanza al siguiente hito sin antes presentar la verificación del estado actual y recibir la aprobación explícita del usuario.
- **Roadmap First:** Cualquier cambio de alcance o entrega debe reflejarse en `docs/INFRA_ROADMAP.md` antes de escribir manifiestos o scripts.

