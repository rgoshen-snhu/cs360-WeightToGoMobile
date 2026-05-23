import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthContext';
import { authClient, type AuthUser } from '../features/auth/api/auth-client';

const user: AuthUser = {
  user_id: 1,
  email: 'a@b.co',
  display_name: 'A',
  created_at: '2026-05-23T00:00:00Z',
};

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={qc}>
      <AuthProvider>{children}</AuthProvider>
    </QueryClientProvider>
  );
}

describe('useAuth (guard)', () => {
  it('throws when called outside an AuthProvider', () => {
    const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    expect(() => renderHook(() => useAuth())).toThrow(
      'useAuth must be used inside an AuthProvider',
    );
    errorSpy.mockRestore();
  });
});

describe('AuthContext', () => {
  beforeEach(() => {
    vi.spyOn(authClient, 'me').mockReset();
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exposes isLoading=true until /me resolves', async () => {
    vi.spyOn(authClient, 'me').mockReturnValue(new Promise(() => undefined));
    const { result } = renderHook(() => useAuth(), { wrapper });
    expect(result.current.isLoading).toBe(true);
    expect(result.current.user).toBeNull();
  });

  it('exposes the user after /me resolves', async () => {
    vi.spyOn(authClient, 'me').mockResolvedValueOnce(user);
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.user).toEqual(user);
    expect(result.current.isAuthenticated).toBe(true);
  });

  it('treats 401 from /me as unauthenticated, not an error', async () => {
    vi.spyOn(authClient, 'me').mockRejectedValueOnce(new Error('401'));
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });

  it('setUser updates state synchronously for callers (login/register)', async () => {
    vi.spyOn(authClient, 'me').mockResolvedValueOnce(user);
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    const next = { ...user, user_id: 2 };
    act(() => {
      result.current.setUser(next);
    });
    expect(result.current.user).toEqual(next);
  });

  it('clearAuth removes the cached user', async () => {
    vi.spyOn(authClient, 'me').mockResolvedValueOnce(user);
    const { result } = renderHook(() => useAuth(), { wrapper });
    await waitFor(() => expect(result.current.user).toEqual(user));
    act(() => {
      result.current.clearAuth();
    });
    expect(result.current.user).toBeNull();
    expect(result.current.isAuthenticated).toBe(false);
  });
});
