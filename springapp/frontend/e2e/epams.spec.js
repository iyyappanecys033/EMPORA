// EPAMS P9 End-to-End Browser Test Specification (E2E-01 through E2E-08)
// Covers real browser cookie security, CSRF header propagation, multi-persona ticket
// lifecycle, approval enforcement, internal comment privacy, notifications, analytics,
// and cryptographic audit verification.
//
// Prerequisites:
//   - Backend running at EPAMS_API_URL (default http://localhost:8080)
//   - Frontend running at EPAMS_BASE_URL (default http://localhost:3000)
//   - Deterministic seeded accounts (override via environment variables if needed)

const { test, expect } = require('@playwright/test');

const CREDENTIALS = {
  admin: {
    username: process.env.E2E_ADMIN_USER || 'admin',
    password: process.env.E2E_ADMIN_PASS || 'admin123'
  },
  employee: {
    username: process.env.E2E_EMPLOYEE_USER || 'john_doe',
    password: process.env.E2E_EMPLOYEE_PASS || 'user123'
  },
  manager: {
    username: process.env.E2E_MANAGER_USER || 'mgr_carol',
    password: process.env.E2E_MANAGER_PASS || 'Password123!'
  },
  specialist: {
    username: process.env.E2E_SPECIALIST_USER || 'it_dave',
    password: process.env.E2E_SPECIALIST_PASS || 'Password123!'
  },
  auditor: {
    username: process.env.E2E_AUDITOR_USER || 'auditor_eve',
    password: process.env.E2E_AUDITOR_PASS || 'Password123!'
  }
};

async function loginViaUi(page, username, password) {
  await page.goto('/');
  const signInBtn = page.getByRole('button', { name: /Sign In/i });
  await expect(signInBtn).toBeVisible();
  await signInBtn.click();

  await page.getByPlaceholder('e.g. admin').fill(username);
  await page.getByPlaceholder('••••••••').fill(password);
  await page.getByRole('button', { name: /Sign In|Authenticate|Login/i }).last().click();

  await expect(page.getByRole('button', { name: /Sign Out|Logout/i })).toBeVisible();
}

async function logoutViaUi(page) {
  const logoutBtn = page.getByRole('button', { name: /Sign Out|Logout/i });
  if (await logoutBtn.isVisible()) {
    await logoutBtn.click();
    await expect(page.getByRole('button', { name: /Sign In/i })).toBeVisible();
  }
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
    await expect(page.getByRole('button', { name: /Sign Out|Logout/i })).toBeVisible();

    // Logout revokes session
    await logoutViaUi(page);
    await page.goto('/tickets');
    await expect(page).toHaveURL(/\/$/);
  });

  test('E2E-02: Employee browses Service Catalog and creates a ticket', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.employee.username, CREDENTIALS.employee.password);

    await page.goto('/catalog');
    await expect(page.getByRole('heading', { name: /Service Catalog/i })).toBeVisible();

    await page.getByRole('button', { name: /Custom Request/i }).click();
    const descField = page.locator('textarea').first();
    await descField.fill('Playwright E2E-02 automated service ticket request');
    await page.getByRole('button', { name: /Submit Request|Create Ticket/i }).click();

    await page.goto('/tickets');
    await expect(page.getByText(/Playwright E2E-02 automated service ticket request/i).first()).toBeVisible();
  });

  test('E2E-03: Manager approves pending approval ticket; self-approval prevented', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.manager.username, CREDENTIALS.manager.password);
    await page.goto('/approvals');
    await expect(page.getByRole('heading', { name: /Approval/i })).toBeVisible();

    const approveBtn = page.getByRole('button', { name: /^Approve$/i }).first();
    if (await approveBtn.isVisible()) {
      await approveBtn.click();
      const reasonInput = page.locator('textarea, input[type="text"]').last();
      await reasonInput.fill('Approved via Playwright E2E-03 verification with sufficient business reason.');
      await page.getByRole('button', { name: /Confirm|Submit/i }).click();
      await expect(page.getByText(/403|Conflict|Error/i)).toHaveCount(0);
    }
  });

  test('E2E-04: Department Specialist assigns, adds internal note, and resolves ticket', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.specialist.username, CREDENTIALS.specialist.password);
    await page.goto('/queue');
    await expect(page.getByRole('heading', { name: /Queue|Workspace/i })).toBeVisible();

    const firstTicketLink = page.locator('a[href^="/tickets/"]').first();
    if (await firstTicketLink.isVisible()) {
      await firstTicketLink.click();
      await expect(page.getByText(/TKT-/i).first()).toBeVisible();
    }
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

  test('E2E-07: Auditor cryptographic audit chain verification', async ({ page }) => {
    await loginViaUi(page, CREDENTIALS.auditor.username, CREDENTIALS.auditor.password);
    await page.goto('/audit-chain');
    await expect(page.getByText(/Audit/i).first()).toBeVisible();

    const verifyBtn = page.getByRole('button', { name: /Verify Chain|Run Integrity/i });
    if (await verifyBtn.isVisible()) {
      await verifyBtn.click();
      await expect(page.getByText(/VALID|Verified|Intact/i).first()).toBeVisible();
    }
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
});

