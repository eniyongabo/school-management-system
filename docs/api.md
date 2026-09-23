# REST API guide

Base URL: `http://localhost:8080/api`. Send `Authorization: Bearer <accessToken>`
for protected endpoints. Only account registration/login/verification/reset and
`GET /actuator/health` are public. JSON requests use `Content-Type: application/json`.

## Response conventions

Created resources return 201; reads/updates generally return 200; void updates and
calendar/enrollment deletes return 204. Enrollment DELETE means withdraw, retaining
history. Validation errors return 400 with field errors. Unauthenticated requests
return 401, disallowed operations 403, out-of-scope/missing objects 404, and duplicate
or conflicting writes 409. Oversized uploads return 413 and auth throttling 429.

School collection endpoints accept `page` (zero-based), `size` (1–100), `search`,
`sort`, and `direction=asc|desc`. Supported sort keys are `id`, `name`, `firstName`,
`lastName`, `title`, `dueDate`, `dueAt`, `startAt`, `status`, `amount`, and `date`.

```json
{"items":[],"page":0,"size":25,"totalElements":0,"totalPages":0}
```

User-account search uses `page` and `search`, returns a Spring Page with `content`
and `totalElements`, and has a fixed page size of 25. Child/student-specific histories,
assignment materials/submissions, and reports return scoped arrays. The frontend
supports these documented response shapes.

## Identity

| Method | Path | Request / behavior |
|---|---|---|
| POST | `/auth/register` | `email,password,firstName,lastName`; creates parent and sends verification |
| POST | `/auth/login` | `email,password`; returns `accessToken,expiresIn,user` |
| GET | `/auth/me` | Current user DTO |
| POST | `/auth/logout` | Revokes all current tokens for this user |
| PUT | `/auth/profile` | `firstName,lastName,phone` |
| POST | `/auth/change-password` | `currentPassword,newPassword`; signs out active sessions |
| POST | `/auth/forgot-password` | `email`; generic success message |
| POST | `/auth/reset-password` | `token,password`; single-use reset |
| POST | `/auth/resend-verification` | `email`; generic success message |
| POST | `/auth/verify-email` | `token`; single-use verification |
| GET, POST | `/users` | Administrator list/provision; create adds `role` to registration fields |
| PUT | `/users/{id}/access` | Administrator; `active,roles[]`; cannot change own access |

Passwords require 12–72 characters and at most 72 UTF-8 bytes. Public registration
cannot select roles. Verification and reset links put the secret in the URL fragment,
which the client sends in the POST body.

## People and academics

| Paths | Methods / access |
|---|---|
| `/students`, `/students/{id}` | Scoped GET; administrator/parent POST collection; administrator PUT item |
| `/students/{id}/status` | Administrator PUT `{status}` |
| `/students/{id}/guardians` | Administrator GET / PUT `{parentId,relationship,verified}` |
| `/parents` | Administrator/parent GET |
| `/parents/{id}` | Administrator or own parent PUT `{address}` |
| `/parents/{id}/children` | Administrator or own parent GET; verified links only |
| `/teachers`, `/staff` | Scoped GET; administrator POST and PUT `/{id}` |
| `/academic-years`, `/terms`, `/classes`, `/subjects`, `/offerings`, `/schedules` | Scoped GET; administrator POST and PUT `/{id}` |
| `/enrollments` | Scoped GET; administrator POST `{studentId,classId}` |
| `/enrollments/{id}` | Administrator DELETE to withdraw |

Representative payloads:

```json
{"firstName":"Amina","lastName":"Learner","dateOfBirth":"2015-01-20","gender":"FEMALE","address":"12 School Lane","emergencyContactName":"Taylor Learner","emergencyContactPhone":"555-0100","parentId":1,"userId":null}
```

```json
{"academicYearId":1,"name":"Grade 5A","gradeLevel":"5","capacity":30}
```

```json
{"classId":1,"subjectId":1,"teacherId":1}
```

`TeacherInput` requires `userId,employeeNumber` and optional `qualification`;
`StaffInput` requires `userId,employeeNumber,jobTitle`. Years require `name,startDate,endDate`;
terms add `academicYearId`. Subjects require `code,name`. Schedules require
`offeringId,weekday` (Monday=1), `startTime,endTime,room`.

