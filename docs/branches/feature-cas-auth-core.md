# feature/cas-auth-core

## Цел

Първоначален скелет на rezerv-cas + пълния auth flow по REZERV.md: register, login,
refresh с rotation, `/me`, роли, JWT.

## Направено

- **Maven проект**: Java 21, Spring Boot 3.5.3, virtual threads (`spring.threads.virtual.enabled=true`).
  Зависимости: web, data-jpa, postgresql, flyway, validation, security, data-redis, lombok, actuator, jjwt 0.12.6.
- **Flyway `V1__init_users_roles.sql`**: таблици `users`, `roles`, `user_roles`; seed на 4-те роли
  (PLATFORM_ADMIN/BUSINESS_OWNER/STAFF/CLIENT) + dev админ `admin@rezerv.bg` / `admin123`.
- **Entities** (Lombok, никакъв `@Data`): `User`, `Role`; enum `UserStatus` (ACTIVE/BLOCKED).
- **Endpoint-и** (`AuthController`, gateway маха `/api` префикса):

| Method | Path | Auth | Какво прави |
|--------|------|------|-------------|
| POST | `/auth/register` | публичен | Създава user с роля CLIENT, връща access+refresh+user. 409 `EMAIL_ALREADY_EXISTS`. |
| POST | `/auth/login` | публичен | BCrypt проверка → access JWT (15 мин) + refresh (Redis, 14 д). 401 `INVALID_CREDENTIALS`, 403 `USER_BLOCKED`. |
| POST | `/auth/refresh` | публичен | Rotation: GETDEL стария от Redis, издава нова двойка. 401 `INVALID_REFRESH_TOKEN`. |
| GET | `/auth/me` | token (header `X-User-Id` от gateway) | Профил на текущия user. 401/404. |

- **Response формат**: `AuthResponse { accessToken, refreshToken, expiresInSeconds, user }`,
  `UserResponse { id, email, phone, firstName, lastName, companyId, status, roles[], createdAt }`.
- **JWT** (`JwtService`, jjwt): HS256, claims `sub`/`email`/`roles`/`companyId`/`jti`, TTL 15 мин.
  Secret от env `JWT_SECRET` (dev default по REZERV.md).
- **Refresh rotation** (`RefreshTokenService`): Redis `refresh:<uuid> → userId`, TTL 14 д,
  атомарно `getAndDelete`.
- **Инфра код**: `ContextHeaderFilter` (userId/correlationId → MDC), `ContextHeaders` константи,
  `GlobalExceptionHandler` → единен `ErrorResponse` (секция 2.6), `JwtProperties` record,
  `SecurityConfig` (stateless, permitAll — JWT се валидира в gateway).
- **Конфигурация**: `application.yml` — порт 8081, PG `localhost:5433/cas_db`, Redis `localhost:6380`.

## Решения

- Spring Boot **3.5.3** (най-новата стабилна 3.x от Maven Central; REZERV.md изисква Boot 3.x).
- Паролната политика: мин 8 символа (Jakarta Validation на `RegisterRequest`).
- Email се нормализира до lowercase при register/login.
- `/auth/me` чете `X-User-Id` header (договорът от секция 2.3) — сервизът не парсва JWT сам.

## Как се тества

- `mvn test` — 15 unit теста (AuthService, JwtService, RefreshTokenService), всички зелени.
- Ръчно: PostgreSQL на :5433 (база `cas_db`) + Redis на :6380 → `mvn spring-boot:run` →
  Postman колекцията `postman/rezerv-cas.postman_collection.json` + environment `rezerv-local`
  (Login с dev seed `admin@rezerv.bg`/`admin123`; token-ите се записват автоматично).
