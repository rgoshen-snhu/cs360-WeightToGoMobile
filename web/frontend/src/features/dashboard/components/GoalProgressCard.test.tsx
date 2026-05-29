import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import * as goalClientModule from '../../goals/api/goal-client';
import { GoalProgressCard } from './GoalProgressCard';

function Wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <MemoryRouter>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </MemoryRouter>
  );
}

describe('GoalProgressCard', () => {
  beforeEach(() => {
    vi.spyOn(goalClientModule.goalClient, 'getActive').mockResolvedValue({
      goal: null,
      progress_percent: null,
      current_value: null,
    });
  });

  it('renders the Goal Progress title', () => {
    render(<GoalProgressCard />, { wrapper: Wrapper });
    expect(screen.getByText(/goal progress/i)).toBeInTheDocument();
  });

  it('shows CTA when no active goal', async () => {
    render(<GoalProgressCard />, { wrapper: Wrapper });
    await screen.findByText(/set a goal/i);
    expect(screen.getByText(/set a goal/i)).toBeInTheDocument();
  });

  it('shows loading text while query is pending', () => {
    vi.spyOn(goalClientModule.goalClient, 'getActive').mockReturnValue(
      new Promise(() => {
        /* never resolves */
      }),
    );
    render(<GoalProgressCard />, { wrapper: Wrapper });
    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });

  it('shows error message when query fails', async () => {
    vi.spyOn(goalClientModule.goalClient, 'getActive').mockRejectedValue(
      new Error('Network error'),
    );
    render(<GoalProgressCard />, { wrapper: Wrapper });
    await screen.findByText(/failed to load goal progress/i);
  });

  it('shows progress bar when active goal with progress', async () => {
    vi.spyOn(goalClientModule.goalClient, 'getActive').mockResolvedValue({
      goal: {
        goal_id: 1,
        user_id: 1,
        target_value: 150,
        target_unit: 'lbs',
        start_value: 200,
        goal_type: 'lose',
        target_date: null,
        is_active: true,
        is_achieved: false,
        achieved_at: null,
        created_at: '2026-05-28T00:00:00Z',
        updated_at: '2026-05-28T00:00:00Z',
      },
      progress_percent: 50,
      current_value: 175,
    });
    render(<GoalProgressCard />, { wrapper: Wrapper });
    await screen.findByRole('progressbar');
    expect(screen.getByText('50%')).toBeInTheDocument();
  });
});
