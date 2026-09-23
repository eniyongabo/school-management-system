# Verification results

Verified on **2026-09-23**.

| Check | Result |
|---|---|
| Maven `verify`, H2 2.3 in MySQL mode | **8 tests passed**, no failures/errors/skips; executable JAR built |
| Same integration suite, isolated MySQL 9.0.1 | **8 tests passed**, no failures/errors/skips |
| Frontend Node unit tests | **2 tests passed** |
| React/Vite production build | **Passed**; assets generated in `frontend/dist` |
| Playwright with installed headless Chrome | **3 tests passed** against the real React client and isolated H2 API |
| Desktop/mobile visual inspection | Final mobile CSS recheck passed; administrator workflow and mobile overflow assertion passed; screenshots refreshed |

## Covered behavior

- Application startup, migrations, JPA validation, role seeding and unauthenticated access.
- Parent registration without role escalation; email verification and single-use tokens.
- Password reset, login/logout, account deactivation and token revocation.
- Expired JWTs and JWTs with incorrect audiences are rejected.
- Enrollment, duplicate active-enrollment rejection, marks and weighted averages.
- Attendance updates preserve one record per student/class/date.
- Student submissions and parent/teacher access boundaries.
- A shared student's enrollment history does not expose another teacher's class.
- The teacher pending-grade count decreases when the student's first mark is entered.
- Mock payment retries are idempotent; overpayments and unrelated payers are rejected.
- Authorized receipt download, class notifications and permitted messaging.
- Browser login/logout, school record creation, selectors, event creation/editing,
  anonymous route protection, mobile navigation, parent child isolation, payment and receipt flow.

## Environment and limits of verification

The host has **Java 22.0.1**; Maven compiled using Java **release 17**. A separate
Java 17 runtime was not available on the host. Dockerfiles target Java 17, but the
Docker Compose stack itself was not executed because Docker was unavailable.
The database suite used the installed MySQL **9.0.1** server in its own temporary
data directory on port 13316; the Compose image targets MySQL **8.4**.

SMTP delivery is replaced by Mockito in integration tests. Mailpit configuration
is supplied for local verification/reset workflows, but actual SMTP delivery and
real payment providers were not exercised. Payments are intentionally mocked.

The temporary MySQL server was shut down after testing. Playwright manages and
stops its own test API and frontend servers. Production application data was not used.

Screenshots: [administrator](screenshots/administrator.png),
[parent](screenshots/parent.png), [mobile](screenshots/mobile.png).
See the [README](../README.md) for reproducible commands and
[architecture](architecture.md) for implementation scope and extensions.
