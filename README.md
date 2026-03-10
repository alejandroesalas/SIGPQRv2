# SIGPQRv2 — Sistema Integral de Gestión de Peticiones, Quejas y Reclamos

A microservice-based platform for managing petitions, complaints, and claims (PQR) in an academic environment. Students submit requests to their program's coordinator, who reviews and responds. Admins manage the institutional structure (faculties, programs, users, coordinators).

Rebuilt from a legacy Laravel 5.8 + Angular 8 monolith into a modern, cloud-ready architecture.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 (Virtual Threads enabled) |
| Framework | Spring Boot 4 / Spring Framework 7 |
| Build | Maven (multi-module monorepo) |
| Server | Tomcat (embedded, virtual thread executor) |
| Auth | OAuth2 + Spring Authorization Server + JWT |
| Database | PostgreSQL (database per service) |
| Messaging | RabbitMQ |
| Frontend | Angular 19 (3 independent apps) |
| Resilience | Resilience4j (circuit breaker, retry, bulkhead) |
| Tracing | Micrometer + OTLP (OpenTelemetry Protocol) |
| Logging | Logback + Logstash encoder (structured JSON) + Correlation ID |
| Health | Spring Actuator (readiness/liveness probes) |
| API Docs | SpringDoc OpenAPI (Swagger) |
| Testing | JUnit 5, Testcontainers, Cucumber (integration/BDD) |
| Containers | Docker + Docker Compose (dev) / Kubernetes (prod) |
| CI/CD | GitHub Actions |

---

## Virtual Threads

All services run on **Java 21 Virtual Threads** (Project Loom). This eliminates the traditional thread-per-request bottleneck — each request gets a lightweight virtual thread instead of a costly platform thread.

### Configuration

```yaml
# application.yml (shared via config server)
spring:
  threads:
    virtual:
      enabled: true   # Tomcat uses virtual threads for request handling
```

That single property switches Tomcat's executor to virtual threads. No code changes needed.

### What runs on virtual threads

| Component | Virtual Threads? | How |
|-----------|-----------------|-----|
| Tomcat request handling | Yes | `spring.threads.virtual.enabled=true` |
| `@Async` methods | Yes | Auto-configured with virtual thread executor |
| `@Scheduled` tasks | Yes | Auto-configured with virtual thread executor |
| RabbitMQ listeners (`@RabbitListener`) | Yes | Configure `SimpleRabbitListenerContainerFactory` with virtual thread executor |
| Feign client calls | Yes | Blocking I/O on virtual threads = no wasted platform threads |
| JDBC / JPA calls | Yes | Blocking DB calls on virtual threads = non-blocking behavior |

### Why virtual threads matter for this project

```
Traditional threads (without virtual threads):
  200 platform threads → 200 concurrent requests max
  Request #201 waits in queue

Virtual threads:
  200 platform threads → thousands of concurrent requests
  Each blocking call (DB, Feign, RabbitMQ) yields the platform thread
  Platform thread picks up another virtual thread immediately
```

For a microservice architecture with heavy inter-service communication (Feign calls, DB queries, RabbitMQ), virtual threads eliminate the need for reactive/WebFlux complexity while achieving similar throughput.

### Impact on Resilience4j

With virtual threads, **Bulkhead changes from ThreadPool to Semaphore**:

| Pattern | Without Virtual Threads | With Virtual Threads |
|---------|------------------------|---------------------|
| Bulkhead | `ThreadPoolBulkhead` (dedicated thread pool) | `SemaphoreBulkhead` (concurrency limiter) |
| Reason | Thread pools managed manually | Virtual threads are cheap — just limit concurrency |

```yaml
# Resilience4j config
resilience4j:
  bulkhead:
    instances:
      user-service:
        maxConcurrentCalls: 50    # Semaphore-based, not thread pool
        maxWaitDuration: 500ms
```

### RabbitMQ Listener Configuration

