import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { WeightEntryForm } from './WeightEntryForm';
import type { WeightEntryFormValues } from '../schemas/weight-schemas';

const TODAY = new Date().toISOString().split('T')[0]!;

describe('WeightEntryForm', () => {
  it('renders all form fields', () => {
    render(<WeightEntryForm onSubmit={vi.fn()} />);
    expect(screen.getByLabelText(/weight value/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/weight unit/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/observation date/i)).toBeInTheDocument();
  });

  it('renders empty in create mode (no defaultValues)', () => {
    render(<WeightEntryForm onSubmit={vi.fn()} />);
    const input = screen.getByLabelText(/weight value/i) as HTMLInputElement;
    expect(input.value).toBe('');
  });

  it('renders pre-populated in edit mode', () => {
    const defaults: WeightEntryFormValues = {
      weight_value: 175,
      weight_unit: 'lbs',
      observation_date: TODAY,
      notes: 'Test note',
    };
    render(<WeightEntryForm onSubmit={vi.fn()} defaultValues={defaults} />);
    const input = screen.getByLabelText(/weight value/i) as HTMLInputElement;
    expect(input.value).toBe('175');
  });

  it('blocks submit when weight value is empty', async () => {
    const onSubmit = vi.fn();
    render(<WeightEntryForm onSubmit={onSubmit} />);
    await userEvent.click(screen.getByRole('button', { name: /save/i }));
    await waitFor(() => expect(onSubmit).not.toHaveBeenCalled());
  });

  it('calls onSubmit with validated values on valid input', async () => {
    const onSubmit = vi.fn();
    render(<WeightEntryForm onSubmit={onSubmit} />);
    await userEvent.type(screen.getByLabelText(/weight value/i), '175');
    // observation_date has default to today set in the form
    await userEvent.click(screen.getByRole('button', { name: /save/i }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalled());
  });

  it('shows notes character counter', async () => {
    render(<WeightEntryForm onSubmit={vi.fn()} />);
    await userEvent.type(screen.getByLabelText(/notes/i), 'Hello');
    expect(screen.getByText(/5 \/ 500/)).toBeInTheDocument();
  });
});
