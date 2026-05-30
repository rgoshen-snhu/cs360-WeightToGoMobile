import { expect, test } from '@playwright/test';

/**
 * E2E: goal history (FR-G-5).
 *
 * A user creates a goal, abandons it, then sees it listed under
 * "Goal history" on the /goals page with an "Abandoned" outcome.
 */
test.describe.serial('goal history', () => {
  const unique = Date.now();
  const email = `goal-history-${unique}@example.com`;
  const password = 'Aa1!aaaaaaaa';

  test('seed: register', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel(/display name/i).fill('History Tester');
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/^password$/i).fill(password);
    await page.getByLabel(/confirm password/i).fill(password);
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });
  });

  test('create then abandon a goal, see it in history', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/password/i).fill(password);
    await page.getByRole('button', { name: /log in/i }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });

    await page.goto('/goals');
    await page.getByLabel(/target weight/i).fill('150');
    await page.getByLabel(/starting weight/i).fill('200');
    const createGoal = page.waitForResponse(
      (r) => r.url().includes('/api/v1/goals') && r.request().method() === 'POST',
    );
    await page.getByRole('button', { name: /set goal/i }).click();
    await createGoal;

    const abandon = page.waitForResponse(
      (r) => r.url().includes('/api/v1/goals/') && r.request().method() === 'DELETE',
    );
    await page.getByRole('button', { name: /abandon goal/i }).click();
    await abandon;

    const history = page.getByRole('list', { name: /goal history/i });
    await expect(history).toBeVisible({ timeout: 10_000 });
    await expect(history.getByText(/abandoned/i)).toBeVisible();
  });
});