```java
@Configuration
public class RabbitMQConfig {

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setTaskExecutor(new VirtualThreadTaskExecutor("rabbit-"));
        return factory;
    }
}
```

---

## Project Structure

```
SIGPQRv2/
├── backend/
│   ├── pom.xml                              # Parent POM (dependency management)
│   ├── sigpqr-common/                       # Shared library
│   ├── sigpqr-discovery-server/             # Eureka service registry
│   ├── sigpqr-config-server/               # Spring Cloud Config
│   ├── sigpqr-api-gateway/                  # Spring Cloud Gateway
│   ├── sigpqr-auth-service/                 # OAuth2 Authorization Server
│   ├── sigpqr-user-service/                 # User CRUD & role management
│   ├── sigpqr-academic-service/             # Faculties & Programs
│   ├── sigpqr-pqr-service/                  # Core PQR (Requests, Responses)
│   ├── sigpqr-notification-service/         # Email notifications (async)
│   └── sigpqr-file-service/                 # File upload/download/storage
├── frontend/
│   ├── sigpqr-student-app/                  # Angular 19 — Student portal
│   ├── sigpqr-coordinator-app/              # Angular 19 — Coordinator portal
│   └── sigpqr-admin-app/                    # Angular 19 — Admin portal
├── docker-compose.yml                       # Local development environment
└── README.md
```

---

## Microservices

### Infrastructure Services

| # | Service | Port | Description |
|---|---------|------|-------------|
| 1 | **sigpqr-discovery-server** | 8761 | Eureka service registry. All services register here for discovery. In production with Kubernetes, service discovery can be handled by K8s DNS. |
| 2 | **sigpqr-config-server** | 8888 | Centralized configuration backed by a Git repository. Services pull their config on startup. Secrets managed via cloud provider secret manager in production. |
| 3 | **sigpqr-api-gateway** | 8080 | Single entry point for all clients. Handles routing, CORS, rate limiting, JWT validation via Token Relay, and Correlation ID generation. No business logic. |

### Business Services

| # | Service | Port | Database | Description |
|---|---------|------|----------|-------------|
| 4 | **sigpqr-auth-service** | 9000 | `sigpqr_auth` | OAuth2 Authorization Server. Handles login, JWT issuing/refresh/revocation, password reset tokens, and email verification tokens. Calls user-service via Feign (client credentials) to validate credentials. |
| 5 | **sigpqr-user-service** | 8081 | `sigpqr_users` | User lifecycle management. CRUD for students, teachers, coordinators. Profile management. Role promotion (teacher→coordinator) and demotion. |
| 6 | **sigpqr-academic-service** | 8082 | `sigpqr_academic` | Academic structure management. Faculty and Program CRUD. Program-coordinator assignment. Program-student relationships. |
| 7 | **sigpqr-pqr-service** | 8083 | `sigpqr_pqr` | Core domain. Request (PQR) CRUD, Response CRUD, RequestType management. Request lifecycle: `abierta → en proceso → cerrada`. Attachment metadata tracking. |

### Supporting Services

| # | Service | Port | Database | Description |
|---|---------|------|----------|-------------|
| 8 | **sigpqr-notification-service** | 8084 | `sigpqr_notification` | Asynchronous email delivery. Consumes events from RabbitMQ (registration verification, password reset, PQR status changes). Logs all notifications sent. |
| 9 | **sigpqr-file-service** | 8085 | `sigpqr_files` | File upload, download, and deletion. Abstracts storage backend (local filesystem in dev, S3/Azure Blob/GCS in production). Tracks file metadata. |

### Shared Library

| Module | Description |
|--------|-------------|
| **sigpqr-common** | Shared across all services. Contains: API response wrapper, global exception handler, base DTOs, enums (Profile, RequestStatus, RequestType), Correlation ID interceptor + Feign propagator, pagination utilities, constants. |

---

## Frontend Applications

