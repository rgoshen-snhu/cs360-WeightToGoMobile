package com.example.weightogo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weightogo.R;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.utils.DateUtils;
import com.example.weightogo.utils.WeightUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * RecyclerView adapter for displaying goal history items.
 * Shows past goals with achievement status, stats, and dates.
 */
public class GoalHistoryAdapter extends RecyclerView.Adapter<GoalHistoryAdapter.ViewHolder> {

    private final List<GoalWeight> goals;

    /**
     * Constructor for GoalHistoryAdapter.
     *
     * @param goals list of past goals to display
     */
    public GoalHistoryAdapter(List<GoalWeight> goals) {
        this.goals = goals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GoalWeight goal = goals.get(position);

        // Show/hide achievement badge
        if (goal.isAchieved()) {
            holder.achievementBadge.setVisibility(View.VISIBLE);
            holder.labelAchieved.setVisibility(View.VISIBLE);
        } else {
            holder.achievementBadge.setVisibility(View.GONE);
            holder.labelAchieved.setVisibility(View.GONE);
        }

        // Goal weight value
        String goalWeightText = WeightUtils.formatWeightWithUnit(goal.getGoalWeight(), goal.getGoalUnit());
        holder.textGoalWeightValue.setText(goalWeightText);

        // Calculate lbs lost
        double lbsLost = Math.abs(goal.getStartWeight() - goal.getGoalWeight());
        String lbsLostText = String.format(holder.itemView.getContext().getString(R.string.lost_format), lbsLost);
        holder.textLbsLost.setText(lbsLostText);

        // Calculate duration
        LocalDate startDate = goal.getCreatedAt().toLocalDate();
        LocalDate endDate = goal.getUpdatedAt().toLocalDate();
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        String durationText = String.format(holder.itemView.getContext().getString(R.string.duration_days), daysBetween);
        holder.textDuration.setText(durationText);

        // Date range
        String startDateStr = DateUtils.formatDateShort(startDate);
        String endDateStr = DateUtils.formatDateShort(endDate);
        String dateRangeText = String.format(holder.itemView.getContext().getString(R.string.date_range_format),
                startDateStr, endDateStr);
        holder.textDates.setText(dateRangeText);

        // Target date (if set)
        if (goal.getTargetDate() != null) {
            String targetDateText = String.format(
                    holder.itemView.getContext().getString(R.string.target_date_display),
                    DateUtils.formatDateFull(goal.getTargetDate()));
            holder.textTargetDate.setText(targetDateText);
            holder.textTargetDate.setVisibility(View.VISIBLE);
        } else {
            holder.textTargetDate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    /**
     * Update the adapter's data and notify of changes.
     * More efficient than calling notifyDataSetChanged() externally.
     *
     * @param newGoals updated list of goals
     */
    public void updateGoals(List<GoalWeight> newGoals) {
        // Clear and update in place to maintain the same list reference
        goals.clear();
        if (newGoals != null && !newGoals.isEmpty()) {
            goals.addAll(newGoals);
        }
        notifyDataSetChanged();  // For small lists this is acceptable; DiffUtil for larger lists
    }

    /**
     * ViewHolder for goal history items.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView achievementBadge;
        TextView textGoalWeightValue;
        TextView labelAchieved;
        TextView textLbsLost;
        TextView textDuration;
        TextView textDates;
        TextView textTargetDate;

        ViewHolder(View itemView) {
            super(itemView);
            achievementBadge = itemView.findViewById(R.id.achievement_badge);
            textGoalWeightValue = itemView.findViewById(R.id.text_goal_weight_value);
            labelAchieved = itemView.findViewById(R.id.label_achieved);
            textLbsLost = itemView.findViewById(R.id.text_lbs_lost);
            textDuration = itemView.findViewById(R.id.text_duration);
            textDates = itemView.findViewById(R.id.text_dates);
            textTargetDate = itemView.findViewById(R.id.text_target_date);
        }
    }
}
