# Schoolhouse — School Management System

A Java 17 / Spring Boot / MySQL REST API with a responsive React application for
administrators, teachers, parents, students, and school staff/accountants. The
original application in the parent directory is preserved.

## Implemented features

- **Accounts:** parent registration, email verification, login/logout, BCrypt passwords,
  JWT validation and revocation, reset/change password, profile editing, last login,
  administrator provisioning, role assignment and activation/deactivation.
- **School records:** parent/guardian links and approval, student profiles, teachers,
  staff, academic years, terms, classes, subjects, teaching assignments, enrollment,
  capacity checks, and weekly schedules with collision detection.
- **Learning:** assignments and deadlines, text submissions, private learning-material
  upload/download, assessment marks, weighted subject/term averages, letter grades,
  attendance and report cards.
- **Billing:** fee structures, student invoices, outstanding balances, payment history,
  idempotent **mock payments**, and downloadable receipts. No real money is processed.
- **Communication:** school/class announcements, recipient notifications and read state,
  permitted parent–teacher and teacher–administrator messages, and calendar event CRUD.
- **Interface:** role-based navigation and dashboards, search, paginated lists,
  editable records, mobile layouts, dark/light mode, profile management, CSV reports.
- **Engineering:** Flyway SQL migrations, JPA relationships, DTOs, Bean Validation,
  transactional services, API errors, audit records, JUnit/Mockito integration tests,
  and Playwright browser tests.

## Run locally on port 8080 without Docker

With Python 3, Java, MySQL server/client, and Node installed, after building the
backend JAR and installing frontend dependencies, run:

```sh
python3 scripts/start-local.py
```

Open **http://localhost:8080** and use the administrator credentials printed by the
launcher. It starts a dedicated MySQL database on port 13317, Spring Boot on 8081,
and the React development server on 8080. Verification/reset emails stay in the
local inbox at **http://localhost:8025**; no external mail is sent.

Data, private generated credentials, process IDs, and logs persist in the ignored
`.local/` directory. Services continue running after the launcher exits.

## Quick start: Docker Compose

Requires Docker with Compose. Run from this directory:

```sh
cp .env.example .env
openssl rand -base64 32
```

Edit `.env`: replace both database passwords, set `JWT_SECRET` to the generated
value, and choose `ADMIN_EMAIL` and a strong `ADMIN_PASSWORD` (at least 12 characters).
Keep `.env` private; it is ignored by Git.

```sh
docker compose up --build -d
```

- Application: **http://localhost:3000**
- API health: **http://localhost:8080/actuator/health**
- Local email inbox: **http://localhost:8025** (Mailpit captures verification/reset mail)

Sign in using the administrator credentials you configured. Bootstrap runs only
when the users table is empty. Subsequent starts preserve accounts and data.
`docker compose logs -f api` shows API startup; `docker compose stop` stops services
without deleting the MySQL volume. The Dockerfiles use Java 17 and Node 22.

## Local development

Requires JDK 17+, Maven 3.6.3+, Node 22.12+ and MySQL 8.4+.

1. Start the supporting services: `docker compose up -d mysql mailpit`.
2. Load the edited `.env` and start the API:

   ```sh
   set -a
   . ./.env
   set +a
   mvn -f backend/pom.xml spring-boot:run
   ```

3. In another terminal:

   ```sh
   npm --prefix frontend ci
   npm --prefix frontend run dev
   ```

4. Open **http://localhost:5173**. Vite forwards `/api` to port 8080.

Without Docker, create a MySQL database and application user, then export `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`, and SMTP
settings. Use a mail server for verification/reset; login itself does not send mail.

| Variable | Default / purpose |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/school_management?connectionTimeZone=UTC` |
| `DB_USERNAME` | `school_app` |
| `DB_PASSWORD` | Required |
| `JWT_SECRET` | Required; base64 encoding of at least 32 random bytes |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Optional first-run administrator bootstrap |
| `FRONTEND_ORIGIN` | `http://localhost:5173`; must match the browser's origin exactly |
| `SMTP_HOST`, `SMTP_PORT` | `localhost`, `1025` |
| `SMTP_USERNAME`, `SMTP_PASSWORD` | Empty for local Mailpit |
| `SMTP_AUTH`, `SMTP_TLS` | `false`; configure for your production SMTP service |
| `MAIL_FROM` | `school@localhost` |
| `PORT` | `8080` |
| `VITE_API_URL` | Frontend API base; `/api` by default |

## Walk through a complete school workflow

