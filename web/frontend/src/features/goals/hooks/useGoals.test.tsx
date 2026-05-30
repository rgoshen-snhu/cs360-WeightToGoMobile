import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import * as goalClientModule from '../api/goal-client';
import { useGoals } from './useGoals';

const mockList: goalClientModule.GoalListResponse = { goals: [] };

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('useGoals', () => {
  beforeEach(() => {
    vi.spyOn(goalClientModule.goalClient, 'list').mockResolvedValue(mockList);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('calls goalClient.list', async () => {
    const { result } = renderHook(() => useGoals(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(goalClientModule.goalClient.list).toHaveBeenCalled();
  });

  it('returns empty goals list', async () => {
    const { result } = renderHook(() => useGoals(), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.goals).toEqual([]);
  });

  it('requests history goals when called with { history: true }', async () => {
    const { result } = renderHook(() => useGoals({ history: true }), { wrapper });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(goalClientModule.goalClient.list).toHaveBeenCalledWith({ history: true });
  });
});
