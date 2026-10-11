# Spec: Production deploy on Render

## Goal

Run the API on Render (Docker web service) against the shared `portfolio-db`
PostgreSQL instance, in its own `mcdeck` schema and role, so the frontend on
Vercel can use it.

Out of scope: backups, monitoring, a paid plan.

## What the code already provides

- `Dockerfile`: builds the jar with JDK 21, runs on JRE 21 as a non-root user with
  `SPRING_PROFILES_ACTIVE=prod`.
- `application-prod.yaml`: datasource from `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`;
  Flyway and Hibernate on the `mcdeck` schema with `create-schemas: false`;
  `server.port: ${PORT}`; `forward-headers-strategy: framework` behind Render's proxy.
- CI builds the image and starts it with the prod profile against PostgreSQL on every PR.

## Database (done once, by the database admin)

On the shared database `portfolio_y6hg`, as `portfolio_admin`:

```sql
CREATE ROLE mcdeck_user LOGIN PASSWORD '<generated with: openssl rand -hex 24>';
GRANT mcdeck_user TO CURRENT_USER WITH SET TRUE;
CREATE SCHEMA mcdeck AUTHORIZATION mcdeck_user;
ALTER ROLE mcdeck_user SET search_path = mcdeck;
```

The app role owns only its schema; Flyway keeps its history table inside `mcdeck`.

## Render web service

- Source: this GitHub repo, branch `main`, runtime **Docker**, same region as the database.
- Health check path: `/actuator/health`.
- Environment variables:

| Variable               | Value                                                                  |
|------------------------|------------------------------------------------------------------------|
| `DB_URL`               | `jdbc:postgresql://<internal host>:5432/portfolio_y6hg?currentSchema=mcdeck` |
| `DB_USERNAME`          | `mcdeck_user`                                                          |
| `DB_PASSWORD`          | the role's password                                                    |
| `JWT_SECRET`           | `openssl rand -base64 48`, used only in production                     |
| `CORS_ALLOWED_ORIGINS` | `https://mc-deck-builder.vercel.app`                                   |

Use the **internal** database host (not the external one) and the `jdbc:postgresql://` form.

## Known limits

- Free web services sleep after about 15 minutes without traffic; the first request
  then takes 30 to 60 seconds. The frontend shows a "connecting" message meanwhile.
- Render's free PostgreSQL expires 30 days after it was created (plus 14 days of grace).
  The shared `portfolio-db` must move to a paid plan or another provider before then,
  or every app on it loses its data.

## Acceptance criteria

- [ ] Startup logs show Flyway migrating schema `mcdeck` to version 2.
- [ ] `GET /actuator/health` on the public URL answers `UP`.
- [ ] Register, login, collection and deck calls work on the public URL.
- [ ] A CORS preflight from `https://mc-deck-builder.vercel.app` is allowed.
- [ ] `\dt mcdeck.*` lists the tables; `\dt public.*` lists none of this app's tables.