1. As administrator, create parent, teacher, student and accountant accounts in
   **User accounts**. Public self-registration creates a parent role only and requires
   email verification. Roles with privileged access are provisioned by an administrator.
2. Create teacher/staff profiles linked to the corresponding accounts.
3. Create an academic year, term, class, subject and teaching assignment.
4. Create a student and link a parent plus an optional student login. Alternatively,
   a parent may register a child. Administrator-created student records are active;
   parent-created records are pending until approved.
5. Open the student profile to activate the student and verify guardian links.
   An unverified link never grants access to academic records.
6. Enroll the student in a class. Create a class schedule if desired.
7. Sign in as the assigned teacher to create assignments, upload materials, enter
   marks and record attendance. Students can submit text before assignment deadlines.
8. Create a fee structure and invoice as an administrator/accountant. Sign in as the
   linked parent, make a mock payment, and download its receipt.
9. Post announcements, exchange permitted messages, add calendar events, and review
   role-specific dashboards and reports.

The frontend holds its access token in memory. Reloading the page signs you out.
Access tokens expire after 30 minutes. Logout, password changes, resets, and access
changes invalidate previously issued tokens immediately.

## Test and build

```sh
mvn -f backend/pom.xml verify
npm --prefix frontend test
npm --prefix frontend run build
```

The backend tests apply both SQL migrations and validate JPA mappings against H2
in MySQL mode. They cover authentication lifecycle, account deactivation,
student isolation, teacher/class isolation, enrollment, marks, attendance,
submissions, mock payment retries and overpayment, receipts, and communications.
Mockito replaces account email delivery during the workflow tests.

To run the same backend suite against a **dedicated disposable MySQL database**:

```sh
mvn -f backend/pom.xml test \
  -Dspring.datasource.url='jdbc:mysql://localhost:3306/school_test?connectionTimeZone=UTC' \
  -Dspring.datasource.username=school_test \
  -Dspring.datasource.password="$MYSQL_TEST_PASSWORD" \
  -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

Never point this test command at your application database. Test fixtures use
transactions and rollback; Flyway still creates the schema in the selected database.

For end-to-end browser checks:

```sh
cd frontend
npx playwright install chromium
npm run test:e2e
```

Playwright starts a loopback-only H2 test API on port 18080 and Vite on port 15173,
then stops them. Test credentials and signing keys in `scripts/browser-api.sh` are
exclusively for that disposable test server. An installed Chrome can be used via
`PLAYWRIGHT_CHROME_PATH`. Set `SCHOOL_MAVEN_REPO` only if you use a custom Maven cache.
Screenshots are saved under `frontend/test-results/`.

The executable backend is `backend/target/school-management-api-0.1.0-SNAPSHOT.jar`;
frontend production assets are in `frontend/dist/`.

## Screenshots

[Administrator dashboard](docs/screenshots/administrator.png) · [Parent dashboard](docs/screenshots/parent.png) · [Mobile layout](docs/screenshots/mobile.png)

## Project map

```text
school-management/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/schoolmanagement/
│       │   ├── config/             # Security, JWT, CORS, bootstrap, rate limit
│       │   ├── identity/           # Account entities, repositories, DTOs, service, API
│       │   ├── common/             # API error handling
│       │   └── school/
│       │       ├── controller/    # REST endpoints and downloads
│       │       ├── dto/           # Validated inputs and scalar-only responses
│       │       ├── service/       # Authorization and business transactions
│       │       ├── repository/    # JPA persistence and parameterized queries
│       │       └── entity/        # Relational domain model
│       ├── main/resources/db/migration/
│       └── test/                  # JUnit/Mockito integration checks
├── frontend/src/
│   ├── components/                # Forms, dialogs, tables
│   ├── pages/                     # Dashboards and school workflows
│   ├── services/                  # Axios and downloads
│   ├── hooks/                     # API loading/cancellation
│   ├── context/                   # In-memory authentication
│   ├── layouts/                   # Responsive role-aware shell
│   ├── routes/                    # Authentication/role guards
│   └── utils/                     # Module forms, navigation, formatting
├── frontend/tests/                # Playwright browser workflows
├── scripts/                       # Isolated browser-test API
├── docs/                          # Architecture, ER model, API, delivery stages
└── compose.yaml                   # MySQL, Mailpit, API, Nginx/React
```

See [architecture and limits](docs/architecture.md), [database design](docs/database-design.md),
[API guide](docs/api.md), and [increment record](docs/increments.md).
