# feature/create-staff-user

## Цел
Internal endpoint за създаване на нов потребител с роля STAFF, закачен към фирма
(за owner onboarding на служители от rezerv-business).

## Направено
- `POST /internal/users/create-staff` — body: email, password, firstName, lastName,
  phone?, companyId.
- `CompanyAssignmentService.createStaffUser`: нов user с CLIENT+STAFF, password hash,
  `company_id` = companyId, membership в `user_companies`.
- 409 `EMAIL_ALREADY_EXISTS` ако email вече съществува.
- Unit тест: `createStaffUserCreatesAccountWithStaffRole`.

## Решения
- Винаги CLIENT + STAFF (служителят може и да резервира като клиент).
- Само internal (service-to-service); business подава companyId от owned salon.
- Не сменя съществуващи акаунти — за тях остава `assign-staff`.

## Как се тества
```bash
./mvnw -q test
```

## За frontend / business
Не се вика директно от FE. Business: `CasClient` →
`POST {cas}/internal/users/create-staff` → `UserSummaryResponse` (id, email, names).
