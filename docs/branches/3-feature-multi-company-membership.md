# 3-feature-multi-company-membership

## Цел
Един user да може да е owner на **много фирми** (типичен BG случай: всеки салон =
отделен ЕИК). `users.company_id` става **активната** фирма (JWT / `X-Company-Id`).

## Направено
- Flyway **`V2__user_companies.sql`**: таблица `user_companies (user_id, company_id)` +
  backfill от старото `users.company_id`.
- **`CompanyAssignmentService.assignCompany`**: добавя membership; **не** отказва при
  вече съществуваща друга фирма; отказва само дублиран `(user, company)`;
  новата фирма става активна; добавя `BUSINESS_OWNER`.
- **`POST /auth/switch-company`** `{ companyId }` → сменя активната + **нов JWT**
  (AuthResponse). Грешка `NOT_COMPANY_MEMBER` ако няма membership.
- **`UserResponse`**: поле `companyIds: Long[]` (всички membership-и) + `companyId` (активна).

## Решения
- JWT остава с **един** `companyId` (gateway без промяна) — „контекстът“ е активната фирма.
- Имена/ЕИК на фирмите живеят в business; CAS държи само ID-та.

## Как се тества
```bash
./mvnw test

# След login + onboarding на 2 фирми:
curl -X POST http://localhost:8080/api/auth/switch-company \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"companyId":2}'
```

## За frontend-а
- `user.companyIds` + `user.companyId` от login/refresh/me/switch-company.
- След switch → запази новия access+refresh token.
- Списък с имена: `GET /api/business/companies/mine`.
