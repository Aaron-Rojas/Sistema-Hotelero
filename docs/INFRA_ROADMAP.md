# Roadmap de Infraestructura y Operaciones Cloud (Rama: `infra`)

Este documento define la planificación técnica de arquitectura cloud, contenedores, observabilidad y orquestación en Kubernetes para el **Sistema de Reservas Hoteleras Cloud-Native**.

**Rol Operativo:** Lead Cloud & DevOps Engineer  
**Estado General de la Rama:** `Hito 1 [25%] ✅ ➔ Hito 2 [50%] ✅ ➔ Hito 3 [75%] ✅ ➔ Hito 4 [100%] (Siguiente)`  
**Regla Operativa:** Máximo 2 a 3 archivos creados o modificados por prompt.

---

## 📌 Hito 1 [25%]: Empaquetado Multi-Stage y Stack de Observabilidad Local
> **Objetivo:** Contenerizar el microservicio y el frontend bajo estándares ligeros y orquestar el stack completo con métricas de Prometheus y dashboards de Grafana.

- [x] **1.1 Dockerfile Multi-Stage para `ms-reservas` (`infra/docker/backend.Dockerfile`)**
  - [x] Stage 1: Compilación con Maven 3.9.9 y JDK 21 con caché de dependencias (`go-offline`).
  - [x] Stage 2: Runtime ligero sobre `eclipse-temurin:21-jre-alpine` con usuario no-root (`appuser`), heap al 75% (`MaxRAMPercentage`) y G1GC.
- [x] **1.2 Dockerfile Multi-Stage para `frontend` (`infra/docker/frontend.Dockerfile`)**
  - [x] Stage 1: Build con Node 20 / Vite y dependencias determinísticas (`npm ci`).
  - [x] Stage 2: Servidor estático sobre `nginx:1.27-alpine` con enrutamiento SPA y proxy reverso hacia la API (`/api/` -> `http://ms-reservas-api:8080/api/`).
- [x] **1.3 Integración de Observabilidad en `docker-compose.yml`**
  - [x] Configurar servicio de **Prometheus** (`infra/monitoring/prometheus.yml`) raspando métricas de Actuator en `/actuator/prometheus` cada 5s.
  - [x] Configurar servicio de **Grafana** (`infra/monitoring/grafana-datasources.yml`) con datasource Prometheus autoaprovisionado (puerto 3001).
  - [x] Separación de workloads en Compose: `ms-reservas-api` (perfil `api`) y `ms-pagos-worker` (perfil `worker`, sin puertos expuestos).
- [x] *Criterio de Aceptación:* `docker compose up -d` levanta Postgres, Redis, LocalStack SQS, ms-reservas-api, ms-pagos-worker, Frontend, Prometheus y Grafana.

---

## 📌 Hito 2 [50%]: Despliegue y Automatización en Máquina Virtual Ubuntu / AWS EC2
> **Objetivo:** Preparar la infraestructura de cómputo en VM Linux para simular o ejecutar un ambiente productivo desacoplado.

- [x] **2.1 Scripting de Aprovisionamiento (`infra/scripts/setup-host.sh`)**
  - [x] Instalación desatendida de Docker Engine, Docker Compose v2 y utilitarios de red.
  - [x] Hardening básico de firewall (UFW) y configuración de límites del kernel (`sysctl` para Redis: `vm.overcommit_memory=1`, `somaxconn=65535`).
- [x] **2.2 Configuración de Entorno de Producción (`.env.production.example`)**
  - [x] Externalización completa de credenciales y endpoints hacia servicios gestionados (RDS / ElastiCache).
- [x] **2.3 Validación Operativa de Métricas en Producción (`infra/scripts/smoke-test.sh`)**
  - [x] Verificación de telemetría activa en Grafana y prueba de carga con script de validación/smoke-test E2E.
- [x] *Criterio de Aceptación:* La VM levanta los contenedores en background y Grafana refleja consumo de CPU/memoria y tasa de peticiones.

---

## 📌 Hito 3 [75%]: Orquestación en Kubernetes y Autoescalado Híbrido (HPA + KEDA)
> **Objetivo:** Desplegar en Kubernetes separando las cargas de API y Workers mediante perfiles, autoescalado elástico y optimización de costos On-Demand vs. Spot.

- [x] **3.1 Manifiestos Base (`infra/k8s/`)**
  - [x] `ConfigMap` y `Secret` con variables desacopladas (`infra/k8s/config.yaml`).
  - [x] `Deployment` API (`SPRING_PROFILES_ACTIVE=api`) con `LivenessProbe` (`/actuator/health/liveness`) y `ReadinessProbe` (`/actuator/health/readiness`) (`infra/k8s/api-deployment.yaml`).
  - [x] `Deployment` Worker (`SPRING_PROFILES_ACTIVE=worker`) para consumo exclusivo de Amazon SQS (`infra/k8s/worker-deployment.yaml`).
  - [x] `Service` ClusterIP para la API (`infra/k8s/api-deployment.yaml`).
  - [x] `Deployment` y `Service` LoadBalancer para Frontend Nginx (`infra/k8s/frontend-deployment.yaml`).
- [x] **3.2 Separación de Nodos (On-Demand vs. Spot)**
  - [x] `nodeSelector` / `tolerations`: Cargas de API en instancias **On-Demand** (alta disponibilidad sin interrupciones).
  - [x] `nodeSelector` / `tolerations`: Cargas de Worker en instancias **Spot** (tolerantes a fallos y con reducción de hasta 80% en costos de cómputo).
- [x] **3.3 Autoescalado Dual (`infra/k8s/autoscaling.yaml`)**
  - [x] **HPA v2 (Horizontal Pod Autoscaler):** Escala pods de la API por CPU > 70%. `minReplicas: 2`, `maxReplicas: 10`. Scale-up: +3 pods/60s. Scale-down: -1 pod/5min (ventana anti-flapping).
  - [x] **KEDA `ScaledObject`:** Escala pods del Worker desde `minReplicaCount: 1` hasta `maxReplicaCount: 10` según `ApproximateNumberOfMessagesVisible`. Umbral: `queueLength: 5`. `pollingInterval: 15s`. `cooldownPeriod: 60s`. URL de cola via `queueURLFromEnv: "AWS_SQS_QUEUE_URL"` referenciando `config.yaml`.
- [x] *Criterio de Aceptación:* 5 manifiestos K8s (`config.yaml`, `api-deployment.yaml`, `worker-deployment.yaml`, `frontend-deployment.yaml`, `autoscaling.yaml`) validados y listos para Amazon EKS o clúster local (k3d/Minikube).

---

## 📌 Hito 4 [100%]: Integración Final E2E tras Merge de `main`
> **Objetivo:** Reconciliar el frontend final validado desde la rama principal y certificar la resiliencia integral.

- [ ] Reconciliación y sincronización final de artefactos post-merge de `main`.
- [ ] Prueba de humo End-to-End validando el flujo completo: Frontend -> API Stateless -> RDS -> SQS -> Worker -> Prometheus/Grafana.
- [ ] Documentación final en `README.md` del manual de operaciones de infraestructura y topología de despliegue.

