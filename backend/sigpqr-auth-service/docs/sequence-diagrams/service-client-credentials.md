# Service Client Flow (Client Credentials)

Used by backend microservices for machine-to-machine communication (no user involved).

## Token Acquisition & API Call

```
+----------------+         +----------------+        +----------------+
| Microservice   |         |  Auth Server   |        | Target Service |
| (e.g. PQR)     |         |  :9000         |        | (e.g. User)    |
+-------+--------+         +-------+--------+        +-------+--------+
        |                          |                         |
        |  1. POST /oauth2/token   |                         |
        |  Authorization: Basic    |                         |
        |    base64(client_id:secret)                        |
        |  grant_type=client_credentials                     |
        |  scope=user:manage profile:read                    |
        |------------------------->|                         |
        |                          |                         |
        |                          |  2. Validate client     |
        |                          |  credentials (BCrypt)   |
        |                          |                         |
        |  3. Token response       |                         |
        |<-------------------------|                         |
        |  {                       |                         |
        |   access_token: "eyJ.."    (15 min)                |
        |   token_type: "Bearer"   |                         |
        |   expires_in: 900        |                         |
        |  }                       |                         |
        |  (no refresh token!)     |                         |
        |                          |                         |
        |  4. GET /api/users/123   |                         |
        |  Authorization: Bearer eyJ..                       |
        |----------------------------------------------->    |
        |                          |                         |
        |                          |  5. Validate JWT        |
        |                          |  (verify via jwks_uri)  |
        |                          |                         |
        |  6. Response             |                         |
        |<-----------------------------------------------|   |
        |                          |                         |
        |  When token expires:     |                         |
        |  just request a new one  |                         |
        |  (repeat step 1)         |                         |
```

## Notes

- **Client secret required**: Service clients authenticate via HTTP Basic (`client_secret_basic`) with a BCrypt-hashed secret.
- **No refresh token**: The `client_credentials` grant does not issue refresh tokens. When the access token expires (15 min), the service simply requests a new one.
- **No user context**: The token only contains client identity and scopes, no `sub` (user ID) claim.
- **Token lifetime**: 15 minutes (configured in `RegisteredClientInitializer.java`).

## Registered Service Clients

| Client ID | Secret | Scopes |
|---|---|---|
| `sigpqr-user-service` | `user-service-secret` | `user:manage`, `profile:read` |
| `sigpqr-pqr-service` | `pqr-service-secret` | `pqr:read`, `pqr:write`, `profile:read` |
| `sigpqr-academic-service` | `academic-service-secret` | `profile:read` |
| `sigpqr-notification-service` | `notification-service-secret` | `profile:read` |
| `sigpqr-file-service` | `file-service-secret` | `pqr:read` |
| `sigpqr-api-gateway` | `gateway-secret` | `profile:read` |

## Comparison with SPA Flow

| Aspect | SPA (Authorization Code + PKCE) | Service (Client Credentials) |
|---|---|---|
| **Who authenticates** | End user (via login form) | The service itself |
| **Client secret** | None (public client) | Yes (HTTP Basic) |
| **PKCE** | Required (S256) | N/A |
| **Access token TTL** | 30 min | 15 min |
| **Refresh token** | Yes (8h, rotated) | No |
| **User context** | Yes (`sub` = user ID) | No (only client + scopes) |
