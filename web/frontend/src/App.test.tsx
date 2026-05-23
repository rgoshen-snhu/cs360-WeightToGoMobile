import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@mui/material';

import { App } from './App';
import { AuthProvider } from './contexts/AuthContext';
import { PreferencesProvider } from './contexts/PreferencesContext';
import { theme } from './theme/theme';
import { authClient } from './features/auth/api/auth-client';

/** Full provider wrapper matching the structure in main.tsx. */
function FullProviders({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <PreferencesProvider>
          <ThemeProvider theme={theme}>{children}</ThemeProvider>
        </PreferencesProvider>
      </AuthProvider>
    </QueryClientProvider>
  );
}

describe('App (integration)', () => {
  // Stub authClient.me so tests do not make real HTTP calls. Default: reject
  // with a 401-like error so the user is treated as unauthenticated.
  beforeEach(() => {
    vi.spyOn(authClient, 'me').mockRejectedValue(new Error('401'));
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders without crashing with full provider setup', async () => {
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/login']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    // Wait for auth hydration to settle
    await waitFor(() => expect(screen.queryByRole('status')).not.toBeInTheDocument());
  });

  it('navigating to /login renders the Log In heading from LoginPage', async () => {
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/login']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    // LoginPage renders an h5 heading "Log In" inside AuthLayout.
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /log in/i })).toBeInTheDocument(),
    );
  });

  it('navigating to /register renders the Create Account heading', async () => {
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/register']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /create account/i })).toBeInTheDocument(),
    );
  });

  it('navigating to /goals when unauthenticated redirects to /login', async () => {
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/goals']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    // Unauthenticated — App should redirect to login. The login page or its
    // AuthLayout branding must be visible after hydration completes.
    await waitFor(() => expect(screen.getByText(/weigh to go/i)).toBeInTheDocument());
    // The goals page content should NOT be visible.
    expect(screen.queryByText(/coming in milestone 3/i)).not.toBeInTheDocument();
  });

  it('shows LoadingSplash while auth is hydrating on a protected route', () => {
    // me() never resolves — keeps isLoading=true indefinitely
    vi.spyOn(authClient, 'me').mockReturnValue(new Promise(() => undefined));
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/goals']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('renders protected route content when user is authenticated', async () => {
    vi.spyOn(authClient, 'me').mockResolvedValueOnce({
      user_id: 1,
      email: 'user@example.com',
      display_name: 'User',
      created_at: '2026-05-23T00:00:00Z',
    });
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument(),
    );
    expect(screen.queryByRole('heading', { name: /log in/i })).not.toBeInTheDocument();
  });

  it('redirects to /login after auth hydration when unauthenticated', async () => {
    render(
      <FullProviders>
        <MemoryRouter initialEntries={['/goals']}>
          <App />
        </MemoryRouter>
      </FullProviders>,
    );
    // After hydration completes (me() rejects → unauthenticated), redirect occurs
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: /log in/i })).toBeInTheDocument(),
    );
  });
});
