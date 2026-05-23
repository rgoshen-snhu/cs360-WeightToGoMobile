import { expect, test } from '@playwright/test';

test.describe.serial('login flow', () => {
  const unique = Date.now();
  const email = `login-${unique}@example.com`;
  const password = 'Aa1!aaaaaaaa';

  test('seed: register the user', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel(/display name/i).fill('Login User');
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/^password$/i).fill(password);
    await page.getByLabel(/confirm password/i).fill(password);
    await page.getByRole('button', { name: /create account/i }).click();
    await expect(page).toHaveURL('/', { timeout: 10_000 });
    await page.getByRole('button', { name: /account menu/i }).click();
    await page.getByRole('menuitem', { name: /log out/i }).click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('returning user can log in and is redirected to ?from=', async ({ page }) => {
    await page.goto('/weight');
    await expect(page).toHaveURL(/\/login\?from=%2Fweight/);
    await page.getByLabel(/email/i).fill(email);
    await page.getByLabel(/password/i).fill(password);
    await page.getByRole('button', { name: /log in/i }).click();
    await expect(page).toHaveURL('/weight', { timeout: 10_000 });
  });
});