| App | URL (dev) | Users | Scope |
|-----|-----------|-------|-------|
| **sigpqr-student-app** | `http://localhost:4200` | Students | Registration, login, create PQR, view request timeline, upload attachments, profile management |
| **sigpqr-coordinator-app** | `http://localhost:4300` | Coordinators | Login, view PQRs by type for their program, respond to PQRs, profile management |
| **sigpqr-admin-app** | `http://localhost:4400` | Admins | Login, manage users/faculties/programs, promote/demote coordinators, restore soft-deleted items, dashboard statistics |

Each app is an independent Angular 19 project. They all communicate with the backend through the **API Gateway** only.

---

## Architecture

### High-Level Overview

```
┌──────────────┐  ┌──────────────────┐  ┌──────────────┐
│  Student App │  │ Coordinator App  │  │  Admin App   │
│  :4200       │  │ :4300            │  │  :4400       │
└──────┬───────┘  └────────┬─────────┘  └──────┬───────┘
       │                   │                    │
       │    authorization_code + PKCE           │
       ▼                   ▼                    ▼
┌─────────────────────────────────────────────────────┐
│                   API Gateway :8080                  │
│     (JWT validation + Token Relay + Correlation ID)  │
└──────────────────────┬──────────────────────────────┘
                       │
       ┌───────────────┼───────────────────┐
       ▼               ▼                   ▼
┌─────────────┐ ┌─────────────┐  ┌──────────────────┐
│ auth-service│ │ user-service│  │ academic-service │
│ :9000       │ │ :8081       │  │ :8082            │
│ OAuth2 AS   │ │             │  │                  │
└─────────────┘ └─────────────┘  └──────────────────┘
                       │
              ┌────────┼─────────┐
              ▼        ▼         ▼
       ┌───────────┐ ┌──────┐ ┌──────────────────┐
       │pqr-service│ │ file │ │ notification     │
       │ :8083     │ │:8085 │ │ :8084            │
       └───────────┘ └──────┘ └──────────────────┘

              ┌─────────────────────┐
              │     RabbitMQ        │
              │  (async events)     │
              └─────────────────────┘

       ┌──────────────────────────────┐
       │  Discovery :8761 | Config :8888  │
       └──────────────────────────────┘
```

### Authentication & Authorization (OAuth2)

#### Grant Types

| Client | Grant Type | Why |
|--------|-----------|-----|
| sigpqr-student-app | `authorization_code` + PKCE | Browser-based SPA, most secure for public clients |
| sigpqr-coordinator-app | `authorization_code` + PKCE | Same as above |
| sigpqr-admin-app | `authorization_code` + PKCE | Same as above |
| sigpqr-pqr-service | `client_credentials` | Service-to-service, no user context |
| sigpqr-auth-service | `client_credentials` | Calls user-service during login |
| sigpqr-academic-service | `client_credentials` | Internal service communication |
| sigpqr-file-service | `client_credentials` | Internal service communication |
| sigpqr-notification-service | `client_credentials` | Consumes async events, no user context |

#### Scopes

| Scope | Description | Granted To |
|-------|-------------|-----------|
| `pqr:read` | Read PQR requests | student, coordinator |
| `pqr:write` | Create/update PQR requests | student |
| `pqr:respond` | Respond to PQR requests | coordinator |
| `profile:read` | Read own profile | student, coordinator |
| `profile:write` | Update own profile | student, coordinator |
| `admin:read` | Read admin dashboard data | admin |
| `admin:write` | Manage entities (CRUD) | admin |
| `user:manage` | Promote/demote, enable/disable users | admin |

#### Token Flow

```
Token Relay (user-initiated requests):
  FE ──[user JWT]──► Gateway ──[same JWT]──► service ──[same JWT]──► downstream service

Client Credentials (service-to-service, no user):
  service ──[client_credentials]──► auth-service ──► [service JWT]
  service ──[service JWT]──► another service
```

