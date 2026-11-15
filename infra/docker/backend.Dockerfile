# ==============================================================================
# Multi-Stage Dockerfile para ms-reservas (Spring Boot 3.3 / Java 21)
# Contexto de construcción: Raíz del monorepositorio
# ==============================================================================

# ------------------------------------------------------------------------------
# ETAPA 1: Construcción y Empaquetado (Build Stage)
# ¿POR QUÉ esta etapa?
# Se utiliza una imagen completa con Maven y JDK para compilar el código y generar
# el JAR ejecutable. Toda la caché y herramientas de compilación se descartan
# al finalizar esta etapa, evitando inflar la imagen de producción.
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Copiar descriptor de dependencias y descargar dependencias en capa aislada
# ¿POR QUÉ? Optimización de Docker Cache: si el pom.xml no cambia, no se re-descargan dependencias.
COPY backend/ms-reservas/pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copiar código fuente y compilar artefacto omitiendo tests (validados en CI)
COPY backend/ms-reservas/src ./src
RUN mvn clean package -DskipTests -B

# ------------------------------------------------------------------------------
# ETAPA 2: Runtime Ligero de Producción
# ¿POR QUÉ Alpine JRE y Non-Root User?
# 1. Utiliza exclusivamente el JRE de Temurin en Alpine (~180MB vs >600MB de JDK completo).
# 2. Crea un usuario del sistema sin privilegios ('appuser') para cumplir con
#    el estándar de seguridad de mínimo privilegio exigido en entornos cloud.
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runner

# Crear grupo y usuario no-root por seguridad
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copiar exclusivamente el JAR generado desde la etapa de compilación
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Cambiar al usuario no privilegiado
USER appuser

# Parámetros JVM y puerto por defecto (12-Factor App)
# -XX:+UseG1GC: Garbage collector optimizado para latencia y concurrencia.
# -XX:MaxRAMPercentage=75.0: Limita el heap al 75% de la memoria asignada al contenedor.
# -XX:+ExitOnOutOfMemoryError: Garantiza que el pod/contenedor muera y sea reiniciado si ocurre OOM.
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    PORT=8080

EXPOSE 8080

# Chequeo de salud nativo utilizando las sondas de Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

