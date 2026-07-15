# 2-feature-company-assignment-internal

## Цел
След B2B onboarding в rezerv-business (`POST /business/companies`) user-ът в cas_db
трябва да получи `company_id` и роля `BUSINESS_OWNER`, за да JWT/refresh включват
`companyId` → gateway `X-Company-Id`.

## Направено
- **`POST /internal/users/{userId}/assign-company`** — service-to-service (gateway НЕ го route-ва).
  Body: `{ "companyId": <long> }` → `UserResponse` с обновени roles + companyId.
- **`CompanyAssignmentService`**: проверки ACTIVE user, без вече зададен companyId (409),
  добавя `BUSINESS_OWNER` (запазва `CLIENT`), задава `company_id`.
- **`AssignCompanyRequest`** record с Jakarta Validation.
- Тестове: `CompanyAssignmentServiceTest` (3), `InternalUserControllerTest` (1).
  Общо 19 unit теста в repo-то.

## Решения
- **Internal endpoint**, не public `/auth/*` — само rezerv-business вика след като е проверил
  ownership при създаване на фирмата (anti-spoofing).
- Няма Flyway миграция — schema-та вече има `users.company_id` и роля `BUSINESS_OWNER`.
- Frontend трябва да **refresh-не token-а** след onboarding, за да получи нов JWT с `companyId`.

## Договор с rezerv-business

```
POST http://rezerv-cas:8081/internal/users/{userId}/assign-company
Content-Type: application/json
{ "companyId": 123 }
→ 200 UserResponse | 404 USER_NOT_FOUND | 409 COMPANY_ALREADY_ASSIGNED | 403 USER_BLOCKED
```

Business вика synchronously след успешен INSERT в `companies`. При failure business rollback-ва
транзакцията (или връща грешка).

## Как се тества
```bash
mvn test

curl -X POST http://localhost:8081/internal/users/2/assign-company \
  -H "Content-Type: application/json" \
  -d '{"companyId":10}'
```

## За frontend-а
След успешен `POST /api/business/companies` + assign в cas (от business):
1. `POST /api/auth/refresh` с refresh token → нов access JWT с `companyId` и роля `BUSINESS_OWNER`.
2. `/auth/me` показва `companyId` и roles `["BUSINESS_OWNER","CLIENT"]` (sorted).
