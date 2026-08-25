# CLAUDE.md — iris-api

Context for Claude Code sessions working in this repo.

## Project

**I.R.I.S** (Intelligent Rest & Insight Suite) — a mobile-first diet and
wellness tracking app. This repo is the **backend REST API**. The mobile app
lives in a sibling repo `iris-mobile` (React Native + Expo).

The mobile client and this API are **decoupled**. This is deliberate — the
mobile app talks only to this REST API, never directly to the database.

## Stack

- Java 21, Spring Boot 3.3, Maven
- PostgreSQL (Supabase Postgres for hosted, local Docker for dev)
- Flyway for schema migrations
- JWT auth (access + refresh, HS256)
- Google Sign-In via `google-api-client` (server-side ID token verification)
- springdoc for OpenAPI 3 / Swagger UI

## Architectural rules — please follow

1. **Package-by-feature.** Each feature owns its own model, repository,
   service, controller, and DTOs. No shared `/controllers` or `/services`
   folders. Cross-feature access goes through the other feature's **service**,
   never its repository.

2. **Controllers stay thin.** They handle HTTP (path binding, validation,
   status codes) and delegate to services. No business logic in controllers.

3. **Services are transactional.** Use `@Transactional` at the service method
   level. `readOnly = true` for reads.

4. **Repositories are Spring Data JPA only.** No custom implementations
   unless a Spring Data derived query genuinely can't express what's needed.

5. **DTOs are Java records.** Use Jakarta Validation annotations on request
   DTOs. Response DTOs have a static `from(Entity)` factory.

6. **Never expose entities directly** in HTTP responses. Always map to a DTO.

7. **User scoping is mandatory.** Every query on user-owned data must filter
   by `userId`. Use `AuthenticatedUser.currentUserId()` in services — do
   not accept `userId` in request DTOs.

8. **Flyway owns the schema.** `spring.jpa.hibernate.ddl-auto=validate`.
   Never modify the DB from Hibernate. Every schema change is a new
   `V<n>__description.sql` file.

9. **Passwords are BCrypt.** Never store plaintext. Password hash is
   nullable on `users` — Google-only accounts have no password.

10. **Refresh tokens are stored as SHA-256 hashes**, never raw. On refresh,
    we rotate: old token is revoked, new pair issued.

## Code quality principles

Apply these to every change, not just new features:

- Correctness first — production-ready code only, not a sketch of the idea.
- Follow SOLID, DRY, KISS, and separation of concerns; prefer composition
  over inheritance where a choice exists.
- Avoid code smells, anti-patterns, unnecessary abstractions, duplicated
  logic, and over-engineering — the simplest robust solution wins over a
  clever or speculative one.
- Follow modern Java 21 / Spring Boot 3.3 idioms and conventions.
- Write self-explanatory code with meaningful names; comment only where
  intent genuinely isn't obvious from the code itself.
- Handle edge cases, validation, error handling, concurrency, and resource
  management explicitly — don't assume the happy path.
- Design for security, observability, reliability, and testability from the
  start, not bolted on after.
- Preserve existing behavior when refactoring unless a behavior change was
  explicitly requested.
- Be mindful of performance and database access patterns (avoid N+1 queries,
  keep indexed columns in mind) for production workloads.
- New or changed logic needs unit/integration test coverage.
- Before proposing a solution, weigh trade-offs briefly and pick the
  simplest robust approach — don't dump an exhaustive options survey.
- If a requirement is ambiguous, ask rather than guessing at something
  risky or hard to reverse.
- Keep responses focused: the implementation and essential explanation
  only — no filler, no speculative changes not asked for.

## Reference implementation

The **nutrition** feature (`com.iris.nutrition`) is the fully-fleshed-out
reference. When building `hydration`, `weight`, `sleep`, or `notification`,
copy that structure. Each of those directories has a `README.md` with
feature-specific notes.

## v1 scope (locked)

- Manual food logging (solid + drinks, with fluid_ml and caffeine_mg fields)
- Water tracking
- Weight + BMI
- Sleep logging (manual)
- Caffeine tracking (as a field on food entries, aggregated by progress)
- Multi-method auth: email + Google (mobile OTP deferred)
- Reminders: water, sleep, end-of-day summary
- Progress visualizations (daily/weekly aggregation endpoints)

## Deferred (do not build unless asked)

- Photo-based nutrient breakdown (v1.1) — will need an LLM integration and
  probably an `image_analysis` feature.
- Recipe analysis (v1.1)
- HealthKit/Health Connect step sync (later)
- Web frontend (later — but the API is already designed to serve it)
- Smoking/alcohol tracking (later)
- Fitness tracker sync (later)
- MCP connection for user data (later)
- Workout tracking (v2)

## Things I appreciate

- Ask before adding a new dependency to `pom.xml`.
- Prefer small, focused PRs — one feature per branch.
- If a schema change touches an existing column, write the migration
  carefully and mention any backfill needed.
- If you're unsure whether something belongs in `common/` or in a feature,
  default to the feature. Extract to `common/` only when a second feature
  needs the same thing.