### Inter-Service Communication

| From | To | Method | Pattern | Example |
|------|----|--------|---------|---------|
| Gateway | All services | HTTP | Token Relay | Forward user request + JWT |
| auth-service | user-service | Feign (HTTP) | Client Credentials | Fetch user credentials during login |
| pqr-service | user-service | Feign (HTTP) | Token Relay | Get student/coordinator details |
| pqr-service | academic-service | Feign (HTTP) | Token Relay | Get program info |
| pqr-service | notification-service | RabbitMQ | Async Event | PQR status changed → send email |
| pqr-service | file-service | RabbitMQ | Async Event | Attachment cleanup |
| user-service | notification-service | RabbitMQ | Async Event | Registration → send verification email |
| auth-service | notification-service | RabbitMQ | Async Event | Password reset → send reset email |

### Resilience Patterns

| Pattern | Tool | Applied Where |
|---------|------|---------------|
| Circuit Breaker | Resilience4j | All Feign clients (fallback on downstream failure) |
| Retry | Resilience4j | Transient failures on service-to-service calls |
| Bulkhead | Resilience4j | Semaphore-based concurrency limiter (virtual thread compatible) |
| Rate Limiting | Spring Cloud Gateway | API Gateway (per-client limits) |
| Timeout | Resilience4j + Feign | All HTTP calls (prevent hanging) |

---

## Observability

### Correlation ID

Every request gets an `X-Correlation-Id` header that travels through the entire service chain and is returned to the client in the response.

```
Client Request:
  POST /api/pqr/requests
  X-Correlation-Id: (optional — client can provide one)
         │
         ▼
  ┌─── Gateway ───┐
  │ No ID?        │──► Generate UUID: "corr-abc123"
  │ Has ID?       │──► Use client's value
  └───────┬───────┘
          │  X-Correlation-Id: corr-abc123  (added to request + response headers)
          ▼
  pqr-service ──► user-service ──► academic-service
  MDC: corr-abc123   MDC: corr-abc123   MDC: corr-abc123
  Logs with it       Logs with it       Logs with it
          │
          ▼
Client Response:
  HTTP 201 Created
  X-Correlation-Id: corr-abc123    ← Client stores this for error reporting
```

**Implementation lives in sigpqr-common** (shared by all services):

| Component | Purpose |
|-----------|---------|
| `CorrelationIdFilter` (Gateway) | Generates or accepts correlation ID, adds to request + response headers |
| `CorrelationIdInterceptor` (Services) | Reads header, puts in MDC, adds to response header |
| `CorrelationIdFeignInterceptor` (Services) | Propagates correlation ID on Feign calls |

**Frontend Angular interceptor** captures `X-Correlation-Id` from responses and displays it on error screens: *"Something went wrong. Reference: corr-abc123"*

### Distributed Tracing (OTLP)

Uses **Micrometer + OpenTelemetry Protocol (OTLP)** — cloud-agnostic, works with any tracing backend.

```yaml
# application.yml (shared via config server)
management:
  tracing:
    sampling:
      probability: 1.0              # 100% in dev, lower in prod
  otlp:
    tracing:
      endpoint: ${TRACING_ENDPOINT:http://localhost:4318/v1/traces}
```

Swap the backend by changing one environment variable:

| Environment | TRACING_ENDPOINT | Backend |
|-------------|-----------------|---------|
| Local | `http://localhost:4318/v1/traces` | Zipkin / Jaeger (Docker) |
| AWS | `http://otel-collector:4318/v1/traces` | AWS X-Ray |
| Azure | `http://otel-collector:4318/v1/traces` | Application Insights |
| GCP | `http://otel-collector:4318/v1/traces` | Cloud Trace |
| Self-managed | `http://otel-collector:4318/v1/traces` | Grafana Tempo / Datadog |

Zero code changes across environments.

### Centralized Logging

