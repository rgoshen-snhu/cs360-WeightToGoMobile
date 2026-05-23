import { expect, test } from '@playwright/test';

test('logout clears the session and protected routes redirect', async ({ page, context }) => {
  const email = `logout-${Date.now()}@example.com`;
  await page.goto('/register');
  await page.getByLabel(/display name/i).fill('Logout User');
  await page.getByLabel(/email/i).fill(email);
  await page.getByLabel(/^password$/i).fill('Aa1!aaaaaaaa');
  await page.getByLabel(/confirm password/i).fill('Aa1!aaaaaaaa');
  await page.getByRole('button', { name: /create account/i }).click();
  await expect(page).toHaveURL('/', { timeout: 10_000 });

  await page.getByRole('button', { name: /account menu/i }).click();
  await page.getByRole('menuitem', { name: /log out/i }).click();
  await expect(page).toHaveURL(/\/login/);

  await page.goto('/weight');
  await expect(page).toHaveURL(/\/login\?from=%2Fweight/);

  const cookies = await context.cookies();
  expect(cookies.find((c) => c.name === 'access_token')).toBeUndefined();
});
