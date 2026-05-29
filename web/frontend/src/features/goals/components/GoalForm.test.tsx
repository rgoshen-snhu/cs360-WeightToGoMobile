import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { GoalForm } from './GoalForm';

vi.mock('../../../contexts/PreferencesContext', () => ({
  usePreferences: () => ({
    preferences: {
      weightUnit: 'lbs',
      notifyAchievement: true,
      notifyMilestone: true,
      notifyStreak: true,
    },
    isLoading: false,
    setPreference: vi.fn(),
  }),
}));

describe('GoalForm', () => {
  it('renders all create-mode fields', () => {
    render(<GoalForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/target weight/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/starting weight/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /set goal/i })).toBeInTheDocument();
  });

  it('shows the set-goal button in create mode', () => {
    render(<GoalForm onSubmit={vi.fn()} />);
    expect(screen.getByRole('button', { name: /set goal/i })).toBeInTheDocument();
  });

  it('shows the update-goal button in edit mode', () => {
    render(<GoalForm onSubmit={vi.fn()} isEditMode />);
    expect(screen.getByRole('button', { name: /update goal/i })).toBeInTheDocument();
  });

  it('blocks submit when target weight is empty', async () => {
    const onSubmit = vi.fn();
    render(<GoalForm onSubmit={onSubmit} />);
    await userEvent.click(screen.getByRole('button', { name: /set goal/i }));
    await waitFor(() => expect(onSubmit).not.toHaveBeenCalled());
  });

  it('shows conflict error alert when conflictError is set', () => {
    render(<GoalForm onSubmit={vi.fn()} conflictError="You already have an active goal." />);
    expect(screen.getByRole('alert')).toHaveTextContent(/already have an active goal/i);
  });

  it('disables submit button when isSubmitting is true', () => {
    render(<GoalForm onSubmit={vi.fn()} isSubmitting />);
    expect(screen.getByRole('button', { name: /set goal/i })).toBeDisabled();
  });
});