**Local:** Human-readable logs to console.
**Cloud:** Structured JSON logs via Logstash encoder.

Log format includes `correlationId`, `traceId`, and `spanId`:

```
# Local (plain text)
10:15:32 [corr-abc123,trace-abc123,span-001] INFO  c.s.pqr.RequestController - Creating PQR

# Cloud (structured JSON)
{
  "timestamp": "2026-03-10T10:15:32.123Z",
  "service": "sigpqr-pqr-service",
  "correlationId": "corr-abc123",
  "traceId": "abc123def456",
  "spanId": "001aaa",
  "level": "INFO",
  "logger": "c.s.pqr.RequestController",
  "message": "Creating PQR request"
}
```

Switched via Spring profile:

| Profile | Format | Destination |
|---------|--------|-------------|
| `local` | Plain text | Console |
| `cloud` | JSON (Logstash encoder) | stdout → collected by Fluent Bit / CloudWatch Agent / Cloud Logging |

**Log collector per cloud:**

| Cloud | Collector | Storage & Search |
|-------|-----------|-----------------|
| AWS | CloudWatch Agent / Fluent Bit | CloudWatch Logs Insights |
| Azure | Azure Monitor Agent | Log Analytics (KQL) |
| GCP | Built-in (stdout → Cloud Logging) | Cloud Logging |
| Self-managed | Fluent Bit / Filebeat | ELK (Elasticsearch + Kibana) |

### Health Checks

Every service exposes Spring Actuator endpoints:

```
GET /actuator/health           # Overall health
GET /actuator/health/readiness # Ready to accept traffic?
GET /actuator/health/liveness  # Process alive?
GET /actuator/info             # Service metadata
GET /actuator/metrics          # Micrometer metrics
```

Used by Kubernetes for pod lifecycle management and by the Gateway for service health awareness.

### Observability Summary

```
┌──────────────────────────────────────────────────────────────┐
│                       Your Services                           │
│                                                              │
│  Request arrives → CorrelationIdInterceptor adds to MDC     │
│  Micrometer auto-injects traceId + spanId into MDC          │
│                                                              │
│  Every log line contains: correlationId, traceId, spanId    │
│                                                              │
│  Output:                                                     │
│    ├── Logs    → Logback → stdout → log collector → search  │
│    ├── Traces  → OTLP exporter → tracing backend            │
│    └── Metrics → Micrometer → Prometheus / CloudWatch        │
└──────────────────────────────────────────────────────────────┘

Debugging flow:
  1. User reports error → provides X-Correlation-Id from response
  2. Search correlationId across all service logs
  3. See full request path + where it failed
  4. Click traceId → jump to distributed trace visualization
```

---

## Testing Strategy

### Stack

| Tool | Purpose |
|------|---------|
| **JUnit 5 (Jupiter)** | Unit and integration test runner |
| **Testcontainers** | Spin up real PostgreSQL and RabbitMQ containers for integration tests |
| **Cucumber** | BDD-style integration/acceptance tests with Gherkin feature files |
| **Spring Boot Test** | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`, test slices |
| **Mockito** | Mocking for unit tests |

### Test Types

| Type | Scope | Tools | Runs Against |
|------|-------|-------|-------------|
| **Unit** | Single class/method | JUnit 5 + Mockito | In-memory (no containers) |
| **Integration** | Service layer + DB | JUnit 5 + Testcontainers | Real PostgreSQL/RabbitMQ containers |
| **BDD / Acceptance** | End-to-end per service | Cucumber + Testcontainers | Real containers, Gherkin scenarios |

### Directory Structure (per service)

```
src/
├── main/java/...
└── test/
    ├── java/com/sigpqr/<service>/
    │   ├── unit/              # Unit tests (Mockito, no Spring context)
    │   ├── integration/       # Integration tests (@SpringBootTest + Testcontainers)
    │   └── bdd/               # Cucumber glue code (step definitions, hooks)
    │       └── steps/
    └── resources/
        └── features/          # Gherkin .feature files
