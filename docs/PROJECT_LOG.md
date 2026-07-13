# PROJECT LOG — rezerv-cas

> Паметта на repo-то. Всеки завършен branch добавя ред НАЙ-ОТГОРЕ в таблицата.
> Ползва се от AI агента (за контекст какво е правено до сега) и от frontend-а
> (за референция какви endpoint-и/контракти има бекендът).

| # | Branch | Дата | Обобщение |
|---|--------|------|-----------|
| 1 | `feature/cas-auth-core` | 2026-07-13 | Скелет на сервиза (Boot 3.5.3, Java 21, virtual threads) + пълен auth flow: `POST /auth/register` (CLIENT роля), `POST /auth/login`, `POST /auth/refresh` (Redis rotation, GETDEL), `GET /auth/me` (по `X-User-Id`). JWT HS256 15 мин с claims sub/email/roles/companyId/jti. Flyway V1: users/roles/user_roles + seed роли и dev админ admin@rezerv.bg/admin123. Единен ErrorResponse, ContextHeaderFilter (MDC), Postman колекция. 15 unit теста. Детайли: `docs/branches/feature-cas-auth-core.md` |

Детайли per branch: `docs/branches/<branch-name>.md`
