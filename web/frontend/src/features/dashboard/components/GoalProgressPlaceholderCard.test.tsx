import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { GoalProgressPlaceholderCard } from './GoalProgressPlaceholderCard';

describe('GoalProgressPlaceholderCard', () => {
  it('renders a milestone 3 placeholder message', () => {
    render(<GoalProgressPlaceholderCard />);
    expect(screen.getByText(/milestone 3/i)).toBeInTheDocument();
  });
});
