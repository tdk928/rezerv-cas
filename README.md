# rezerv-cas

Central Auth Service на платформата **REZERV** — B2B/B2C резервации за салони/бръснарници.

- **Порт:** 8081 (зад gateway `/api/auth/**`)
- **Стек:** Java 21, Spring Boot 3.x (MVC, virtual threads), PostgreSQL (`cas_db`), Redis (refresh tokens), Flyway
- **Отговорност:** register/login, access JWT (15 мин) + refresh token с rotation, роли, `/me`. Единственият сервиз, който пипа пароли.

## Документация

- Обща архитектура: `REZERV.md` в родителската папка (single source of truth)
- История на branch-овете: [`docs/PROJECT_LOG.md`](docs/PROJECT_LOG.md)
- Postman колекция: [`postman/`](postman/)

## Git workflow

`feature/* → development → test → master`

## Локално стартиране

```bash
# инфраструктура (PostgreSQL :5433, Redis :6380) — общ docker-compose на проекта
mvn spring-boot:run
```
