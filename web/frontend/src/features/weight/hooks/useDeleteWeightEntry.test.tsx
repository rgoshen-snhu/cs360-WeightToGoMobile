import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as weightClientModule from '../api/weight-client';
import { useDeleteWeightEntry } from './useDeleteWeightEntry';

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useDeleteWeightEntry', () => {
  beforeEach(() => {
    vi.spyOn(weightClientModule.weightClient, 'remove').mockResolvedValue(undefined);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('calls weightClient.remove with the entryId', async () => {
    const { result } = renderHook(() => useDeleteWeightEntry(), { wrapper });
    result.current.mutate(1);
    await waitFor(() => expect(weightClientModule.weightClient.remove).toHaveBeenCalledWith(1));
  });
});
