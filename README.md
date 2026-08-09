# iris-api

Backend service for **I.R.I.S** (Intelligent Rest & Insight Suite) — a diet
and wellness tracking app.

- **Java**: 21 (LTS)
- **Framework**: Spring Boot 3.3
- **Build**: Maven
- **Database**: PostgreSQL (Supabase Postgres for hosted, local Docker for dev)
- **Auth**: JWT (access + refresh), email/password + Google Sign-In
- **Schema management**: Flyway
- **API docs**: OpenAPI 3 via springdoc — Swagger UI at `/swagger-ui.html`

## Architecture

Package-by-feature. Each feature owns its own model, repository, service, and
controller — no shared `/controllers` or `/services` folders.

```
com.iris
├── IrisApiApplication.java
├── common/              — cross-cutting: security, config, exception handling
├── auth/                — signup, login, refresh, Google OAuth
├── user/                — profile, preferences
├── nutrition/           — food and drink logging (reference implementation)
├── hydration/           — water logging
├── weight/              — weight + BMI
├── sleep/               — sleep logging
├── progress/            — cross-feature aggregation (dashboards, streaks)
└── notification/        — reminders + push delivery
```

Features marked as skeletons (README only) follow the same pattern as
`nutrition` — copy it as your template.

## Running locally

**Prerequisites**: JDK 21, Docker (for local Postgres), a Google OAuth client ID.

```bash
# 1. Start Postgres locally
docker run -d --name iris-db \
  -e POSTGRES_DB=iris \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16

# 2. Copy env template and fill in values
cp .env.example .env
# edit .env — set JWT_SECRET and GOOGLE_CLIENT_ID

# 3. Load env and run
export $(cat .env | xargs)
./mvnw spring-boot:run
```

App boots on `http://localhost:8080`. Swagger UI at
`http://localhost:8080/swagger-ui.html`.

## API surface (v1)

| Feature     | Base path                          |
| ----------- | ---------------------------------- |
| Auth        | `/api/v1/auth/*`                   |
| Users       | `/api/v1/users/me`                 |
| Nutrition   | `/api/v1/nutrition/entries`        |
| Hydration   | `/api/v1/hydration/entries`        |
| Weight      | `/api/v1/weight/entries`           |
| Sleep       | `/api/v1/sleep/entries`            |
| Progress    | `/api/v1/progress/{daily,weekly}`  |
| Reminders   | `/api/v1/reminders`                |

Every endpoint except `/api/v1/auth/**` and `/actuator/health` requires a
`Authorization: Bearer <access-token>` header.

## Adding a new feature

1. Add a Flyway migration under `src/main/resources/db/migration/`
   (e.g. `V2__add_workouts.sql`).
2. Copy the `nutrition/` package as a template.
3. Rename the package and classes.
4. Wire endpoints under `/api/v1/<feature>`.

## Deploying

TBD — first drop targets local dev + a hosted Postgres (Supabase). Production
hosting options: Railway, Fly.io, Render.

## License

Private / TBD.
