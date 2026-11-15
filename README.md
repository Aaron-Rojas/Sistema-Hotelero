# Sistema de Reservas Hoteleras Cloud-Native

Plataforma moderna, escalable y resiliente para la gestión de reservas hoteleras en tiempo real, diseñada bajo principios cloud-native, arquitectura desacoplada y alta disponibilidad Multi-AZ en Amazon Web Services (AWS).

---

## 1. Problema de Negocio

La industria hotelera experimenta fluctuaciones agresivas en la demanda (temporadas altas, eventos masivos y promociones relámpago). Los sistemas monolíticos y tradicionales suelen presentar los siguientes puntos de dolor:

- **Condiciones de Carrera y Sobreventa (*Overbooking*):** Concurrencia simultánea en la reserva de las mismas habitaciones, generando inconsistencias transaccionales y pérdidas económicas/reputacionales.
- **Cuellos de Botella por Acoplamiento:** Los procesos lentos (verificación bancaria, pasarelas de pago, emisión de boletas y envío de correos electrónicos) bloquean los hilos de ejecución de la API HTTP principal.
- **Puntos Únicos de Falla (SPOF):** Caídas de infraestructura local o de una única zona de disponibilidad que interrumpen la operación continua 24/7.
- **Dificultad de Escalabilidad Horizontal:** Servidores con estado en memoria (*sticky sessions*) que impiden elasticidad ágil frente a incrementos repentinos de tráfico.

### Objetivo de la Solución
Construir una plataforma basada en **APIs Stateless** con balanceo de carga, persistencia relacional con garantías ACID para inventario crítico, colas de mensajería asíncronas para amortiguar picos de demanda y despliegue en múltiples Zonas de Disponibilidad (Multi-AZ).

---

## 2. Arquitectura Conceptual

```
                             [ Usuarios / Clientes Web ]
                                         │
                                         ▼
                             [ AWS CloudFront (CDN) ]
                                         │
                      ┌──────────────────┴──────────────────┐
                      ▼                                     ▼
        [ Amazon S3 (Frontend Estático) ]       [ Application Load Balancer (ALB) ]
                                                            │
                       ┌────────────────────────────────────┴────────────────────────────────────┐
                       │                   Amazon VPC (Multi-AZ Deployment)                     │
                       │                                                                         │
                       │   [ Availability Zone A ]               [ Availability Zone B ]         │
                       │   ┌─────────────────────────────┐       ┌─────────────────────────────┐ │
                       │   │ Private App Subnet A        │       │ Private App Subnet B        │ │
                       │   │ ┌─────────────────────────┐ │       │ ┌─────────────────────────┐ │ │
                       │   │ │ Stateless Backend API   │ │       │ │ Stateless Backend API   │ │ │
                       │   │ └───────────┬─────────────┘ │       │ └───────────┬─────────────┘ │ │
                       │   └─────────────┼───────────────┘       └─────────────┼───────────────┘ │
                       │                 │                                     │                 │
                       │                 └─────────────────┬───────────────────┘                 │
                       │                                   │                                     │
                       │                        ┌──────────┴──────────┐                          │
                       │                        ▼                     ▼                          │
                       │                [ Amazon SQS ]      [ Isolated DB Subnet ]               │
                       │                (Booking Queue)     ┌────────────────────────┐           │
                       │                        │           │ Amazon RDS Multi-AZ    │           │
                       │                        ▼           │ ┌────────────────────┐ │           │
                       │                [ Async Worker ]───►│ │ Primary DB Instance│ │           │
                       │                (Fulfillment/Email) │ └─────────┬──────────┘ │           │
                       │                                    │           │ (Sync Rep) │           │
                       │                                    │ ┌─────────▼──────────┐ │           │
                       │                                    │ │ Standby DB Instance│ │           │
                       │                                    │ └────────────────────┘ │           │
                       │                                    └────────────────────────┘           │
                       └─────────────────────────────────────────────────────────────────────────┘
```

### Componentes Clave:
1. **Frontend (SPA/SSR Híbrido):** Alojado en Amazon S3 y distribuido globalmente mediante Amazon CloudFront con compresión Brotli/Gzip y terminación SSL/TLS en el borde.
2. **Application Load Balancer (ALB):** Distribuye el tráfico entrante de manera uniforme entre las instancias del backend en múltiples zonas de disponibilidad, realizando health checks periódicos.
3. **Stateless Backend APIs:** Contenedores efímeros (AWS ECS Fargate o EKS) sin estado en disco ni en memoria, escalando horizontalmente mediante políticas de Auto Scaling basadas en métricas de CPU, memoria y tasa de solicitudes.
4. **Amazon SQS (Decoupling Buffer):** Amortiguador de eventos asíncronos para el procesamiento de pagos, emisión de comprobantes y notificaciones, protegiendo al sistema de picos de carga.
5. **Amazon RDS Multi-AZ (PostgreSQL / MySQL):** Motor de base de datos relacional para el control transaccional de habitaciones, clientes y reservas con replicación síncrona automática en una zona secundaria para conmutación por error (*failover*) sin pérdida de datos.

---

## 3. Estructura del Monorepositorio

