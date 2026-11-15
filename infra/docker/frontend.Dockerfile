# ==============================================================================
# Multi-Stage Dockerfile para Frontend SPA (React + Vite + TailwindCSS)
# Contexto de construcción: Raíz del monorepositorio
# ==============================================================================

# ------------------------------------------------------------------------------
# ETAPA 1: Construcción de Assets Estáticos (Build Stage)
# ¿POR QUÉ esta etapa?
# Se compila el código TypeScript y se procesa TailwindCSS para generar
# archivos estáticos minificados (HTML, CSS y JS). El runtime de Node.js
# y sus herramientas de desarrollo se descartan por completo en producción.
# ------------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# 1. Copiar manifiesto de dependencias e instalar mediante npm ci (reproducibilidad exacta)
COPY frontend/package*.json ./
RUN npm ci

# 2. Copiar código fuente del frontend y compilar bundles de producción en /app/dist
COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------------------------
# ETAPA 2: Servidor Web Estático Ligero (Runtime Stage)
# ¿POR QUÉ Nginx Alpine?
# 1. Consume menos de 15MB de RAM en runtime y tiene un tamaño de imagen <25MB.
# 2. Resuelve el enrutamiento SPA mediante try_files y actúa como reverse proxy
#    hacia la API eliminando por completo problemas de CORS en producción.
# ------------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runner

# Reemplazar la configuración por defecto de Nginx con nuestra configuración personalizada
COPY infra/docker/nginx.conf /etc/nginx/conf.d/default.conf

# Copiar los assets compilados desde la etapa de construcción
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

# Chequeo de salud nativo de Nginx
HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/health || exit 1

CMD ["nginx", "-g", "daemon off;"]

