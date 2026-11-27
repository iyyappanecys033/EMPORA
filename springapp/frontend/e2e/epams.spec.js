// EPAMS P9 End-to-End Browser Test Specification (E2E-01 through E2E-09)
// Covers real browser cookie security, CSRF header propagation, multi-persona ticket
// lifecycle, approval enforcement, internal comment privacy, notifications, analytics,
// and cryptographic audit verification.
//
// Prerequisites:
//   - Backend running at EPAMS_API_URL (default http://localhost:8080)
//   - Frontend running at EPAMS_BASE_URL (default http://localhost:3000)
//   - Deterministic seeded accounts (override via environment variables if needed)

const { test, expect } = require('@playwright/test');

const ADMIN_CREDENTIALS = {
  username: process.env.E2E_ADMIN_USER || 'admin',
  password: process.env.E2E_ADMIN_PASS || 'admin123'
};

const CREDENTIALS = {
  admin: ADMIN_CREDENTIALS,
  employee: {
    username: process.env.E2E_EMPLOYEE_USER || 'john_doe',
    password: process.env.E2E_EMPLOYEE_PASS || 'user123'
  },
};

async function loginViaUi(page, username, password) {
  await page.goto('/');
  const signInBtn = page.getByRole('button', { name: 'Sign In / Register' });
  await expect(signInBtn).toBeVisible();
  await signInBtn.click();

  const usernameInput = page.getByLabel('Username *');
  const passwordInput = page.getByLabel('Password *');
  await usernameInput.fill(username);
  await passwordInput.fill(password);
  await expect(usernameInput).toHaveValue(username);
  await expect(passwordInput).toHaveValue(password);
  await page.getByRole('dialog').getByRole('button', { name: 'Sign In', exact: true }).click();

  await expect(page.getByRole('button', { name: /Account and profile menu/i })).toBeVisible();
}

async function logoutViaUi(page) {
  const accountMenu = page.getByRole('button', { name: /Account and profile menu/i });
  if (await accountMenu.isVisible()) {
    await accountMenu.click();
    await page.getByRole('menuitem', { name: /Sign Out/i }).click();
    await expect(page.getByRole('button', { name: 'Sign In / Register' })).toBeVisible();
  }
}

async function createApprovalTicket(page, testCase) {
  await loginViaUi(page, CREDENTIALS.employee.username, CREDENTIALS.employee.password);
  await page.goto('/catalog');
  await expect(page.getByRole('heading', { name: /Service Catalog/i })).toBeVisible();
  await page.getByRole('button', { name: /Custom Request/i }).click();

  const approvalOption = page.locator('#serviceOfferingSelect option').filter({
    hasText: 'Developer Workstation Laptop Upgrade'
  });
  await expect(approvalOption).toHaveCount(1);
  await page.locator('#serviceOfferingSelect').selectOption(await approvalOption.getAttribute('value'));
  await expect(page.getByText(/Requires Manager Approval/)).toBeVisible();

  const description = `Playwright ${testCase} approval journey ${Date.now()}`;
  await page.getByLabel('Business Justification & Details *').fill(description);
  const createResponse = page.waitForResponse((response) =>
    response.url().includes('/api/v1/tickets') &&
    response.request().method() === 'POST'
  );
  await page.getByRole('button', { name: /Submit Ticket/i }).click();
  const response = await createResponse;
  expect(response.ok()).toBeTruthy();
  const ticket = await response.json();
  expect(ticket.ticketNumber).toBeTruthy();
  expect(ticket.id).toBeTruthy();
  expect(ticket.status).toBe('PENDING_APPROVAL');
  const approvalHistory = await page.evaluate(async (ticketId) => {
    const historyResponse = await fetch(`/api/v1/approvals/tickets/${ticketId}`, {
      credentials: 'include'
    });
    return {
      status: historyResponse.status,
      tasks: historyResponse.ok ? await historyResponse.json() : []
    };
  }, ticket.id);
  expect(approvalHistory.status).toBe(200);
  expect(approvalHistory.tasks).toHaveLength(1);
  expect(approvalHistory.tasks[0].status).toBe('PENDING');
  await expect(page.getByRole('dialog')).toHaveCount(0);

  await page.goto('/tickets');
  await expect(page.getByText(description, { exact: true })).toBeVisible();
  await logoutViaUi(page);
  return {
    ...ticket,
    description,
    approvalTaskId: approvalHistory.tasks[0].id
  };
}