```
Sistema-Hotelero/
├── .antigravity/                   # Normas operativas de ingeniería y reglas para el agente
│   └── rules.md
├── backend/
│   └── ms-reservas/                # Microservicio Spring Boot 3.3 + Java 21
│       ├── pom.xml                 # Descriptor de dependencias Maven (AWS SQS, JPA, Actuator, OpenAPI)
│       └── src/
│           ├── main/java/com/hotel/reservas/
│           │   ├── config/         # Configuración OpenAPI y Amazon SQS
│           │   ├── controller/     # Controladores RESTful Stateless
│           │   ├── dto/            # DTOs de Request y Response con validación Jakarta
│           │   ├── entity/         # Entidades JPA con @Version para Optimistic Locking
│           │   ├── exception/      # Manejador Global RFC 7807 (Problem Details)
│           │   ├── repository/     # Repositorios Spring Data JPA con consultas atómicas
│           │   ├── service/        # Lógica transaccional y publicación de eventos SQS
│           │   └── MsReservasApplication.java
│           └── main/resources/
│               └── application.yml # Parámetros HikariCP, PostgreSQL y LocalStack SQS
├── frontend/                       # Cliente Web React 18 + Vite + TypeScript + TailwindCSS
│   ├── package.json
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   ├── tsconfig.json
│   └── src/
│       ├── components/             # Componentes UI (Navbar, ReservationForm, ReservationList)
│       ├── services/               # Cliente HTTP tipado con mapeo de errores RFC 7807
│       ├── types/                  # Modelos de dominio tipados estrictamente
│       ├── App.tsx
│       └── main.tsx
├── docker/
│   └── localstack/
│       └── init-sqs.sh             # Script de inicialización automática de colas SQS
├── docs/                           # Documentación arquitectónica, contratos y roadmap
│   └── ROADMAP.md
├── docker-compose.yml              # Infraestructura local: PostgreSQL 15, Redis 7 y LocalStack
├── .gitignore                      # Exclusiones de Git optimizadas para Web, JVM y Cloud
└── README.md                       # Documentación principal del proyecto
```

---

## 4. Instalación y Entorno de Ejecución Local

### Requisitos Previos:
- **Java Development Kit (JDK):** Versión 21 LTS
- **Node.js:** Versión 20+ LTS y `npm`
- **Docker Desktop & Docker Compose v2+**

---

### Paso 1: Levantar la Infraestructura Local (Bases de Datos y Colas)
Ejecutar desde la raíz del proyecto para iniciar los contenedores en segundo plano:

```bash
docker compose up -d
```

Validar el estado saludable (*healthy*) de los servicios:

```bash
docker compose ps
```

- **PostgreSQL 15:** `localhost:5432` (Base de datos: `hotel_reservas_db`, Usuario: `postgres`, Contraseña: `postgres123`)
- **Redis 7:** `localhost:6379`
- **LocalStack (AWS SQS):** `localhost:4566` (Cola inicializada: `hotel-reservation-events-queue`)

---

### Paso 2: Ejecutar el Microservicio Backend (`ms-reservas`)
Abrir una terminal en el directorio del microservicio:

```bash
cd backend/ms-reservas
mvn spring-boot:run
```

- **API Base:** `http://localhost:8080/api/v1/reservations`
- **Swagger UI (OpenAPI 3.1):** `http://localhost:8080/swagger-ui.html`
- **Health Check Actuator:** `http://localhost:8080/actuator/health`

---

### Paso 3: Ejecutar el Frontend (`React + Vite`)
Abrir una segunda terminal en el directorio del frontend:

```bash
cd frontend
npm install
npm run dev
```

- **Aplicación Web:** `http://localhost:3000` (con proxy reverso automático hacia `http://localhost:8080/api`)

---

## 5. Buenas Prácticas y Estrategia de Escalabilidad Futura

1. **Control de Concurrencia Optimista (`@Version`):**
   - El microservicio implementa bloqueo optimista mediante Hibernate. Cuando dos transacciones simultáneas intentan reservar una misma habitación para fechas colisionantes, la base de datos aborta la segunda mediante un conflicto HTTP 409, garantizando **cero sobreventa** sin necesidad de bloqueos pesimistas bloqueantes a nivel de tabla.
2. **Buffer Asíncrono con Amazon SQS:**
   - La API responde inmediatamente al usuario con la reserva en estado `PENDING` y despacha el evento a la cola SQS. Workers en segundo plano procesan el pago y la emisión de comprobantes, desacoplando el tiempo de respuesta del backend de la latencia de terceros.
3. **Caché Distribuida con Amazon ElastiCache (Redis):**
   - Implementar el patrón *Cache-Aside* con Redis 7 para lecturas frecuentes de catálogo de hoteles y disponibilidad, reduciendo la carga de lectura en la instancia primaria de PostgreSQL.
4. **Despliegue Multi-AZ en Producción:**
   - En AWS, la aplicación se empaqueta en imágenes de contenedor distroless ejecutándose sobre Amazon ECS Fargate con autoescalado basado en consumo de CPU y longitud de colas SQS (Target Tracking Scaling Policy).
   - Administrar la VPC, subredes, grupos de seguridad, colas SQS y clústeres mediante Terraform o AWS Cloud Development Kit (CDK), garantizando entornos reproducibles e inmutables.
5. **Observabilidad Distribuida:**
   - Integrar AWS CloudWatch Container Insights y OpenTelemetry / AWS X-Ray para correlacionar trazas distribuidas entre el frontend, las APIs stateless y los consumidores de SQS.

