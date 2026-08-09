# hydration

Water tracking. Table `water_entries` is already in `V1__init_schema.sql`.

To build this out, mirror the `nutrition` package:

- `model/WaterEntry.java` — JPA entity for `water_entries`
- `HydrationRepository.java` — Spring Data repo with a
  `findByUserIdAndConsumedAtBetweenOrderByConsumedAtDesc` method
- `dto/WaterEntryRequest.java` — `{ amountMl, consumedAt }`
- `dto/WaterEntryResponse.java`
- `HydrationService.java` — CRUD + daily total
- `HydrationController.java` — `POST/GET/PUT/DELETE /api/v1/hydration/entries`

**Note:** the daily total shown to the user should include `fluid_ml` from
`food_entries` where `fluid_ml IS NOT NULL` — coffee, juice, and other drinks
logged under nutrition count toward hydration.
