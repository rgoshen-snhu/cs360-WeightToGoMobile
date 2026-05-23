import { expect, test } from '@playwright/test';

const OUT = '/Users/richardgoshen/workspaces/snhu/WeightToGoMobile/docs/screenshots/phase-7';
const unique = Date.now();
const email = `screenshot-${unique}@example.com`;
const password = 'Aa1!aaaaaaaa';

test.describe.serial('Phase 7 screenshots', () => {
  test('01-register-page', async ({ page }) => {
    await page.goto('/register');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${OUT}/01-register-page.png`, fullPage: true });
  });

  test('02-login-page', async ({ page }) => {
    await page.goto('/login');
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${OUT}/02-login-page.png`, fullPage: true });
  });

  test('03-register-and-dashboard', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel(/display name/i).fill('Screenshot User');
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/^password$/i).fill(password);
    await page.getByLabel(/confirm password/i).fill(password);
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });
    await page.waitForLoadState('networkidle');
    await page.screenshot({ path: `${OUT}/03-dashboard-authenticated.png`, fullPage: true });
  });

  test('04-user-menu-open', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/password/i).fill(password);
    await page.getByRole('button', { name: /log in/i }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /account menu/i }).click();
    await page.waitForTimeout(300);
    await page.screenshot({ path: `${OUT}/04-user-menu-open.png`, fullPage: true });
  });

  test('05-login-error', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/password/i).fill('wrong-password!');
    await page.getByRole('button', { name: /log in/i }).click();
    await expect(page.getByRole('alert')).toBeVisible({ timeout: 5_000 });
    await page.screenshot({ path: `${OUT}/05-login-error.png`, fullPage: true });
  });
});
