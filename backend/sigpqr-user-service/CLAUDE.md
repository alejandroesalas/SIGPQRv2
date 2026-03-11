# sigpqr-user-service

## Overview

User lifecycle management microservice for SIGPQRv2. Handles CRUD operations for students, teachers, coordinators and admins. Provides profile management, role promotion/demotion, and internal endpoints consumed by auth-service for credential lookup, password updates, and email verification.

## Directory Structure

```
src/main/java/com/sigpqr/user/
├── UserServiceApplication.java          # Spring Boot entry point
├── config/
│   ├── SecurityConfig.java              # OAuth2 resource server + endpoint authorization
│   ├── DataSeeder.java                  # Seed profiles & users (local profile only)
│   ├── OpenApiConfig.java               # Swagger/OpenAPI configuration
│   └── RabbitMQConfig.java              # user.events exchange + JSON converter
├── controller/
│   ├── UserController.java              # Public API: /api/users/**
│   └── InternalUserController.java      # Service-to-service: /api/users/internal/**
├── converter/
│   └── ProfileConverter.java            # JPA AttributeConverter: Profile enum ↔ Integer (DB)
├── dto/
│   ├── RegisterStudentDto.java          # Public student registration input (validated, no profile field)
│   ├── RegisterTeacherDto.java          # Admin-only teacher creation input (validated, no profile field)
│   ├── UpdateUserDto.java               # Update input (validated)
│   ├── UserResponseDto.java             # API response (Profile enum, no passwordHash)
│   ├── UserCredentialsDto.java          # Internal: credentials for auth-service
│   ├── UserCountDto.java                # Count by profile response
│   └── ProfileResponseDto.java          # Profile list response
├── entity/
│   ├── UserEntity.java                  # users table — UUID PK, soft delete, Profile enum via converter
│   └── ProfileEntity.java              # profiles table — Long PK (maps to Profile enum)
├── enums/
│   ├── UserStatus.java                  # ACTIVE, INACTIVE
│   └── IdType.java                      # CC, TI, CE, PASSPORT
├── event/
│   └── UserRegisteredEvent.java         # Published to RabbitMQ on registration
├── repository/
│   ├── UserRepository.java              # Soft-delete aware queries (uses Profile enum)
│   └── ProfileRepository.java
└── service/
    ├── UserService.java                 # Core business logic + RabbitMQ publishing
    ├── ProfileService.java              # Profile listing
    └── InternalUserService.java         # Credential lookup, password/email update
```

## Tech Stack

- Java 17, Spring Boot 3.4.3, Spring Cloud 2024.0.1
- Spring Data JPA + PostgreSQL (`sigpqr_users` database)
- Spring Security OAuth2 Resource Server (JWT validation from auth-service :9000)
- Spring AMQP (RabbitMQ) for event publishing
- Eureka client for service discovery
- SpringDoc OpenAPI (Swagger UI)
- sigpqr-common (ApiResponse, PageResponse, exceptions, constants, Profile enum, CorrelationIdFilter)

## Key Design Decisions

### Profile Enum (not Long)
`UserEntity.profile` is a `com.sigpqr.common.enums.Profile` enum, stored as integer in DB via `ProfileConverter` (JPA `AttributeConverter`). The DB column stays `profile_id INTEGER` — the converter handles `Profile.STUDENT ↔ 3`. No casts anywhere in Java code.

### Split Registration Endpoints
- `POST /api/users` — public student self-registration (profile hardcoded to `STUDENT`)
- `POST /api/users/teachers` — admin-only teacher creation (requires `admin:write` scope)
- No generic "create any role" endpoint exists. This prevents unauthorized role escalation.

## API Endpoints

### Public (JWT-protected via gateway)

| Method | Endpoint | Scope | Description |
|--------|----------|-------|-------------|
| GET | `/api/users` | `admin:read` | List users (filterable by `?profile=STUDENT`) |
| POST | `/api/users` | public | Register student |
| POST | `/api/users/teachers` | `admin:write` | Create teacher (admin only) |
| GET | `/api/users/{id}` | authenticated | Get user details |
| PUT | `/api/users/{id}` | authenticated | Update user |
| DELETE | `/api/users/{id}` | `admin:write` | Soft delete |
| PUT | `/api/users/{id}/promote` | `user:manage` | Teacher → Coordinator |
| PUT | `/api/users/{id}/demote` | `user:manage` | Coordinator → Teacher |
| POST | `/api/users/{id}/restore` | `admin:write` | Restore deleted user |
| GET | `/api/users/profiles` | `admin:read` | List profiles |
| GET | `/api/users/count` | `admin:read` | Count users by profile |

### Internal (service-to-service via client_credentials)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/internal/by-email?email=` | Credential lookup |
| GET | `/api/users/internal/update-password?userId=&passwordHash=` | Update password hash |
| GET | `/api/users/internal/verify-email?userId=` | Mark email verified |

## RabbitMQ Events

- **Exchange:** `user.events` (TopicExchange)
- **Publishes:** `user.registered` → `{userId, email, verificationToken}` on user creation

## Database

- **DB name:** `sigpqr_users`
- **Tables:** `users`, `profiles`
- **Schema managed by:** Hibernate `ddl-auto: update`

## Configuration

- **Port:** 8081
- **JWT issuer:** http://localhost:9000 (auth-service)
- **Eureka:** http://localhost:8761/eureka/
- **RabbitMQ:** localhost:5672
- **PostgreSQL:** localhost:5432/sigpqr_users

## Build & Run

```bash
# Compile
cd backend && mvn clean compile -pl sigpqr-user-service -am

# Run (requires PostgreSQL + RabbitMQ + Eureka + auth-service)
mvn spring-boot:run -pl sigpqr-user-service

# Swagger UI
http://localhost:8081/swagger-ui.html
```