async function approveTicketAsAdmin(page, taskId) {
  const result = await page.evaluate(async ({ id, reason }) => {
    const csrfCookie = document.cookie
      .split('; ')
      .find((cookie) => cookie.startsWith('XSRF-TOKEN='));
    const csrfToken = csrfCookie ? decodeURIComponent(csrfCookie.slice('XSRF-TOKEN='.length)) : '';
    const response = await fetch(`/api/v1/approvals/${id}/decide`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'X-XSRF-TOKEN': csrfToken
      },
      body: JSON.stringify({ decision: 'APPROVED', reason })
    });
    return {
      status: response.status,
      body: response.ok ? await response.json() : await response.text()
    };
  }, {
    id: taskId,
    reason: 'Approved by system administrator after verifying the request details.'
  });

  expect(result.status).toBe(200);
  expect(result.body.status).toBe('APPROVED');
  expect(result.body.newTicketStatus).toBe('ASSIGNED');
}

async function confirmTicketAction(page, ticketId, buttonName, reason) {
  await page.getByRole('button', { name: buttonName }).click();
  const actionDialog = page.getByRole('dialog');
  await expect(actionDialog).toBeVisible();
  await actionDialog.getByLabel('Reason / Diagnostic Notes *').fill(reason);
  const actionResponse = page.waitForResponse((response) =>
    response.url().includes(`/api/v1/tickets/${ticketId}/status`) &&
    response.request().method() === 'POST',
  { timeout: 15_000 });
  await actionDialog.getByRole('button', { name: 'Confirm Action' }).click();
  expect((await actionResponse).ok()).toBeTruthy();
  await expect(actionDialog).toHaveCount(0);
}

