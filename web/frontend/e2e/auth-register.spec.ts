import { expect, test } from '@playwright/test';

test('new user can register and land on the dashboard', async ({ page }) => {
  const unique = Date.now();
  await page.goto('/register');
  await page.getByLabel(/display name/i).fill('Plan User');
  await page.getByLabel(/email/i).fill(`plan-${unique}@example.com`);
  await page.getByLabel(/^password$/i).fill('Aa1!aaaaaaaa');
  await page.getByLabel(/confirm password/i).fill('Aa1!aaaaaaaa');
  await page.getByRole('button', { name: /create account/i }).click();
  await expect(page).toHaveURL('/', { timeout: 10_000 });
  await expect(page.getByRole('button', { name: /account menu/i })).toBeVisible();
});
