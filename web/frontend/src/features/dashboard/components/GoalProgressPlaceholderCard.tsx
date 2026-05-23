import { Card, CardContent, Typography } from '@mui/material';

/**
 * Placeholder card for the goal progress widget.
 *
 * Goal integration is deferred to Milestone 3 (SRS §13.1.4).  This card uses
 * disabled visual treatment to indicate the feature is not yet available.
 */
export function GoalProgressPlaceholderCard() {
  return (
    <Card sx={{ opacity: 0.5 }}>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Goal Progress
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Coming in Milestone 3
        </Typography>
      </CardContent>
    </Card>
  );
}
