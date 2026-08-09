# weight

Weight logging and BMI derivation. Table `weight_entries` is in `V1__init_schema.sql`.

Mirror the `nutrition` package structure:

- `model/WeightEntry.java`
- `WeightRepository.java`
- `dto/WeightEntryRequest.java` — `{ weightKg, recordedAt, notes }`
- `dto/WeightEntryResponse.java` — include derived BMI (needs
  `user.heightCm` — inject `UserRepository`)
- `WeightService.java`
- `WeightController.java` — `/api/v1/weight/entries` + `/latest`

**BMI**: `weightKg / (heightM * heightM)`. If `heightCm` is null, return BMI
as `null` and let the client prompt the user to set it.
