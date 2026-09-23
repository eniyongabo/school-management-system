# Increment record

The project was built in successive groups: foundation; identity; people and academics;
learning; billing; communications/calendar; React; integration and browser verification.
Each entry identifies the implementation files rather than duplicating source code here.

| Increment | Implementation | Primary files |
|---|---|---|
| 1. Architecture and schema | Java 17 build, database/CORS settings, initial identity and school relationships | `backend/pom.xml`, `application.yml`, `V1__identity_and_school_structure.sql` |
| 2. Authentication and RBAC | JWT, BCrypt, registration, verification/reset, profiles, roles, account state, bootstrap | `identity/*.java`, `config/*.java` |
| 3. Parent and student management | Multiple guardians/children, pending enrollment, approval and scoped profiles | `PeopleService`, `SchoolAccess`, `Student`, `Parent` |
| 4. Teacher/staff management | Accounts and separate employee profiles; assigned-teacher access | `Teacher`, `Staff`, `PeopleService` |
| 5. Classes and subjects | Years, terms, class capacity, offerings, enrollment and timetable validation | `AcademicService`, academic entities |
| 6. Assignments and grades | Deadlines, materials, text submissions, assessment marks and weighted averages | `LearningService`, `V2__school_modules.sql` |
| 7. Attendance | Unique daily class records, four statuses, scoped history | `LearningService`, `Attendance` |
| 8. Fees and payments | Fee structures, invoice snapshots, locked mock payments, idempotency and receipts | `BillingService`, `PaymentGateway`, `MockPaymentGateway` |
| 9. Communications | Announcements, recipient notifications, read history, permitted direct messaging | `CommunicationService` |
| 10. Calendar | School events with administrator creation/edit/deletion | `CalendarEvent`, `CommunicationService` |
| 11. React and dashboards | Role routes, reusable UI, API forms, profiles, responsive overview, reports | `frontend/src/` |
| 12. Testing and documentation | JUnit/Mockito workflows, MySQL validation, frontend unit and browser tests, run guides | `backend/src/test`, `frontend/tests`, `docs/`, `README.md` |

## Verification strategy

Foundation tests check startup, schema seeding, database health and unauthorized API
access. Workflow tests cover real controller/service/repository interactions, mocking
SMTP delivery to inspect verification/reset tokens. The same suite can target H2 or
an isolated MySQL database. A regression test checks that shared student history does
not leak another class to a teacher.

Browser tests run the real React client against a dedicated H2-backed API, exercise
sign-in, record creation, selection forms and calendar flows, verify mobile overflow,
and check anonymous route protection. Browser screenshots are local test artifacts.

Use the commands in the README to reproduce checks. Verification results for this
checkout are recorded in `docs/verification.md` after the final run.