```

### Testcontainers Usage

Each service that uses a database or message broker defines a shared container config:

```java
@Testcontainers
@SpringBootTest
abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

### Cucumber Integration

Feature files describe business scenarios in Gherkin:

```gherkin
# src/test/resources/features/create-pqr.feature
Feature: Create PQR Request

  Scenario: Student creates a petition
    Given a student with email "student@uniexample.edu"
    When the student creates a PQR of type "Petición"
    Then the PQR status should be "ABIERTA"
    And a notification event should be published
```

Cucumber runs on top of JUnit 5 via `@Suite` + `@SelectClasspathResource`:

```java
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.sigpqr.pqr.bdd.steps")
class CucumberRunnerTest {
}
```

### Running Tests

```bash
# All tests (unit + integration + BDD) — requires Docker running
cd backend
mvn clean verify

# Unit tests only (no Docker needed)
mvn test -Dgroups="unit"

# Integration tests only
mvn test -Dgroups="integration"
```

---

## Database Schema (per service)

### sigpqr_auth
```
password_reset_tokens (id, token, email, user_id, status, expires_at, timestamps)
email_verification_tokens (id, token, email, user_id, status, expires_at, timestamps)
oauth2_registered_client (Spring Authorization Server managed)
oauth2_authorization (Spring Authorization Server managed)
oauth2_authorization_consent (Spring Authorization Server managed)
```

### sigpqr_users
```
users (id, name, lastname, email, password_hash, id_type, id_num, verified, status, profile_id, program_id, timestamps, soft_delete)
profiles (id, name, description, timestamps, soft_delete)
```

### sigpqr_academic
```
faculties (id, name, timestamps, soft_delete)
programs (id, name, faculty_id, coordinator_id, timestamps, soft_delete)
```

### sigpqr_pqr
```
request_types (id, type, description, timestamps, soft_delete)
requests (id, title, description, status, request_type_id, program_id, student_id, timestamps, soft_delete)
responses (id, title, description, status_response, type, request_id, student_id, coordinator_id, timestamps, soft_delete)
attachment_requests (id, name, file_id, extension, request_id, timestamps, soft_delete)
attachment_responses (id, name, file_id, extension, response_id, timestamps, soft_delete)
```

### sigpqr_notification
```
notification_log (id, type, recipient_email, subject, status, error_message, timestamps)
```

### sigpqr_files
```
file_metadata (id, original_name, stored_name, extension, size, content_type, storage_path, uploaded_by, timestamps, soft_delete)
```

---

## Domain Model

### User Roles

| Role | Profile ID | Capabilities |
|------|-----------|-------------|
| Admin | 1 | Full CRUD on all entities, promote/demote users, restore deleted items |
| Coordinator | 2 | View requests for their program, respond to PQRs |
| Student | 3 | Create PQRs, view own requests and responses, upload files |
| Teacher | 4 | Base role, can be promoted to Coordinator by Admin |

### PQR Request Lifecycle

```
┌──────────┐     ┌─────────────┐     ┌──────────┐
│ ABIERTA  │────►│ EN PROCESO  │────►│ CERRADA  │
│ (open)   │     │(in progress)│     │ (closed) │
└──────────┘     └─────────────┘     └──────────┘
  Student          Coordinator         Coordinator
  creates          responds            sends final
  request                              response
```

### Request Types

| ID | Type | Description |
|----|------|-------------|
| 1 | Petición | Formal petition/request |
| 2 | Queja | Complaint about a service or process |
| 3 | Reclamo | Claim requiring resolution |

---

## Build Order

