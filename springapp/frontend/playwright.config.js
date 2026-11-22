// EPAMS P9 Reproducible Browser E2E Suite Configuration (Playwright)
// NOTE: Playwright is not installed in the offline P9 sandbox environment.
// To execute in a networked CI / P10 staging environment:
//   1. Start the Spring Boot backend on http://localhost:8080 (with H2 or live MySQL 8.0+)
//   2. Start the React frontend on http://localhost:3000 (or serve built bundle from :8080)
//   3. Install Playwright once: npm install -D @playwright/test && npx playwright install chromium
//   4. Execute: npx playwright test

const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './e2e',
  timeout: 45_000,
  expect: {
    timeout: 10_000
  },
  fullyParallel: false, // Sequential execution preserves deterministic cross-persona workflows
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: 1,
  reporter: [['list'], ['html', { outputFolder: 'playwright-report', open: 'never' }]],
  use: {
    baseURL: process.env.EPAMS_BASE_URL || 'http://localhost:3000',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
});

