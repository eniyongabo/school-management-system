import { test, expect } from "@playwright/test";
test("administrator signs in, manages records, and signs out", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/login");
  await page
    .getByLabel("Email address")
    .fill(process.env.E2E_ADMIN_EMAIL || "browser-admin@example.test");
  await page
    .getByLabel("Password", { exact: false })
    .fill(process.env.E2E_ADMIN_PASSWORD || "Browser-Test-Password-42");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Hello, School." }),
  ).toBeVisible();
  await page.screenshot({
    animations: "disabled",
    path: "test-results/dashboard-desktop.png",
    fullPage: true,
  });
  await page.getByRole("link", { name: "Academic years", exact: true }).click();
  await page.getByRole("button", { name: "Add record" }).click();
  await page.getByLabel("Year name").fill("Browser year " + Date.now());
  await page.getByLabel("Start date").fill("2026-01-01");
  await page.getByLabel("End date").fill("2027-12-31");
  await page.getByRole("button", { name: "Save record" }).click();
  await expect(page.getByRole("status")).toHaveText("Saved successfully.");
  await expect(page.locator("tbody tr")).toHaveCount(1);
  await page.getByRole("link", { name: "Classes", exact: true }).click();
  await page.getByRole("button", { name: "Add record" }).click();
  await page.getByLabel("Academic year").selectOption({ index: 1 });
  await page.getByLabel("Class name").fill("Grade 5A");
  await page.getByLabel("Grade level").fill("5");
  await page.getByLabel("Capacity").fill("30");
  await page.getByRole("button", { name: "Save record" }).click();
  await expect(
    page.getByRole("cell", { name: "Grade 5A", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("link", { name: "School calendar", exact: true })
    .count();
  await page.getByRole("link", { name: "Calendar", exact: true }).click();
  await page.getByRole("button", { name: "Add record" }).click();
  await page.getByLabel("Event title").fill("Community evening");
  await page.getByLabel("Category").selectOption("CONFERENCE");
  const futureDay = `${new Date().getFullYear() + 1}-01-15`;
  await page.getByLabel("Starts").fill(`${futureDay}T17:00`);
  await page.getByLabel("Ends").fill(`${futureDay}T19:00`);
  await page.getByRole("button", { name: "Save record" }).click();
  await expect(
    page.getByRole("cell", { name: "Community evening" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Edit", exact: true }).click();
  await expect(page.getByLabel("Starts")).toHaveValue(`${futureDay}T17:00`);
  await page.getByRole("button", { name: "Close dialog" }).click();
  await page.getByRole("link", { name: "Dashboard", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Community evening" }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    animations: "disabled",
    path: "test-results/dashboard-mobile.png",
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await page.getByRole("button", { name: "Open navigation" }).click();
  await page.getByRole("button", { name: "Sign out" }).click();
  await expect(
    page.getByRole("heading", { name: "Welcome back." }),
  ).toBeVisible();
  expect(errors).toEqual([]);
});
test("protected routes redirect anonymous visitors", async ({ page }) => {
  await page.goto("/users");
  await expect(page).toHaveURL(/\/login$/);
});

test("parent sees only linked children and completes a mock payment", async ({
  page,
  request,
}) => {
  const password = "Browser-Parent-Password-42";
  const adminResponse = await request.post(
    "http://127.0.0.1:18080/api/auth/login",
    {
      data: {
        email: "browser-admin@example.test",
        password: "Browser-Test-Password-42",
      },
    },
  );
  expect(adminResponse.ok()).toBeTruthy();
  const adminToken = (await adminResponse.json()).accessToken;
  const headers = { Authorization: `Bearer ${adminToken}` };
  async function create(path, data) {
    const response = await request.post(`http://127.0.0.1:18080/api/${path}`, {
      headers,
      data,
    });
    expect(response.status(), await response.text()).toBe(201);
    return response.json();
  }
  const parent = await create("users", {
    email: "browser-parent@example.test",
    password,
    firstName: "Taylor",
    lastName: "Parent",
    role: "PARENT",
  });
  const parentRows = await (
    await request.get("http://127.0.0.1:18080/api/parents", { headers })
  ).json();
  const parentId = parentRows.items.find((p) => p.userId === parent.id).id;
  const student = await create("students", {
    firstName: "Amina",
    lastName: "Learner",
    dateOfBirth: "2015-01-01",
    emergencyContactName: "Taylor",
    emergencyContactPhone: "555-0100",
    parentId,
  });
  await create("students", {
    firstName: "Unrelated",
    lastName: "Student",
    dateOfBirth: "2015-01-01",
    emergencyContactName: "Another parent",
    emergencyContactPhone: "555-0110",
  });
  const year = await create("academic-years", {
    name: "Parent browser year",
    startDate: "2026-01-01",
    endDate: "2027-12-31",
  });
  const fee = await create("fee-structures", {
    academicYearId: year.id,
    name: "Activity fee",
    amount: 125,
    currency: "USD",
  });
  await create("invoices", {
    studentId: student.id,
    feeStructureId: fee.id,
    dueDate: "2026-12-01",
  });
  await page.goto("/login");
  await page.getByLabel("Email address").fill("browser-parent@example.test");
  await page.getByLabel("Password", { exact: false }).fill(password);
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByRole("heading", { name: "Your children", level: 2 }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Amina Learner", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("link", { name: "User accounts", exact: true }),
  ).toHaveCount(0);
  await page.getByRole("link", { name: "Students", exact: true }).click();
  await expect(
    page.getByRole("cell", { name: "Amina", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("cell", { name: "Unrelated", exact: true }),
  ).toHaveCount(0);
  await page
    .getByRole("link", { name: "Fees & invoices", exact: true })
    .first()
    .click();
  await page.getByRole("button", { name: "Mock payment", exact: true }).click();
  await page.getByLabel("Payment amount").fill("125");
  await page.getByRole("button", { name: "Confirm mock payment" }).click();
  await expect(
    page.getByRole("cell", { name: "Paid", exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "Payments", exact: true }).click();
  const receipt = page.waitForEvent("download");
  await page.getByRole("button", { name: "Receipt", exact: true }).click();
  expect((await receipt).suggestedFilename()).toMatch(/^receipt-\d+\.txt$/);
  await page.getByRole("link", { name: "Dashboard", exact: true }).click();
  await page.screenshot({
    animations: "disabled",
    path: "test-results/dashboard-parent.png",
    fullPage: true,
  });
});
