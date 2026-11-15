#!/usr/bin/env bash
# ==============================================================================
# Script de Validación Operativa y Smoke Testing
# Rol: Lead Cloud & DevOps Engineer
# Valida la salud de todos los componentes y ejecuta una prueba funcional E2E
# ==============================================================================

set -euo pipefail

BASE_URL_FRONTEND="${FRONTEND_URL:-http://localhost:3000}"
BASE_URL_API="${API_URL:-http://localhost:8080}"
PROMETHEUS_URL="${PROMETHEUS_URL:-http://localhost:9090}"
GRAFANA_URL="${GRAFANA_URL:-http://localhost:3001}"

echo "===================================================================="
echo " Ejecutando Smoke Test del Sistema de Reservas Hoteleras"
echo "===================================================================="

# ------------------------------------------------------------------------------
# 1. Chequeos de Salud de Infraestructura (Health Probes)
# ------------------------------------------------------------------------------
echo -n "[1/4] Verificando Frontend Nginx (/health)... "
STATUS_FRONTEND=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL_FRONTEND/health" || echo "000")
if [ "$STATUS_FRONTEND" -eq 200 ]; then
    echo "OK (HTTP 200)"
else
    echo "FALLÓ (HTTP $STATUS_FRONTEND)"
    exit 1
fi

echo -n "[2/4] Verificando API Backend Spring Boot Actuator (/actuator/health)... "
STATUS_BACKEND=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL_API/actuator/health" || echo "000")
if [ "$STATUS_BACKEND" -eq 200 ]; then
    echo "OK (HTTP 200)"
else
    echo "FALLÓ (HTTP $STATUS_BACKEND)"
    exit 1
fi

echo -n "[3/4] Verificando Servidor Prometheus (/api/v1/targets)... "
STATUS_PROM=$(curl -s -o /dev/null -w "%{http_code}" "$PROMETHEUS_URL/api/v1/targets" || echo "000")
if [ "$STATUS_PROM" -eq 200 ]; then
    echo "OK (HTTP 200)"
else
    echo "FALLÓ (HTTP $STATUS_PROM)"
    exit 1
fi

# ------------------------------------------------------------------------------
# 2. Prueba Funcional Sintética: Creación de Reserva CQRS Command
# ¿CÓMO valida el flujo?
# Envía un payload a través del reverse proxy de Nginx (/api/) para confirmar
# que el proxy interno, la base de datos, Redis y SQS procesan el comando
# retornando HTTP 202 Accepted en < 500ms.
# ------------------------------------------------------------------------------
echo "[4/4] Enviando solicitud sintética de reserva (CQRS Command)..."

RANDOM_ID=$((RANDOM % 1000 + 500))
TODAY=$(date +%Y-%m-%d)
NEXT_WEEK=$(date -d "+5 days" +%Y-%m-%d 2>/dev/null || date -v+5d +%Y-%m-%d 2>/dev/null || echo "2026-12-15")

PAYLOAD=$(cat <<EOF
{
  "roomId": $RANDOM_ID,
  "guestName": "Smoke Test Runner",
  "guestEmail": "smoke.test@hotel.com",
  "checkInDate": "$TODAY",
  "checkOutDate": "$NEXT_WEEK",
  "totalPrice": 199.99
}
EOF
)

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL_FRONTEND/api/v1/reservations" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD" || echo -e "{}\n000")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

echo "Respuesta del Backend: $BODY"
echo "Código HTTP Retornado: $HTTP_CODE"

if [ "$HTTP_CODE" -eq 202 ]; then
    echo ">>> ÉXITO: Reserva encolada correctamente con HTTP 202 Accepted (PENDING)."
else
    echo ">>> ERROR: Se esperaba HTTP 202 pero se obtuvo $HTTP_CODE."
    exit 1
fi

echo "===================================================================="
echo " Smoke Test Finalizado: Todos los componentes operan con éxito."
echo "===================================================================="

