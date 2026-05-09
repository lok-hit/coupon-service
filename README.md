# Coupon Service

## Overview

REST service for managing discount coupons. Designed with production-grade quality in mind — scalability, concurrency safety, and configurability across environments.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 3 |
| Database | PostgreSQL |
| Cache | Redis |
| Migrations | Flyway |
| Build | Gradle |
| Containerization | Docker Compose |
| Resilience | Resilience4j |
| Testing | JUnit 5, Testcontainers |

---

## How to Run

```bash
docker compose up -d        # PostgreSQL + Redis
./gradlew bootRun           # default profile (dev)
```

Production profile:
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

---

## Architecture

Hexagonal architecture (Ports & Adapters):

```
api/             # Controllers, DTOs, validation, exception handling
domain/          # Business logic, domain exceptions, ports (interfaces)
infrastructure/  # JPA repositories, Redis client, IP geolocation client
config/          # Profile-based configuration, conditional beans
```

Domain layer has zero dependency on Spring or infrastructure. Business rules are testable without application context.

---

## Key Design Decisions

### 1. Concurrency — CAS instead of Optimistic Locking

Coupon usage increment is handled via a single atomic SQL statement:

```sql
UPDATE coupon 
SET current_uses = current_uses + 1 
WHERE code = :code AND current_uses < max_uses
```

Returns number of affected rows. If `0` → coupon exhausted. This eliminates version conflicts, retry storms, and the need for distributed locks. One round-trip to the database, no race condition possible.

Resilience4j Retry is configured for the geolocation external call — not for the DB operation.

---

### 2. IP Geolocation — Fail Strategy per Environment

External service: `ip-api.com` (free tier, ~45 req/min without API key).

Results are cached in Redis with configurable TTL (default: 1h) to reduce external calls and protect against rate limiting.

Failure strategy is configurable via `@ConditionalOnProperty`:

| Profile | Strategy | Behavior |
|---|---|---|
| dev | fail-open | Request passes through, warning logged |
| prod | fail-closed | 503 returned with explicit message |

**Known limitation:** free tier rate limit can be hit on cache cold start with high unique IP volume. Production recommendation: upgrade to paid plan or self-host a GeoIP database (e.g. MaxMind GeoLite2).

---

### 3. Client IP Extraction

Handled by `IpExtractor` with the following priority:

1. `X-Forwarded-For` (first token)
2. `X-Real-IP`
3. `request.getRemoteAddr()`

**Production note:** `X-Forwarded-For` can be spoofed by the client. In a real deployment, the application should sit behind a load balancer (e.g. nginx with `real_ip_module`) and trust only a configured IP range. The header should be set exclusively by infrastructure, not accepted blindly from the client.

---

### 4. Coupon Code Case Insensitivity

Codes are normalized to uppercase on both write and read. `WIOSNA` and `wiosna` resolve to the same coupon. Stored as uppercase in the database, unique constraint on the column.

---

### 5. User Identity

`userId` is accepted as an arbitrary string — validated for presence and max length. There is no cryptographic verification of identity.

**Known limitation:** a client can supply any `userId` and bypass the per-user usage limit. In production this requires an authentication layer (e.g. JWT with verified subject claim). This is explicitly out of scope per task requirements.

---

### 6. Audit Log — CouponUsage Table

Enabled by default. Can be disabled via `features.audit.enabled=false`.

Stores: `userId`, `couponCode`, `usedAt`, `sourceIp`.

Source IP is pseudonymized — last octet zeroed (e.g. `192.168.1.123` → `192.168.1.0`) to reduce GDPR exposure. `userId` linked to IP is personal data — full audit log in production requires a data retention policy.

---

### 7. Validation Order in Redeem Flow

Operations ordered from cheapest to most expensive:

1. Coupon exists → 404
2. Coupon active + not expired → 409
3. User already used this coupon → 409
4. IP geolocation check → 403 *(external call — intentionally last before write)*
5. CAS increment → 409 if exhausted
6. CouponUsage insert — same `@Transactional` boundary as step 5

---

### 8. Error Responses

All errors return a consistent structure — no stack traces exposed to the client:

```json
{
  "code": "COUPON_EXHAUSTED",
  "message": "Coupon has reached its usage limit"
}
```

---

## Configuration

All tuneable parameters are externalized to `application.yml`. Key properties are documented inline with comments — see `application.yml`, `application-dev.yml`, `application-prod.yml`.

---

## Testing Strategy

- **Unit tests** — domain logic only, no Spring context, no DB
- **Integration tests** — Testcontainers (PostgreSQL + Redis), full application context
- **Concurrency test** — parallel threads hitting the same coupon simultaneously, asserting `currentUses` never exceeds `maxUses`

Concurrency test is the most important one given the nature of this service.

Known Limitations
User identity is not verified
userId is accepted as an arbitrary string. Any client can supply another user's identifier and bypass the per-user usage limit. Resolving this requires an authentication layer (e.g. Spring Security + JWT) where userId is extracted from a verified token subject — explicitly out of scope per task requirements.

Geolocation accuracy
IP-based geolocation is inherently imprecise. VPNs, proxies, and corporate networks can produce incorrect country resolution. This is a known limitation of any IP-based geographic restriction and is not specific to this implementation.
Free tier rate limiting on ip-api.com
The free tier allows ~45 requests/minute per IP without an API key. Redis caching with a 1h TTL significantly reduces external call volume, but a cold start with high unique IP traffic can hit this limit. Production recommendation: MaxMind GeoLite2 self-hosted database — no external dependency, no rate limit, sub-millisecond resolution.
No authentication on coupon creation endpoint
POST /api/v1/coupons is publicly accessible. Per task requirements, authentication is out of scope. In production this endpoint requires at minimum an API key or OAuth2 client credentials flow to prevent unauthorized coupon creation.
X-Forwarded-For can be spoofed
The application reads client IP from request headers which can be manipulated by the client. In production the application must sit behind a trusted load balancer that overwrites this header — the application should never be exposed directly to the internet.
