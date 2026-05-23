import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as weightClientModule from '../api/weight-client';
import { useWeightEntry } from './useWeightEntry';

const mockEntry: weightClientModule.WeightEntryRecord = {
  entry_id: 1,
  weight_value: 175.5,
  weight_unit: 'lbs',
  observation_date: '2026-05-20',
  notes: null,
  created_at: '2026-05-20T12:00:00Z',
  updated_at: '2026-05-20T12:00:00Z',
};

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useWeightEntry', () => {
  beforeEach(() => {
    vi.spyOn(weightClientModule.weightClient, 'get').mockResolvedValue(mockEntry);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('returns the entry on success', async () => {
    const { result } = renderHook(() => useWeightEntry(1), { wrapper });
    await waitFor(() => expect(result.current.data).toBeDefined());
    expect(result.current.data?.entry_id).toBe(1);
  });

  it('does not fetch when entryId is null', () => {
    const { result } = renderHook(() => useWeightEntry(null), { wrapper });
    expect(result.current.isFetching).toBe(false);
  });
});
