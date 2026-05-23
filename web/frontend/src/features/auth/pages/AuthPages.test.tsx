import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ThemeProvider } from '@mui/material';
import { theme } from '../../../theme/theme';
import { AuthProvider } from '../../../contexts/AuthContext';
import { authClient, type AuthUser } from '../api/auth-client';

import { LoginPage } from './LoginPage';
import { RegisterPage } from './RegisterPage';

const authenticatedUser: AuthUser = {
  user_id: 1,
  email: 'test@example.com',
  display_name: 'Test User',
  created_at: '2026-05-23T00:00:00Z',
};

function Wrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ThemeProvider theme={theme}>
          <AuthProvider>{children}</AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function AuthenticatedWrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/login']}>
        <ThemeProvider theme={theme}>
          <AuthProvider>
            <Routes>
              <Route path="/login" element={children} />
              <Route path="/" element={<div>Home Page</div>} />
            </Routes>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

function AuthenticatedRegisterWrapper({ children }: { children: React.ReactNode }) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/register']}>
        <ThemeProvider theme={theme}>
          <AuthProvider>
            <Routes>
              <Route path="/register" element={children} />
              <Route path="/" element={<div>Home Page</div>} />
            </Routes>
          </AuthProvider>
        </ThemeProvider>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.spyOn(authClient, 'me').mockResolvedValue(null as unknown as AuthUser);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the login form with a Log in button', async () => {
    render(
      <Wrapper>
        <LoginPage />
      </Wrapper>,
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument(),
    );
  });

  it('has at least one accessible heading', async () => {
    render(
      <Wrapper>
        <LoginPage />
      </Wrapper>,
    );
    await waitFor(() => expect(screen.getAllByRole('heading').length).toBeGreaterThan(0));
  });

  it('redirects authenticated users to /', async () => {
    vi.spyOn(authClient, 'me').mockResolvedValue(authenticatedUser);
    render(
      <AuthenticatedWrapper>
        <LoginPage />
      </AuthenticatedWrapper>,
    );
    await screen.findByText('Home Page');
  });
});

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.spyOn(authClient, 'me').mockResolvedValue(null as unknown as AuthUser);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders the register form with a Create account button', async () => {
    render(
      <Wrapper>
        <RegisterPage />
      </Wrapper>,
    );
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument(),
    );
  });

  it('has at least one accessible heading', async () => {
    render(
      <Wrapper>
        <RegisterPage />
      </Wrapper>,
    );
    await waitFor(() => expect(screen.getAllByRole('heading').length).toBeGreaterThan(0));
  });

  it('redirects authenticated users to /', async () => {
    vi.spyOn(authClient, 'me').mockResolvedValue(authenticatedUser);
    render(
      <AuthenticatedRegisterWrapper>
        <RegisterPage />
      </AuthenticatedRegisterWrapper>,
    );
    await screen.findByText('Home Page');
  });
});
