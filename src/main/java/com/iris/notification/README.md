# notification

Reminder scheduling and delivery. Tables `reminders` and `device_tokens` are in
`V1__init_schema.sql`.

Two responsibilities:

1. **Reminder CRUD** — user creates/updates reminders (water every 90 min,
   bedtime at day_end - 1h, end-of-day summary at day_end + 30m, etc.).
   `/api/v1/reminders` endpoints.

2. **Push delivery** — a scheduled component that:
   - Runs every minute (`@Scheduled(cron = "0 * * * * *")`)
   - Finds due reminders (respecting user timezone and days_of_week bitmask)
   - Sends push notifications via Expo Push API (v1 mobile is Expo)
     or FCM/APNs directly

Files to build:

- `model/Reminder.java`, `model/DeviceToken.java`
- `ReminderRepository.java`, `DeviceTokenRepository.java`
- `dto/*` for request/response
- `ReminderService.java` — CRUD
- `ReminderController.java` — `/api/v1/reminders`
- `DeviceTokenController.java` — `POST /api/v1/device-tokens` (mobile
  registers its Expo push token on login)
- `ReminderScheduler.java` — `@Scheduled` component that dispatches due reminders
- `push/ExpoPushClient.java` — thin HTTP client for
  `https://exp.host/--/api/v2/push/send`

**Note**: enable scheduling by adding `@EnableScheduling` to
`IrisApiApplication` when this feature ships.
