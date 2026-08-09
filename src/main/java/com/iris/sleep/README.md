# sleep

Sleep logging (manual for v1; HealthKit/Health Connect sync later). Table
`sleep_entries` is in `V1__init_schema.sql`.

Mirror the `nutrition` package structure:

- `model/SleepEntry.java`
- `SleepRepository.java`
- `dto/SleepEntryRequest.java` — `{ sleptAt, wokeAt, quality (1-5), notes }`
- `dto/SleepEntryResponse.java` — include derived duration in minutes
- `SleepService.java`
- `SleepController.java` — `/api/v1/sleep/entries` + `/latest`

**Reminder integration**: the sleep bedtime reminder is scheduled based on
`user.dayEndTime` — that lives in the `notification` feature, not here.
