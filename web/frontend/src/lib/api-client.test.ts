import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  ApiError,
  ValidationError,
  fetchJson,
  installAuthRefreshInterceptor,
  resetAuthRefreshInterceptor,
} from './api-client';

// Minimal Response factory for use in fetch mocks.
function makeResponse(
  body: unknown,
  options: { status?: number; contentType?: string } = {},
): Response {
  const { status = 200, contentType = 'application/json' } = options;
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': contentType },
  });
}

describe('fetchJson', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(makeResponse({ ok: true }))),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('exports a fetchJson function', () => {
    expect(typeof fetchJson).toBe('function');
  });

  it('returns parsed JSON on a 2xx response', async () => {
    const data = await fetchJson('/api/test');
    expect(data).toEqual({ ok: true });
  });

  it('throws an error on a 4xx response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(makeResponse({ message: 'Not found' }, { status: 404 }))),
    );
    await expect(fetchJson('/api/missing')).rejects.toThrow();
  });

  it('throws an error on a 5xx response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(makeResponse({ message: 'Server error' }, { status: 500 }))),
    );
    await expect(fetchJson('/api/broken')).rejects.toThrow();
  });

  it('sets the Content-Type header to application/json by default', async () => {
    const mockFetch = vi.fn(() => Promise.resolve(makeResponse({ ok: true })));
    vi.stubGlobal('fetch', mockFetch);

    await fetchJson('/api/test', { method: 'POST', body: { name: 'test' } });

    const callArgs = mockFetch.mock.calls[0] as unknown as [string, RequestInit];
    const headers = callArgs[1]?.headers as Record<string, string>;
    expect(headers['Content-Type']).toBe('application/json');
  });
});

describe('fetchJson (credentials + error model)', () => {
  const originalFetch = globalThis.fetch;
  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });
  afterEach(() => {
    globalThis.fetch = originalFetch;
  });

  it('sends credentials: include on every request', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify({ ok: true }), { status: 200 }),
    );
    await fetchJson('/api/v1/x');
    const init = (globalThis.fetch as ReturnType<typeof vi.fn>).mock.calls[0][1] as RequestInit;
    expect(init.credentials).toBe('include');
  });

  it('throws ApiError for 401', async () => {
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify({ detail: 'Invalid credentials.' }), { status: 401 }),
    );
    await expect(fetchJson('/api/v1/x')).rejects.toBeInstanceOf(ApiError);
  });

  it('throws ValidationError with field map for RFC 7807 422', async () => {
    const body = {
      type: 'about:blank',
      title: 'Validation failed',
      status: 422,
      detail: 'The submitted data did not pass validation.',
      instance: '/api/v1/auth/register',
      errors: [
        { field: 'email', code: 'value_error', message: 'value is not a valid email address' },
        {
          field: 'password',
          code: 'value_error',
          message: 'Password must be at least 12 characters.',
        },
      ],
    };
    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify(body), { status: 422 }),
    );
    try {
      await fetchJson('/api/v1/x');
      throw new Error('should not reach');
    } catch (e) {
      expect(e).toBeInstanceOf(ValidationError);
      expect((e as ValidationError).fieldErrors).toEqual({
        email: 'value is not a valid email address',
        password: 'Password must be at least 12 characters.',
      });
    }
  });
});

describe('auth refresh interceptor', () => {
  const originalFetch = globalThis.fetch;
  beforeEach(() => {
    globalThis.fetch = vi.fn();
  });
  afterEach(() => {
    globalThis.fetch = originalFetch;
    resetAuthRefreshInterceptor();
  });

  it('refreshes once on 401 and retries the original request', async () => {
    const onRefreshSucceeded = vi.fn();
    const onLogout = vi.fn();
    installAuthRefreshInterceptor({
      refresh: async () => {
        onRefreshSucceeded();
      },
      onLogout,
    });

    (globalThis.fetch as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ detail: 'Not authenticated.' }), { status: 401 }),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify({ ok: true }), { status: 200 }));

    const result = await fetchJson<{ ok: boolean }>('/api/v1/weight-entries');
    expect(result).toEqual({ ok: true });
    expect(onRefreshSucceeded).toHaveBeenCalledTimes(1);
    expect(onLogout).not.toHaveBeenCalled();
  });

  it('calls onLogout and throws if refresh also returns 401', async () => {
    const onLogout = vi.fn();
    installAuthRefreshInterceptor({
      refresh: async () => {
        throw new Error('refresh failed');
      },
      onLogout,
    });

    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify({ detail: 'Not authenticated.' }), { status: 401 }),
    );

    await expect(fetchJson('/api/v1/weight-entries')).rejects.toThrow();
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it('calls onLogout and throws when refresh succeeds but the retry itself returns non-2xx', async () => {
    const onLogout = vi.fn();
    installAuthRefreshInterceptor({
      refresh: vi.fn().mockResolvedValue(undefined),
      onLogout,
    });

    (globalThis.fetch as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ detail: 'Not authenticated.' }), { status: 401 }),
      )
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ detail: 'Not authenticated.' }), { status: 401 }),
      );

    await expect(fetchJson('/api/v1/weight-entries')).rejects.toBeInstanceOf(ApiError);
    expect(onLogout).toHaveBeenCalledTimes(1);
  });

  it('throws ValidationError when refresh succeeds but retry returns 422', async () => {
    const onLogout = vi.fn();
    installAuthRefreshInterceptor({
      refresh: vi.fn().mockResolvedValue(undefined),
      onLogout,
    });

    const body422 = {
      type: 'about:blank',
      title: 'Validation failed',
      status: 422,
      detail: 'Post-refresh validation error.',
      instance: '/api/v1/weight-entries',
      errors: [{ field: 'weight', code: 'value_error', message: 'Must be positive.' }],
    };
    (globalThis.fetch as ReturnType<typeof vi.fn>)
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ detail: 'Not authenticated.' }), { status: 401 }),
      )
      .mockResolvedValueOnce(new Response(JSON.stringify(body422), { status: 422 }));

    await expect(fetchJson('/api/v1/weight-entries')).rejects.toBeInstanceOf(ValidationError);
    expect(onLogout).not.toHaveBeenCalled();
  });

  it('does not attempt refresh when the failing URL is /api/v1/auth/refresh itself', async () => {
    const refresh = vi.fn();
    const onLogout = vi.fn();
    installAuthRefreshInterceptor({ refresh, onLogout });

    (globalThis.fetch as ReturnType<typeof vi.fn>).mockResolvedValueOnce(
      new Response(JSON.stringify({ detail: 'Not authenticated.' }), { status: 401 }),
    );

    await expect(fetchJson('/api/v1/auth/refresh', { method: 'POST' })).rejects.toThrow();
    expect(refresh).not.toHaveBeenCalled();
    expect(onLogout).toHaveBeenCalledTimes(1);
  });
});
