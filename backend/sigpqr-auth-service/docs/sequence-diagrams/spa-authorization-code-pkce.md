# SPA Client Flow (Authorization Code + PKCE)

Used by `sigpqr-web-student`, `sigpqr-web-admin`, `sigpqr-web-coordinator`.

## Login & Token Acquisition

```
+-----------+          +-----------+         +----------------+        +----------------+
|  Browser  |          |  SPA App  |         |  Auth Server   |        | Resource API   |
|  (User)   |          |  (React)  |         |  :9000         |        | (Gateway)      |
+-----+-----+          +-----+-----+         +-------+--------+        +-------+--------+
      |  1. Click "Login"    |                        |                         |
      |--------------------->|                        |                         |
      |                      |                        |                         |
      |  2. Generate PKCE pair                        |                         |
      |  +-------------------+                        |                         |
      |  | code_verifier     |                        |                         |
      |  | = random string   |                        |                         |
      |  | code_challenge    |                        |                         |
      |  | = SHA256(verifier)|                        |                         |
      |  +-------------------+                        |                         |
      |                      |                        |                         |
      |  3. Redirect to /oauth2/authorize             |                         |
      |<---------------------|                        |                         |
      |  Location:           |                        |                         |
      |   /oauth2/authorize  |                        |                         |
      |   ?response_type=code                         |                         |
      |   &client_id=sigpqr-web-student               |                         |
      |   &redirect_uri=http://localhost:3000/callback |                        |
      |   &scope=openid profile:read pqr:read pqr:write                        |
      |   &code_challenge=<hash>                      |                         |
      |   &code_challenge_method=S256                 |                         |
      |                      |                        |                         |
      |  4. GET /oauth2/authorize ------------------->|                         |
      |                      |                        |                         |
      |  5. Show login form  |                        |                         |
      |<----------------------------------------------|                         |
      |                      |                        |                         |
      |  6. Submit username + password -------------->|                         |
      |                      |                        |  7. Validate            |
      |                      |                        |  credentials            |
      |                      |                        |  (via user-service)     |
      |                      |                        |                         |
      |  8. Redirect back with authorization code     |                         |
      |<----------------------------------------------|                         |
      |  Location:           |                        |                         |
      |   http://localhost:3000/callback               |                        |
      |   ?code=abc123       |                        |                         |
      |                      |                        |                         |
      |  9. Follow redirect  |                        |                         |
      |--------------------->|                        |                         |
      |                      |                        |                         |
      |                      |  10. POST /oauth2/token                         |
      |                      |  grant_type=authorization_code                  |
      |                      |  code=abc123           |                         |
      |                      |  code_verifier=<raw>   |                         |
      |                      |  client_id=sigpqr-web-student                   |
      |                      |  redirect_uri=http://localhost:3000/callback     |
      |                      |----------------------->|                         |
      |                      |                        |                         |
      |                      |                        |  11. Verify:            |
      |                      |                        |  SHA256(code_verifier)  |
      |                      |                        |  == code_challenge?     |
      |                      |                        |                         |
      |                      |  12. Token response    |                         |
      |                      |<-----------------------|                         |
      |                      |  {                     |                         |
      |                      |   access_token: "eyJ.."  (30 min)               |
      |                      |   refresh_token: "def.." (8 hours)              |
      |                      |   id_token: "eyJ.."      (user info)            |
      |                      |   token_type: "Bearer"                          |
      |                      |   expires_in: 1800     |                         |
      |                      |  }                     |                         |
      |                      |                        |                         |
      |                      |  13. Store tokens in memory                     |
      |                      |                        |                         |
      |                      |  14. GET /api/pqr      |                         |
      |                      |  Authorization: Bearer eyJ..                    |
      |                      |------------------------------------------------>|
      |                      |                        |                         |
      |                      |                        |  15. Validate JWT       |
      |                      |                        |  (verify via jwks_uri)  |
      |                      |                        |                         |
      |                      |  16. Response data     |                         |
      |                      |<------------------------------------------------|
      |  17. Render UI       |                        |                         |
      |<---------------------|                        |                         |
```

## Token Refresh (when access token expires)

```
+-----------+         +----------------+
|  SPA App  |         |  Auth Server   |
+-----+-----+         +-------+--------+
      |                        |
      |  API call returns 401  |
      |  (token expired)       |
      |                        |
      |  POST /oauth2/token    |
      |  grant_type=refresh_token
      |  refresh_token=def..   |
      |  client_id=sigpqr-web-student
      |----------------------->|
      |                        |
      |                        |  Validate refresh token
      |                        |  Rotate: old token invalidated
      |                        |
      |  New token pair        |
      |<-----------------------|
      |  {                     |
      |   access_token: "new.."     (30 min)
      |   refresh_token: "rotated.." (8 hours)
      |  }                     |
      |                        |
      |  Retry original API call with new access_token
```

## Notes

- **No client secret**: SPA clients are public (`ClientAuthenticationMethod.NONE`), secrets can't be safely stored in browser code.
- **PKCE required**: `code_challenge_method=S256` prevents authorization code interception attacks.
- **Refresh token rotation**: `reuseRefreshTokens(false)` means each refresh token is single-use. After refreshing, the old token is invalidated and a new one is issued. This limits damage if a refresh token is stolen.
- **Token lifetimes**: Access token = 30 min, Refresh token = 8 hours (configured in `RegisteredClientInitializer.java`).

## Registered SPA Clients

| Client ID | Name | Redirect URI | Custom Scopes |
|---|---|---|---|
| `sigpqr-web-student` | Student Web App | `http://localhost:3000/callback` | `pqr:read`, `pqr:write`, `profile:read`, `profile:write` |
| `sigpqr-web-admin` | Admin Web App | `http://localhost:3001/callback` | `pqr:read`, `pqr:write`, `pqr:respond`, `profile:read`, `profile:write`, `admin:read`, `admin:write`, `user:manage` |
| `sigpqr-web-coordinator` | Coordinator Web App | `http://localhost:3002/callback` | `pqr:read`, `pqr:write`, `pqr:respond`, `profile:read`, `profile:write`, `admin:read` |