## Learning

| Method | Path | Behavior |
|---|---|---|
| GET, POST | `/assignments` | Scoped list; assigned teacher/admin creates |
| GET, PUT | `/assignments/{id}` | Authorized read/edit |
| GET, POST | `/assignments/{id}/documents` | List/upload learning materials; multipart field `file` |
| GET | `/documents/{id}` | Authorized attachment download |
| GET, POST | `/assignments/{id}/submissions` | Scoped submissions; student submits `{studentId,body}` |
| GET, POST | `/grades` | Scoped list; assigned teacher/admin enters marks |
| PUT | `/grades/{id}` | Assigned teacher/admin updates same student's offering |
| GET | `/grades/student/{studentId}` | Scoped student marks |
| GET | `/grades/student/{studentId}/averages` | Subject/term averages and overall average |
| GET, POST | `/attendance` | Scoped list; teacher/admin upserts student/class/date |
| GET | `/attendance/student/{studentId}` | Scoped attendance history |

```json
{"offeringId":1,"title":"Fractions practice","description":"Show your work.","dueAt":"2026-12-01T18:00:00Z"}
```

```json
{"studentId":1,"offeringId":1,"termId":1,"type":"EXAM","title":"Algebra","score":90,"maximum":100,"weight":1}
```

```json
{"studentId":1,"classId":1,"date":"2026-09-23","status":"PRESENT"}
```

Assessment types: `ASSIGNMENT,QUIZ,EXAM,MIDTERM,FINAL`. Attendance statuses:
`PRESENT,ABSENT,LATE,EXCUSED`.

## Billing

| Method | Path | Behavior |
|---|---|---|
| GET, POST | `/fee-structures` | Administrator/accountant fee management |
| PUT | `/fee-structures/{id}` | Update future fee structure; existing invoices unchanged |
| GET, POST | `/invoices` | Scoped list; administrator/accountant issues invoice |
| GET | `/fees/student/{studentId}` | Scoped invoices and balances |
| GET, POST | `/payments` | Scoped history; parent/admin/accountant mock payment |
| GET | `/receipts/{id}` | Scoped text receipt attachment |

Fee payload: `academicYearId,classId? ,name,amount,currency` (three uppercase letters).
Invoice payload: `studentId,feeStructureId,dueDate`.

Payments require `Idempotency-Key`, a 16–100 character alphanumeric/underscore/hyphen
value. Use a new UUID for each intended payment; reuse the same key for retries of
the same invoice, payer and amount. A reused key with changed data returns 409.

```http
POST /api/payments
Authorization: Bearer <token>
Idempotency-Key: 34ba58ad-f70b-4cd4-8c0a-4700338f5b10
Content-Type: application/json

{"invoiceId":1,"amount":125.00}
```

The response explicitly identifies the mock provider. Never send card details.

## Communication and calendar

| Paths | Methods / behavior |
|---|---|
| `/announcements` | Scoped GET; administrator/assigned teacher POST `classId?,title,body,priority` |
| `/notifications` | Recipient GET; PUT `/{id}/read` |
| `/contacts` | GET permitted messaging contacts |
| `/messages` | Participant GET; POST `recipientId,body`; recipient PUT `/{id}/read` |
| `/events` | Authenticated GET; administrator POST and PUT/DELETE `/{id}` |

Teachers must supply an assigned `classId` when posting announcements. Priorities
are `NORMAL,IMPORTANT,EMERGENCY`. Notifications retain read/unread state.
Events require `title,category,startAt,endAt,allDay`, with optional `description`.
Categories: `HOLIDAY,SCHOOL_DAY,CONFERENCE,EXAM,SPORT,TRIP,MEETING,GRADUATION,DEADLINE`.
Times use ISO-8601 instants with timezone offsets; the client displays local times.

## Dashboards and reports

- `GET /dashboard`: current role's scoped counts, children, academic progress,
  attendance, fee totals by currency, assignments, announcements, notifications and events.
- `GET /reports/{type}`: administrator report array; accountants may access billing types.
- `GET /reports/{type}/export`: CSV download with formula-prefix protection.
- `GET /students/{id}/report-card`: scoped text report card with assessment and subject averages.

Report types: `enrollment,attendance,grades,fees,payments,outstanding,teachers,classes,audit`.
