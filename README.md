# MC Deck Builder API

REST API for [MC Deck Builder](https://github.com/marlondvg/mc-deck-builder), a fan deck builder for
Marvel Champions: The Card Game. It keeps user accounts, each user's collection (owned packs) and
their decks. Card data comes from the public [MarvelCDB](https://marvelcdb.com) API and is not stored here.

Unofficial fan project, not affiliated with Fantasy Flight Games or Marvel.

## Stack

Java 21, Spring Boot 4.1, Gradle, Spring Security (JWT), Spring Data JPA, Flyway,
H2 for local runs and tests, PostgreSQL in production. See [AGENTS.md](AGENTS.md) for the
architecture and conventions.

## Run locally

```bash
JWT_SECRET=$(openssl rand -base64 48) ./gradlew bootRun   # http://localhost:8080
./gradlew test
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- `CORS_ALLOWED_ORIGINS` defaults to `http://localhost:5173` (the Vite dev server).

## Endpoints

| Method | Path                 | Auth  | Description                        |
|--------|----------------------|-------|------------------------------------|
| POST   | `/api/auth/register` | no    | Create an account                  |
| POST   | `/api/auth/login`    | no    | Log in, returns a bearer JWT       |
| GET    | `/api/users/me`      | token | The logged-in user                 |

Collection and deck endpoints are coming next (see `docs/specs`).
