/**
 * Progress bar toward the active goal (FR-D-4).
 *
 * Three states per DDR-0005:
 * - No active goal: renders a call-to-action link.
 * - Active goal, no weight entries: value=0 bar with "No entries yet" helper.
 * - Active goal with entries: determinate bar at progress_percent.
 */

import { Box, LinearProgress, Link, Typography } from '@mui/material';

interface GoalProgressBarProps {
  /** Progress in [0, 100], or null when no weight entries exist. */
  progressPercent: number | null;
  /** Whether the user has an active goal. */
  hasGoal: boolean;
  /** Called when the user clicks the no-goal CTA. */
  onSetGoal?: () => void;
}

/**
 * Determinate MUI LinearProgress with an accessible numeric percent label.
 *
 * Per DDR-0005: uses a text label in addition to the filled bar so progress
 * is readable without relying on colour alone (WCAG 1.4.1).
 */
export function GoalProgressBar({ progressPercent, hasGoal, onSetGoal }: GoalProgressBarProps) {
  if (!hasGoal) {
    return (
      <Box>
        <Typography variant="body2" color="text.secondary">
          No active goal.{' '}
          <Link
            component="button"
            variant="body2"
            onClick={onSetGoal}
            aria-label="Set a goal to track your progress"
          >
            Set a goal to track your progress
          </Link>
        </Typography>
      </Box>
    );
  }

  const value = progressPercent ?? 0;
  const helperText = progressPercent === null ? 'No entries yet' : `${value.toFixed(0)}%`;

  return (
    <Box>
      <LinearProgress
        variant="determinate"
        value={value}
        aria-label="Goal progress"
        aria-valuetext={helperText}
        sx={{ height: 8, borderRadius: 1 }}
      />
      <Typography
        variant="caption"
        color={progressPercent === null ? 'text.secondary' : 'text.primary'}
        sx={{ display: 'block', textAlign: 'right', mt: 0.5 }}
      >
        {helperText}
      </Typography>
    </Box>
  );
}
