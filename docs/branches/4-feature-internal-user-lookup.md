# 4-feature-internal-user-lookup

## Цел
Service-to-service lookup на user профили за business admin панел
(име/email на собственик на фирма).

## Направено
- **`UserSummaryResponse`**: `id`, `email`, `firstName`, `lastName` (без парола/роли).
- **`GET /internal/users/{userId}`** → един user.
- **`POST /internal/users/lookup`** `{ "ids": [1,2,3] }` → списък (batch).
- `UserQueryService` + unit/WebMvc тестове.

## Решения
- Gateway **не** route-ва `/internal/**` — само business → CAS директно.
- Batch endpoint за да не се прави N+1 при admin списък с много фирми.

## Как се тества
```bash
./mvnw test
curl http://localhost:8081/internal/users/1
curl -X POST http://localhost:8081/internal/users/lookup \
  -H 'Content-Type: application/json' -d '{"ids":[1,5]}'
```
