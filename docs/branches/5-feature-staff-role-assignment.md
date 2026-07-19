# feature/staff-role-assignment

## Цел
Свързване на съществуващ CAS user като STAFF към фирма (за служители в business).

## Направено
- `GET /internal/users/by-email?email=` → `UserSummaryResponse`
- `POST /internal/users/{userId}/assign-staff` body `{ companyId }` — membership + роля `STAFF`
  (идемпотентно; не пипа активния `companyId`, ако вече е зададен)
- Unit тестове за assignStaff + getByEmail

## Решения
- Ролята `STAFF` вече е seed-ната в V1 — няма нова Flyway миграция.
- Staff запазва и `CLIENT` (много роли в JWT).

## Как се тества
```bash
./mvnw -q test
```

## За frontend / business
Business вика: lookup by email → assign-staff → създава `staff_members` ред.
