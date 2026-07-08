# HSTS - High School Test System

Group 5, course 203.3140 (Software Engineering), University of Haifa.

## Status (as of this repo's creation)

**Done and working:**
- Login/logout (SUC-9)
- Question bank management - create/edit/delete/search (SUC-1, SUC-16)
- Full client GUI for the exam lifecycle (SUC-2, SUC-3, SUC-4, SUC-6, SUC-7,
  SUC-8, SUC-10, protocol-level support for SUC-17), tested end-to-end
  against `MockServerSimulator` - see `src/test/java/.../SmokeTest.java`

**Not built yet:**
- Real server-side implementation (DB-backed controllers replacing the
  mock) - see "Division of work" below
- Bot (SUC-13, SUC-14, SUC-15)
- Exam-taking screen doesn't yet expose SUC-17 (extend time) as a button,
  though the protocol/mock support it

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
- **Client / GUI (Partner 3):** done for the scope above; bot screens
  (SUC-13/14/15) still pending.

## Running

Client: `com.hsts.client.MainApp` (JavaFX, needs a JDK with JavaFX
bundled, e.g. Liberica Full JDK 17, or `--module-path`/`--add-modules`
with separate JavaFX jars).

Server: `com.hsts.server.MainServerApp` (needs a running MySQL instance
matching `init.sql`, plus `mysql-connector-java` on the classpath).

Mock mode (no server/DB needed): flip `USE_MOCK_SERVER` to `true` in
`MainApp.java`. Seeded accounts: `teacher1`/`pass123`,
`coordinator1`/`pass123`, `student1`/`pass123` (all `pass123`).
