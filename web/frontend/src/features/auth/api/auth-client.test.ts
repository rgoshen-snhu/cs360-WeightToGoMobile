import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { authClient } from './auth-client';

const validUser = {
  user_id: 1,
  email: 'jane@example.com',
  display_name: 'Jane Doe',
  created_at: '2026-05-23T00:00:00Z',
};

describe('authClient', () => {
  const originalFetch = globalThis.fetch;
  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });
  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('register POSTs to /api/v1/auth/register with body and returns user', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify(validUser), { status: 201 }),
    );
    const user = await authClient.register({
      email: 'jane@example.com',
      password: 'Aa1!aaaaaaaa',
      displayName: 'Jane Doe',
    });
    expect(user).toEqual(validUser);
    const [url, init] = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(url).toBe('/api/v1/auth/register');
    expect((init as RequestInit).method).toBe('POST');
    expect(JSON.parse((init as RequestInit).body as string)).toEqual({
      email: 'jane@example.com',
      password: 'Aa1!aaaaaaaa',
      display_name: 'Jane Doe',
    });
  });

  it('login POSTs to /api/v1/auth/login', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify(validUser), { status: 200 }),
    );
    await authClient.login({ email: 'jane@example.com', password: 'Aa1!aaaaaaaa' });
    expect((globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][0]).toBe(
      '/api/v1/auth/login',
    );
  });

  it('logout POSTs to /api/v1/auth/logout and resolves with undefined on 204', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(null, { status: 204 }),
    );
    await expect(authClient.logout()).resolves.toBeUndefined();
  });

  it('refresh POSTs to /api/v1/auth/refresh and returns user', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify(validUser), { status: 200 }),
    );
    await expect(authClient.refresh()).resolves.toEqual(validUser);
  });

  it('me GETs /api/v1/auth/me', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify(validUser), { status: 200 }),
    );
    await expect(authClient.me()).resolves.toEqual(validUser);
  });
});
