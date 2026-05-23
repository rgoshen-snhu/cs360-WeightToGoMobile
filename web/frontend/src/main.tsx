/**
 * Application entry point.
 *
 * Mounts the provider tree in the following order (outermost → innermost):
 *
 * 1. StrictMode     — React double-invoke checks in development
 * 2. BrowserRouter  — React Router context
 * 3. QueryClientProvider — TanStack Query server-state cache
 * 4. AuthProvider   — authentication state (user, isAuthenticated)
 * 5. PreferencesProvider — display preferences (unit, colour scheme)
 * 6. ThemeProvider  — Material UI theme
 * 7. CssBaseline    — normalize browser stylesheets
 *
 * The split between main.tsx (BrowserRouter) and App.tsx (Routes) allows
 * tests to supply a MemoryRouter without altering the component tree.
 */

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';

import { App } from './App';
import { authClient } from './features/auth/api/auth-client';
import { installAuthRefreshInterceptor } from './lib/api-client';
import { AuthProvider } from './contexts/AuthContext';
import { PreferencesProvider } from './contexts/PreferencesContext';
import { theme } from './theme/theme';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      staleTime: 60_000,
    },
  },
});

installAuthRefreshInterceptor({
  refresh: async () => {
    await authClient.refresh();
  },
  onLogout: () => {
    // Only hard-redirect when the user had an active session that just expired.
    // If the query cache has no user yet (null | undefined) we are still in the
    // initial /me probe — React Router's ProtectedRoute will redirect to
    // /login?from=<path> on its own, and firing window.location.assign here
    // would override that redirect and strip the ?from= query parameter.
    const hadSession =
      queryClient.getQueryData<{ user_id: number } | null>(['auth', 'me']) !== null &&
      queryClient.getQueryData<{ user_id: number } | null>(['auth', 'me']) !== undefined;
    queryClient.setQueryData(['auth', 'me'], null);
    if (hadSession) {
      window.location.assign('/login');
    }
  },
});

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <PreferencesProvider>
            <ThemeProvider theme={theme}>
              <CssBaseline />
              <App />
            </ThemeProvider>
          </PreferencesProvider>
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </StrictMode>,
);
