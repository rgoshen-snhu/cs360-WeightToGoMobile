import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../../contexts/AuthContext';
import { authClient, type AuthUser } from '../api/auth-client';
import { ApiError, ValidationError } from '../../../lib/api-client';
import type { LoginFormHelpers } from '../components/LoginForm';
import type { LoginFormValues } from '../schemas/auth-schemas';
import {
  AUTH_INVALID_CREDENTIALS,
  AUTH_ACCOUNT_LOCKED,
  AUTH_RATE_LIMITED,
  AUTH_GENERIC_FAILURE,
} from '../messages';

export function useLogin() {
  const { setUser } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [formError, setFormError] = useState<string | null>(null);

  const mutation = useMutation<
    AuthUser,
    Error,
    { values: LoginFormValues; helpers: LoginFormHelpers }
  >({
    mutationFn: ({ values }) => authClient.login(values),
    onSuccess: (user) => {
      setUser(user);
      const dest = searchParams.get('from') ?? '/';
      navigate(dest, { replace: true });
    },
    onError: (error, vars) => {
      if (error instanceof ValidationError) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          vars.helpers.setError(field as keyof LoginFormValues, { type: 'server', message });
        }
        return;
      }
      if (error instanceof ApiError) {
        if (error.status === 401) {
          setFormError(AUTH_INVALID_CREDENTIALS);
          // Clear the password field after a failed attempt so the browser
          // does not auto-fill the wrong credential on the next try.
          vars.helpers.resetField('password');
        } else if (error.status === 423) {
          setFormError(AUTH_ACCOUNT_LOCKED);
          vars.helpers.resetField('password');
        } else if (error.status === 429) {
          setFormError(AUTH_RATE_LIMITED);
        } else {
          setFormError(AUTH_GENERIC_FAILURE);
        }
      } else {
        setFormError(AUTH_GENERIC_FAILURE);
      }
    },
  });

  return {
    submit: (values: LoginFormValues, helpers: LoginFormHelpers) => {
      setFormError(null);
      mutation.mutate({ values, helpers });
    },
    status: mutation.isPending ? ('submitting' as const) : ('idle' as const),
    formError,
  };
}
