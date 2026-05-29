import { Card, CardContent, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { GoalProgressBar } from '../../goals/components/GoalProgressBar';
import { useActiveGoal } from '../../goals/hooks/useActiveGoal';

/**
 * Dashboard card showing goal progress (FR-D-4).
 *
 * Replaces GoalProgressPlaceholderCard. Uses useActiveGoal to fetch
 * the active goal and progress without repeating the query (TanStack
 * Query deduplicates concurrent requests for the same key).
 */
export function GoalProgressCard() {
  const { data, isLoading, isError } = useActiveGoal();
  const navigate = useNavigate();

  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Goal Progress
        </Typography>
        {isLoading ? (
          <Typography variant="body2" color="text.secondary">
            Loading…
          </Typography>
        ) : isError ? (
          <Typography variant="body2" color="error">
            Failed to load goal progress.
          </Typography>
        ) : (
          <GoalProgressBar
            hasGoal={data?.goal != null}
            progressPercent={data?.progress_percent ?? null}
            onSetGoal={() => navigate('/goals')}
          />
        )}
      </CardContent>
    </Card>
  );
}