| Phase | # | Module | Depends On |
|-------|---|--------|-----------|
| **1 — Infrastructure** | 1 | sigpqr-common | — |
| | 2 | sigpqr-discovery-server | — |
| | 3 | sigpqr-config-server | discovery |
| **2 — Identity** | 4 | sigpqr-auth-service | common, discovery, config |
| | 5 | sigpqr-user-service | common, discovery, config, auth |
| **3 — Domain** | 6 | sigpqr-academic-service | common, discovery, config, auth |
| | 7 | sigpqr-pqr-service | common, discovery, config, auth, user, academic |
| **4 — Supporting** | 8 | sigpqr-file-service | common, discovery, config, auth |
| | 9 | sigpqr-notification-service | common, discovery, config |
| **5 — Entry Point** | 10 | sigpqr-api-gateway | discovery, config, auth |
| **6 — Frontend** | 11 | sigpqr-student-app | gateway |
| | 12 | sigpqr-coordinator-app | gateway |
| | 13 | sigpqr-admin-app | gateway |

---

## Development Setup

### Prerequisites

- Java 21
- Maven 3.9+
- Node.js 20+ / npm
- Angular CLI 19
- Docker + Docker Compose
- PostgreSQL 16+
- RabbitMQ 3.13+

### Local Development

```bash
# 1. Start infrastructure (PostgreSQL, RabbitMQ, tracing)
docker-compose up -d postgres rabbitmq otel-collector

# 2. Build common library
cd backend/sigpqr-common
mvn clean install

# 3. Start discovery server
cd ../sigpqr-discovery-server
mvn spring-boot:run

# 4. Start config server
cd ../sigpqr-config-server
mvn spring-boot:run

# 5. Start business services (in separate terminals)
cd ../sigpqr-auth-service && mvn spring-boot:run
cd ../sigpqr-user-service && mvn spring-boot:run
cd ../sigpqr-academic-service && mvn spring-boot:run
cd ../sigpqr-pqr-service && mvn spring-boot:run
cd ../sigpqr-file-service && mvn spring-boot:run
cd ../sigpqr-notification-service && mvn spring-boot:run

# 6. Start gateway
cd ../sigpqr-api-gateway
mvn spring-boot:run

# 7. Start frontend apps (in separate terminals)
cd frontend/sigpqr-student-app && ng serve --port 4200
cd frontend/sigpqr-coordinator-app && ng serve --port 4300
cd frontend/sigpqr-admin-app && ng serve --port 4400
```

### Full Stack via Docker Compose

```bash
docker-compose up --build
```

---

## Cloud Deployment

### Target Architecture

```
                         ┌─────────────────────────────────────────────┐
                         │            Kubernetes Cluster                │
                         │                                             │
   Internet ──► LB ──►  │  Ingress Controller                        │
                         │       │                                     │
                         │  ┌────▼─────┐                              │
                         │  │ Gateway  │                              │
                         │  └────┬─────┘                              │
                         │       │                                     │
                         │  ┌────┼──────────────┬──────────┐          │
                         │  ▼    ▼              ▼          ▼          │
                         │ auth  user    academic  pqr               │
                         │ x2    x2      x1        x3                │
                         │                                            │
                         │ file  notification                         │
                         │ x2    x1                                   │
                         │                                            │
                         │  ┌───────────┐  ┌────────────────────┐    │
                         │  │ RabbitMQ  │  │ PostgreSQL (managed)│    │
                         │  │ (managed) │  │                    │    │
                         │  └───────────┘  └────────────────────┘    │
                         └─────────────────────────────────────────────┘
```

### Environment Mapping

| Concern | Local (dev) | Cloud (prod) |
|---------|-------------|-------------|
| Service Discovery | Eureka | Kubernetes DNS |
| Configuration | Config Server (local git) | Config Server + Secret Manager |
| Gateway | Spring Cloud Gateway | Gateway + K8s Ingress |
| Databases | Local PostgreSQL | Managed (RDS / Cloud SQL / Azure DB) |
| Messaging | Local RabbitMQ | Managed (Amazon MQ / CloudAMQP) |
| File Storage | Local filesystem | S3 / Azure Blob / GCS |
| Container Registry | Local Docker | ECR / ACR / GCR |
| Secrets | .env / application-local.yml | AWS SSM / Azure Key Vault / GCP Secret Manager |
| Tracing | OTLP → Zipkin/Jaeger (local) | OTLP → X-Ray / App Insights / Cloud Trace |
| Logging | Plain text to console | JSON → Fluent Bit → CloudWatch / ELK / Cloud Logging |

