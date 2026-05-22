package com.example.weightogo.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDateTime;

/**
 * Model class representing a milestone achievement.
 * Corresponds to the achievements table in the database.
 *
 * Achievement Types:
 * - GOAL_REACHED: Target weight achieved
 * - FIRST_ENTRY: First weight logged
 * - STREAK_7: 7-day logging streak
 * - STREAK_30: 30-day logging streak
 * - MILESTONE_5: Lost 5 lbs/kg
 * - MILESTONE_10: Lost 10 lbs/kg
 * - MILESTONE_25: Lost 25 lbs/kg
 * - MILESTONE_50: Lost 50 lbs/kg
 * - NEW_LOW: New lowest weight
 */
public class Achievement {

    /** Primary key - unique identifier for achievement */
    private long achievementId;

    /** Foreign key reference to users table */
    private long userId;

    /** Foreign key reference to goal_weights table (optional - may be null) */
    @Nullable private Long goalId;

    /** Type of achievement (e.g., GOAL_REACHED, FIRST_ENTRY, STREAK_7) */
    @NonNull private String achievementType;

    /** Achievement title for display */
    @NonNull private String title;

    /** Detailed description of the achievement */
    @Nullable private String description;

    /** Associated value (e.g., pounds lost, streak days) */
    @Nullable private Double value;

    /** Timestamp when achievement was earned */
    @NonNull private LocalDateTime achievedAt;

    /** Flag indicating if user has been notified about this achievement */
    private boolean isNotified;

    /**
     * Default constructor.
     */
    public Achievement() {
    }

    public long getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(long achievementId) {
        this.achievementId = achievementId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Nullable
    public Long getGoalId() {
        return goalId;
    }

    public void setGoalId(@Nullable Long goalId) {
        this.goalId = goalId;
    }

    @NonNull
    public String getAchievementType() {
        return achievementType;
    }

    public void setAchievementType(@NonNull String achievementType) {
        this.achievementType = achievementType;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public Double getValue() {
        return value;
    }

    public void setValue(@Nullable Double value) {
        this.value = value;
    }

    @NonNull
    public LocalDateTime getAchievedAt() {
        return achievedAt;
    }

    public void setAchievedAt(@NonNull LocalDateTime achievedAt) {
        this.achievedAt = achievedAt;
    }

    public boolean isNotified() {
        return isNotified;
    }

    public void setNotified(boolean notified) {
        isNotified = notified;
    }

    @NonNull
    @Override
    public String toString() {
        return "Achievement{" +
                "achievementId=" + achievementId +
                ", userId=" + userId +
                ", goalId=" + goalId +
                ", achievementType='" + achievementType + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", value=" + value +
                ", achievedAt=" + achievedAt +
                ", isNotified=" + isNotified +
                '}';
    }
}
