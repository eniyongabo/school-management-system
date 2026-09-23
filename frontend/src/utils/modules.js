const A = ["ADMINISTRATOR"],
  T = ["ADMINISTRATOR", "TEACHER"],
  B = ["ADMINISTRATOR", "ACCOUNTANT"],
  P = ["ADMINISTRATOR", "PARENT"];
const f = (name, label, type = "text", extra = {}) => ({
  name,
  label,
  type,
  ...extra,
});
const ref = (name, label, source, extra = {}) =>
  f(name, label, "select", { source, numeric: true, ...extra });
const opt = (name, label, options, extra = {}) =>
  f(name, label, "select", { options, ...extra });
const cols = (...keys) => keys.map((key) => ({ key }));
const classRef = () => ref("classId", "Class", "/classes");
const studentRef = () => ref("studentId", "Student", "/students");
const offeringRef = () =>
  ref("offeringId", "Class & subject", "/offerings", {
    optionLabel: (o) => `${o.className} · ${o.subject} · ${o.teacher}`,
  });
const yearRef = () => ref("academicYearId", "Academic year", "/academic-years");
const dates = [
  f("startDate", "Start date", "date"),
  f("endDate", "End date", "date"),
];
const amount = f("amount", "Amount", "number", { min: "0.01", step: "0.01" });
export const modules = {
  students: {
    title: "Students",
    subtitle: "Every learner, every next step.",
    create: P,
    columns: cols(
      "studentNumber",
      "firstName",
      "lastName",
      "dateOfBirth",
      "status",
    ),
    fields: [
      f("firstName", "First name"),
      f("lastName", "Last name"),
      f("dateOfBirth", "Date of birth", "date"),
      opt(
        "gender",
        "Gender",
        ["FEMALE", "MALE", "NON_BINARY", "PREFER_NOT_TO_SAY"],
        { required: false },
      ),
      f("address", "Address", "textarea", { required: false, maxLength: 500 }),
      f("emergencyContactName", "Emergency contact"),
      f("emergencyContactPhone", "Emergency phone"),
      ref("parentId", "Parent", "/parents", {
        required: false,
        adminOnly: true,
      }),
      ref("userId", "Student login account", "/users", {
        required: false,
        adminOnly: true,
        optionLabel: (u) => `${u.firstName} ${u.lastName} · ${u.email}`,
      }),
    ],
  },
  parents: {
    title: "Parents & guardians",
    subtitle: "The people supporting each student.",
    columns: cols("name", "email", "address"),
  },
  teachers: {
    title: "Teachers",
    subtitle: "Meet the people guiding your school.",
    create: A,
    columns: cols("name", "email", "employeeNumber", "qualification"),
    fields: [
      ref("userId", "Teacher account", "/users", {
        optionLabel: (u) => `${u.firstName} ${u.lastName} · ${u.email}`,
      }),
      f("employeeNumber", "Employee number"),
      f("qualification", "Qualification", "text", { required: false }),
    ],
  },
  staff: {
    title: "School staff",
    subtitle: "The team behind a well-run school.",
    create: A,
    columns: cols("name", "employeeNumber", "jobTitle"),
    fields: [
      ref("userId", "Staff account", "/users", {
        optionLabel: (u) => `${u.firstName} ${u.lastName} · ${u.email}`,
      }),
      f("employeeNumber", "Employee number"),
      f("jobTitle", "Job title"),
    ],
  },
  "academic-years": {
    title: "Academic years",
    subtitle: "Organize the school year.",
    create: A,
    columns: cols("name", "startDate", "endDate"),
    fields: [f("name", "Year name"), ...dates],
  },
  terms: {
    title: "Terms & semesters",
    subtitle: "A clear structure for academic progress.",
    create: A,
    columns: cols("name", "startDate", "endDate", "academicYearId"),
    fields: [yearRef(), f("name", "Term name"), ...dates],
  },
  classes: {
    title: "Classes",
    subtitle: "A place for every learner.",
    create: A,
    columns: cols(
      "name",
      "gradeLevel",
      "academicYear",
      "studentCount",
      "capacity",
    ),
    fields: [
      yearRef(),
      f("name", "Class name"),
      f("gradeLevel", "Grade level"),
      f("capacity", "Capacity", "number", { min: 1, max: 1000 }),
    ],
  },
  subjects: {
    title: "Subjects",
    subtitle: "Build a balanced curriculum.",
    create: A,
    columns: cols("code", "name"),
    fields: [f("code", "Subject code"), f("name", "Subject name")],
  },
  offerings: {
    title: "Teaching assignments",
    subtitle: "Connect subjects, classes, and teachers.",
    create: A,
    columns: cols("className", "subject", "teacher"),
    fields: [
      classRef(),
      ref("subjectId", "Subject", "/subjects"),
      ref("teacherId", "Teacher", "/teachers"),
    ],
  },
  enrollments: {
    title: "Enrollment",
    subtitle: "Manage student placement and class history.",
    create: A,
    columns: cols("student", "className", "enrolledOn", "withdrawnOn"),
    fields: [studentRef(), classRef()],
  },
  schedules: {
    title: "Class schedule",
    subtitle: "A clear view of the week ahead.",
    create: A,
    columns: cols(
      "className",
      "subject",
      "weekday",
      "startTime",
      "endTime",
      "room",
    ),
    fields: [
      offeringRef(),
      opt(
        "weekday",
        "Day",
        [
          { id: 1, name: "Monday" },
          { id: 2, name: "Tuesday" },
          { id: 3, name: "Wednesday" },
          { id: 4, name: "Thursday" },
          { id: 5, name: "Friday" },
          { id: 6, name: "Saturday" },
          { id: 7, name: "Sunday" },
        ],
        { numeric: true },
      ),
      f("startTime", "Start time", "time"),
      f("endTime", "End time", "time"),
      f("room", "Room"),
    ],
  },
  assignments: {
    title: "Assignments",
    subtitle: "Small steps toward big understanding.",
    create: T,
    columns: cols("title", "className", "subject", "dueAt", "submissionCount"),
    fields: [
      offeringRef(),
      f("title", "Title"),
      f("description", "Instructions", "textarea"),
      f("dueAt", "Due date & time", "datetime-local"),
    ],
  },
  grades: {
    title: "Grades & progress",
    subtitle: "Celebrate progress. Find the next opportunity.",
    create: T,
    columns: cols(
      "student",
      "subject",
      "term",
      "type",
      "title",
      "score",
      "maximum",
      "letter",
    ),
    fields: [
      studentRef(),
      offeringRef(),
      ref("termId", "Term", "/terms"),
      opt("type", "Assessment type", [
        "ASSIGNMENT",
        "QUIZ",
        "EXAM",
        "MIDTERM",
        "FINAL",
      ]),
      f("title", "Assessment title"),
      f("score", "Score", "number", { min: 0, step: "0.01" }),
      f("maximum", "Maximum score", "number", {
        min: "0.01",
        step: "0.01",
        default: 100,
      }),
      f("weight", "Weight", "number", {
        min: "0.01",
        step: "0.01",
        default: 1,
      }),
    ],
  },
  attendance: {
    title: "Attendance",
    subtitle: "Every school day matters.",
    create: T,
    columns: cols("student", "className", "date", "status"),
    fields: [
      studentRef(),
      classRef(),
      f("date", "Date", "date"),
      opt("status", "Status", ["PRESENT", "ABSENT", "LATE", "EXCUSED"]),
    ],
  },
  "fee-structures": {
    title: "Fee structures",
    subtitle: "Clear, consistent school fees.",
    create: B,
    columns: cols("name", "amount", "currency", "academicYearId", "classId"),
    fields: [
      yearRef(),
      { ...classRef(), required: false },
      f("name", "Fee name"),
      amount,
      f("currency", "Currency", "text", { default: "USD", maxLength: 3 }),
    ],
  },
  invoices: {
    title: "Invoices & balances",
    subtitle: "Everything you need to stay up to date.",
    create: B,
    columns: cols(
      "student",
      "description",
      "amount",
      "paid",
      "balance",
      "currency",
      "dueDate",
      "status",
    ),
    fields: [
      studentRef(),
      ref("feeStructureId", "Fee structure", "/fee-structures", {
        optionLabel: (f) => `${f.name} · ${f.currency} ${f.amount}`,
      }),
      f("dueDate", "Due date", "date"),
    ],
  },
  payments: {
    title: "Payment history",
    subtitle: "Payments and receipts, all in one place.",
    columns: cols("student", "amount", "currency", "status", "paidAt"),
  },
  announcements: {
    title: "Announcements",
    subtitle: "Stay connected to what’s happening.",
    create: T,
    columns: cols("title", "className", "priority", "author", "publishedAt"),
    fields: [
      { ...classRef(), required: false },
      f("title", "Title"),
      f("body", "Announcement", "textarea"),
      opt("priority", "Priority", ["NORMAL", "IMPORTANT", "EMERGENCY"], {
        default: "NORMAL",
      }),
    ],
  },
  notifications: {
    title: "Notifications",
    subtitle: "Your school updates, in one place.",
    columns: cols("title", "body", "createdAt", "readAt"),
  },
  messages: {
    title: "Messages",
    subtitle: "Good communication makes a difference.",
    create: ["ADMINISTRATOR", "TEACHER", "PARENT"],
    columns: cols("sender", "recipient", "body", "sentAt", "readAt"),
    fields: [
      ref("recipientId", "To", "/contacts"),
      f("body", "Message", "textarea", { maxLength: 5000 }),
    ],
  },
  events: {
    title: "School calendar",
    subtitle: "Make room for what’s coming next.",
    create: A,
    columns: cols("title", "category", "startAt", "endAt", "allDay"),
    fields: [
      f("title", "Event title"),
      f("description", "Description", "textarea", {
        required: false,
        maxLength: 5000,
      }),
      opt("category", "Category", [
        "HOLIDAY",
        "SCHOOL_DAY",
        "CONFERENCE",
        "EXAM",
        "SPORT",
        "TRIP",
        "MEETING",
        "GRADUATION",
        "DEADLINE",
      ]),
      f("startAt", "Starts", "datetime-local"),
      f("endAt", "Ends", "datetime-local"),
      f("allDay", "All-day event", "checkbox", { required: false }),
    ],
  },
  users: {
    title: "User accounts",
    subtitle: "Manage school access with care.",
    create: A,
    columns: cols(
      "firstName",
      "lastName",
      "email",
      "roles",
      "active",
      "lastLoginAt",
    ),
    fields: [
      f("firstName", "First name"),
      f("lastName", "Last name"),
      f("email", "Email", "email"),
      f("password", "Initial password", "password", {
        minLength: 12,
        maxLength: 72,
      }),
      opt("role", "Role", [
        "ADMINISTRATOR",
        "TEACHER",
        "PARENT",
        "STUDENT",
        "ACCOUNTANT",
      ]),
    ],
  },
};
export const navigation = [
  {
    label: "Overview",
    items: [
      ["", "Dashboard", "LayoutDashboard"],
      ["notifications", "Notifications", "Bell"],
    ],
  },
  {
    label: "School community",
    items: [
      ["students", "Students", "GraduationCap"],
      ["parents", "Parents", "Users", P],
      ["teachers", "Teachers", "Contact"],
      ["staff", "School staff", "BriefcaseBusiness", A],
    ],
  },
  {
    label: "Learning",
    items: [
      ["classes", "Classes", "School"],
      ["subjects", "Subjects", "LibraryBig"],
      ["offerings", "Teaching assignments", "BookOpen", T],
      ["enrollments", "Enrollment", "UserPlus", A],
      ["schedules", "Class schedule", "Clock"],
      [
        "assignments",
        "Assignments",
        "NotebookPen",
        ["ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT"],
      ],
      [
        "grades",
        "Grades",
        "ChartNoAxesCombined",
        ["ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT"],
      ],
      [
        "attendance",
        "Attendance",
        "ClipboardCheck",
        ["ADMINISTRATOR", "TEACHER", "PARENT", "STUDENT"],
      ],
    ],
  },
  {
    label: "School life",
    items: [
      ["announcements", "Announcements", "Megaphone"],
      [
        "messages",
        "Messages",
        "MessagesSquare",
        ["ADMINISTRATOR", "TEACHER", "PARENT"],
      ],
      ["events", "Calendar", "CalendarDays"],
    ],
  },
  {
    label: "Finance",
    items: [
      ["fee-structures", "Fee structures", "ReceiptText", B],
      [
        "invoices",
        "Fees & invoices",
        "Wallet",
        ["ADMINISTRATOR", "ACCOUNTANT", "PARENT", "STUDENT"],
      ],
      [
        "payments",
        "Payments",
        "CreditCard",
        ["ADMINISTRATOR", "ACCOUNTANT", "PARENT", "STUDENT"],
      ],
    ],
  },
  {
    label: "Administration",
    items: [
      ["academic-years", "Academic years", "CalendarRange", A],
      ["terms", "Terms", "ListTree", A],
      ["reports", "Reports", "FileChartColumn", B],
      ["users", "User accounts", "ShieldCheck", A],
    ],
  },
];