---

## API Overview

All requests go through the API Gateway at `:8080`. Every response includes `X-Correlation-Id` header.

### Auth Service (`/api/auth/**`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/oauth2/authorize` | OAuth2 authorization (handled by Spring) |
| POST | `/oauth2/token` | Token issue/refresh (handled by Spring) |
| POST | `/oauth2/revoke` | Token revocation |
| POST | `/api/auth/password/reset-request` | Request password reset email |
| POST | `/api/auth/password/reset` | Reset password with token |

### User Service (`/api/users/**`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users` | List users (filterable by profile) |
| POST | `/api/users` | Create user |
| GET | `/api/users/{id}` | Get user details |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Soft delete user |
| PUT | `/api/users/{id}/promote` | Promote teacher → coordinator |
| PUT | `/api/users/{id}/demote` | Demote coordinator → teacher |
| POST | `/api/users/{id}/restore` | Restore soft-deleted user |
| GET | `/api/users/profiles` | List profiles |
| GET | `/api/users/count` | Count users by profile |

### Academic Service (`/api/academic/**`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/academic/faculties` | List faculties |
| POST | `/api/academic/faculties` | Create faculty |
| GET | `/api/academic/faculties/{id}` | Get faculty with programs |
| PUT | `/api/academic/faculties/{id}` | Update faculty |
| DELETE | `/api/academic/faculties/{id}` | Delete faculty |
| GET | `/api/academic/programs` | List programs |
| POST | `/api/academic/programs` | Create program |
| GET | `/api/academic/programs/{id}` | Get program details |
| PUT | `/api/academic/programs/{id}` | Update program |
| DELETE | `/api/academic/programs/{id}` | Delete program |
| POST | `/api/academic/programs/{id}/restore` | Restore program |
| GET | `/api/academic/programs/unassigned` | Programs without coordinator |

### PQR Service (`/api/pqr/**`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/pqr/requests` | List requests (scoped by role) |
| POST | `/api/pqr/requests` | Create PQR request |
| GET | `/api/pqr/requests/{id}` | Get request with responses |
| PUT | `/api/pqr/requests/{id}` | Update request |
| POST | `/api/pqr/responses` | Create response to request |
| PUT | `/api/pqr/responses/{id}` | Update response |
| GET | `/api/pqr/request-types` | List request types |
| GET | `/api/pqr/requests/by-type/{typeId}` | Requests filtered by type |
| GET | `/api/pqr/requests/by-program/{programId}` | Requests by program |

### File Service (`/api/files/**`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload` | Upload file(s) |
| GET | `/api/files/{id}` | Download file |
| DELETE | `/api/files/{id}` | Delete file |
| GET | `/api/files/metadata/{id}` | Get file metadata |

---

## RabbitMQ Events

| Exchange | Routing Key | Producer | Consumer | Payload |
|----------|------------|----------|----------|---------|
| `user.events` | `user.registered` | user-service | notification-service | `{userId, email, verificationToken}` |
| `auth.events` | `auth.password-reset` | auth-service | notification-service | `{email, resetToken, expiresAt}` |
| `pqr.events` | `pqr.status-changed` | pqr-service | notification-service | `{requestId, studentEmail, newStatus}` |
| `pqr.events` | `pqr.response-created` | pqr-service | notification-service | `{requestId, studentEmail, coordinatorName}` |
| `pqr.events` | `pqr.attachment-deleted` | pqr-service | file-service | `{fileId}` |
