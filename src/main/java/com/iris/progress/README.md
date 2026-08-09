# progress

Cross-feature aggregation for the dashboard and progress screens. This feature
owns no tables — it reads from `nutrition`, `hydration`, `weight`, `sleep`.

Endpoints to build:

- `GET /api/v1/progress/daily?date=YYYY-MM-DD&timezone=Asia/Kolkata`
  Returns totals for one day: calories, macros (P/C/F), fiber, caffeine, water
  (from both `water_entries` and `food_entries.fluid_ml`), sleep duration,
  latest weight.

- `GET /api/v1/progress/weekly?from=YYYY-MM-DD&to=YYYY-MM-DD&timezone=...`
  Same totals per day for a range, for charts.

- `GET /api/v1/progress/streaks`
  Streak counts (days hitting water goal, days hitting calorie goal, etc.).

**Design note**: keep aggregation logic in a `ProgressService` that calls into
each feature's service (not repository) — this keeps feature boundaries clean
and makes it easy to extract progress into its own service later if needed.