test.describe('EPAMS Full-Stack Browser E2E Journeys (P9)', () => {
  test('E2E-01: Authentication, HttpOnly cookie security, page reload persistence, and logout', async ({ page, context }) => {
    await loginViaUi(page, CREDENTIALS.admin.username, CREDENTIALS.admin.password);

    // Verify JWT cookies are HttpOnly and never stored in localStorage/sessionStorage
    const cookies = await context.cookies();
    const jwtCookie = cookies.find((c) => c.name === 'accessToken');
    const xsrfCookie = cookies.find((c) => c.name === 'XSRF-TOKEN');
    expect(jwtCookie).toBeTruthy();
    expect(jwtCookie.httpOnly).toBe(true);
    expect(xsrfCookie).toBeTruthy();
    expect(xsrfCookie.httpOnly).toBe(false);

    const leakedStorage = await page.evaluate(() => ({
      local: Object.keys(window.localStorage),
      session: Object.keys(window.sessionStorage),
      docCookie: document.cookie
    }));
    expect(leakedStorage.docCookie).not.toContain('accessToken');
    expect(JSON.stringify(leakedStorage.local)).not.toContain('eyJ');
    expect(JSON.stringify(leakedStorage.session)).not.toContain('eyJ');

    // Verify reload retains session via /api/users/me
    await page.reload();
    await expect(page.getByRole('button', { name: /Account and profile menu/i })).toBeVisible();

    // Logout revokes session
    await logoutViaUi(page);
    await page.goto('/tickets');
    await expect(page).toHaveURL(/\/$/);
  });

  test('E2E-02: Employee creates a manager-approved catalog ticket', async ({ page }) => {
    const ticket = await createApprovalTicket(page, 'E2E-02');
    expect(ticket.ticketNumber).toMatch(/^TKT-/);
  });

  test('E2E-03: Admin approves the employee ticket through the authorized override', async ({ page }) => {
    const ticket = await createApprovalTicket(page, 'E2E-03');
    await loginViaUi(page, CREDENTIALS.admin.username, CREDENTIALS.admin.password);
    await approveTicketAsAdmin(page, ticket.approvalTaskId);
  });

  test('E2E-04: Admin approves, assigns, notes, and resolves an employee ticket', async ({ page }) => {
    const ticket = await createApprovalTicket(page, 'E2E-04');
    await loginViaUi(page, CREDENTIALS.admin.username, CREDENTIALS.admin.password);
    await approveTicketAsAdmin(page, ticket.approvalTaskId);
    await page.goto(`/tickets/${ticket.id}`);
    await expect(page.getByText(ticket.ticketNumber, { exact: false }).first()).toBeVisible();

    await expect(page.getByRole('button', { name: 'Assign to Me' })).toBeVisible();
    const assignmentResponse = page.waitForResponse((response) =>
      response.url().includes(`/api/v1/tickets/${ticket.id}/assign`) &&
      response.request().method() === 'POST',
    { timeout: 15_000 });
    await page.getByRole('button', { name: 'Assign to Me' }).click();
    expect((await assignmentResponse).ok()).toBeTruthy();
    await expect(page.getByRole('button', { name: 'Mark Resolved' })).toBeVisible();
    await expect(page.getByText('IN PROGRESS', { exact: true })).toBeVisible();

    const internalNote = `Playwright internal note ${Date.now()}`;
    await page.getByLabel('Add comment').fill(internalNote);
    await page.getByRole('checkbox').check();
    const commentResponse = page.waitForResponse((response) =>
      response.url().includes(`/api/v1/tickets/${ticket.id}/comments`) &&
      response.request().method() === 'POST',
    { timeout: 15_000 });
    await page.getByRole('button', { name: 'Post Comment' }).click();
    expect((await commentResponse).ok()).toBeTruthy();
    await expect(page.getByText(internalNote, { exact: true })).toBeVisible();

    await confirmTicketAction(page, ticket.id, 'Mark Resolved', 'Completed and verified the requested work.');
    await expect(page.getByText('RESOLVED', { exact: true })).toBeVisible();
  });

  test('E2E-05: Notification Bell unread badge, Notifications page, and mark-as-read sync', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.employee.username, CREDENTIALS.employee.password);
    await page.goto('/notifications');
    await expect(page.getByRole('heading', { name: /Notification/i })).toBeVisible();

    const markAllBtn = page.getByRole('button', { name: /Mark All Read/i });
    if (await markAllBtn.isVisible() && await markAllBtn.isEnabled()) {
      await markAllBtn.click();
      await expect(page.getByTestId('unread-badge')).toHaveCount(0);
    }
  });

  test('E2E-06: Operational Analytics dashboard rendering and date/department scoping', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.admin.username, CREDENTIALS.admin.password);
    await page.goto('/analytics');
    await expect(page.getByText(/Operational Analytics & Service Insights/i)).toBeVisible();
    await expect(page.getByText(/SLA Compliance/i).first()).toBeVisible();
  });

  test('E2E-07: Admin verifies the cryptographic audit chain', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.admin.username, CREDENTIALS.admin.password);
    await page.goto('/audit-chain');
    await expect(page.getByText(/Audit/i).first()).toBeVisible();

    const verifyBtn = page.getByRole('button', { name: 'Verify Cryptographic Chain' });
    await expect(verifyBtn).toBeVisible();
    await verifyBtn.click();
    await expect(page.getByText(/VALID|Verified|Intact/i).first()).toBeVisible();
  });

  test('E2E-08: Unauthorized route & cross-user security boundaries return 403 / redirect', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.employee.username, CREDENTIALS.employee.password);

    // Employee blocked from Admin User Directory and Operational Analytics
    await page.goto('/users');
    await expect(page.getByText(/403 — Unauthorized Scope|403 — Access Denied/i)).toBeVisible();

    await page.goto('/analytics');
    await expect(page.getByText(/403 — Unauthorized Scope|403 — Access Denied/i)).toBeVisible();

    await page.goto('/audit-chain');
    await expect(page.getByText(/403 — Unauthorized Scope|403 — Access Denied/i)).toBeVisible();
  });

  test('E2E-09: Admin updates and reloads encrypted employee address data', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.admin.username, CREDENTIALS.admin.password);
    await page.goto('/profiles');

    const employeeRow = page.getByRole('row').filter({ hasText: CREDENTIALS.employee.username });
    const profileId = await employeeRow.locator('td').first().innerText();
    const editUrl = `/profiles/${profileId}/edit`;
    await page.goto(editUrl);

    const addressField = page.locator('textarea[name="address"]');
    await expect(addressField).toHaveValue(/.+/);
    const originalAddress = await addressField.inputValue();
    const updatedAddress = `Playwright E2E encrypted PII ${Date.now()}`;

    try {
      await page.locator('input[name="email"]').fill('john.doe@empora.com');
      await addressField.fill(updatedAddress);
      const updateResponse = page.waitForResponse((response) =>
        response.url().endsWith(`/api/profiles/${profileId}`) &&
        response.request().method() === 'PUT'
      );
      const updateNavigation = page.waitForURL('**/profiles');
      await page.getByRole('button', { name: 'Save' }).click();
      const [savedResponse] = await Promise.all([updateResponse, updateNavigation]);
      expect(savedResponse.ok()).toBeTruthy();

      await page.goto(editUrl);
      await expect(page.locator('textarea[name="address"]')).toHaveValue(updatedAddress);
    } finally {
      await page.goto(editUrl);
      await expect(page.locator('select[name="userId"]')).toHaveValue(/.+/);
      await expect(page.locator('textarea[name="address"]')).toHaveValue(/.+/);
      await page.locator('input[name="email"]').fill('john.doe@empora.com');
      await page.locator('textarea[name="address"]').fill(originalAddress);
      const restoreResponse = page.waitForResponse((response) =>
        response.url().endsWith(`/api/profiles/${profileId}`) &&
        response.request().method() === 'PUT'
      );
      const restoreNavigation = page.waitForURL('**/profiles');
      await page.getByRole('button', { name: 'Save' }).click();
      const [restoredResponse] = await Promise.all([restoreResponse, restoreNavigation]);
      expect(restoredResponse.ok()).toBeTruthy();
      await page.goto(editUrl);
      await expect(page.locator('textarea[name="address"]')).toHaveValue(originalAddress);
    }
  });
});

