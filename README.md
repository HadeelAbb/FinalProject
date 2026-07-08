# HSTS - High School Test System

Group 5, course 203.3140 (Software Engineering), University of Haifa.

## Status (as of this repo's creation)

**Done and working (Partner 3 / client scope - complete):**
- Login/logout (SUC-9)
- Question bank management - create/edit/delete/search (SUC-1, SUC-16)
- Full client GUI for the exam lifecycle: build manual/auto (SUC-2/3),
  approval (SUC-4), taking with a live timer (SUC-6), grading (SUC-7/8),
  results (SUC-10), extending time mid-exam (SUC-17)
- Study bot: ask/history (SUC-14/15), anonymized teacher-facing usage
  stats (SUC-13)
- All of the above tested end-to-end against `MockServerSimulator` -
  see `src/test/java/.../SmokeTest.java`, 45 checks, all passing

**Not built (out of client scope, needed from teammates):**
- Real server-side implementation (DB-backed controllers replacing the
  mock) - see "Division of work" below
- Real external AI API call for the bot (currently a canned placeholder
  answer - see the TODO in `MockServerSimulator.handleAskBot`)

## Division of work

The shared foundation (entities in `shared/model`, protocol in
`shared/net`, `EventBus` skeleton) is the contract everyone builds
against. From there:

- **Server / DB (Partner 1):** replace `MockServerSimulator`'s in-memory
  logic with real controllers backed by MySQL - build/approve/take/grade
  logic using the same DTOs. A lightweight repository layer instead of
  raw SQL scattered across controllers is recommended.
- **Protocol / networking (Partner 2):** wire the new `Command`s into
  `ServerRequestRouter`, and finish `EventBus` targeted delivery (see
  the TODO in `EventBus.java` - needs a userId -> connection registry).
- **Client / GUI (Partner 3):** done - login, question bank, full exam
  lifecycle, and the bot, all tested against the mock server.

## Running

Client: `com.hsts.client.MainApp` (JavaFX, needs a JDK with JavaFX
bundled, e.g. Liberica Full JDK 17, or `--module-path`/`--add-modules`
with separate JavaFX jars).

Server: `com.hsts.server.MainServerApp` (needs a running MySQL instance
matching `init.sql`, plus `mysql-connector-java` on the classpath).

Mock mode (no server/DB needed): flip `USE_MOCK_SERVER` to `true` in
`MainApp.java`. Seeded accounts: `teacher1`/`pass123`,
`coordinator1`/`pass123`, `student1`/`pass123` (all `pass123`).
