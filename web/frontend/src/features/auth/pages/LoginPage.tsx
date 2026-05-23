/**
 * Login page.
 *
 * Composes AuthLayout, LoginForm, and useLogin to provide a fully wired
 * login screen. Redirects authenticated users immediately to the app root.
 *
 * Requirements: SRS §3.1 FR-01, FR-02.
 */

import { Typography } from '@mui/material';
import { Navigate } from 'react-router-dom';
import { AuthLayout } from '../../../components/AuthLayout';
import { useAuth } from '../../../contexts/AuthContext';
import { LoginForm } from '../components/LoginForm';
import { useLogin } from '../hooks/useLogin';

/**
 * Renders the login page at /login.
 */
export function LoginPage() {
  const { isAuthenticated, isLoading } = useAuth();
  const { submit, status, formError } = useLogin();

  if (isLoading) return null;
  if (isAuthenticated) return <Navigate to="/" replace />;

  return (
    <AuthLayout>
      <Typography variant="h5" component="h2" gutterBottom>
        Log In
      </Typography>
      <LoginForm onSubmit={submit} status={status} formError={formError} />
    </AuthLayout>
  );
}
