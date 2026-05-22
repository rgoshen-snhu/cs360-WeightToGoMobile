package com.example.weightogo.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Model class representing a weight goal.
 * Corresponds to the goal_weights table in the database.
 */
public class GoalWeight {

    /** Primary key - unique identifier for goal */
    private long goalId;

    /** Foreign key reference to users table */
    private long userId;

    /** Target weight value */
    private double goalWeight;

    /** Unit of measurement: 'lbs' or 'kg' */
    @NonNull private String goalUnit;

    /** Starting weight when goal was created */
    private double startWeight;

    /** Optional target date to achieve goal (date only) */
    @Nullable private LocalDate targetDate;

    /** Achievement status - true if goal reached, false if in progress */
    private boolean isAchieved;

    /** Date when goal was achieved (null if not yet achieved) */
    @Nullable private LocalDate achievedDate;

    /** Timestamp when goal was created */
    @NonNull private LocalDateTime createdAt;

    /** Timestamp when goal was last updated */
    @NonNull private LocalDateTime updatedAt;

    /** Active status - only one goal per user can be active at a time */
    private boolean isActive;

    /**
     * Default constructor.
     */
    public GoalWeight() {
    }

    public long getGoalId() {
        return goalId;
    }

    public void setGoalId(long goalId) {
        this.goalId = goalId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public double getGoalWeight() {
        return goalWeight;
    }

    public void setGoalWeight(double goalWeight) {
        this.goalWeight = goalWeight;
    }

    @NonNull
    public String getGoalUnit() {
        return goalUnit;
    }

    public void setGoalUnit(@NonNull String goalUnit) {
        this.goalUnit = goalUnit;
    }

    public double getStartWeight() {
        return startWeight;
    }

    public void setStartWeight(double startWeight) {
        this.startWeight = startWeight;
    }

    @Nullable
    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(@Nullable LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public boolean isAchieved() {
        return isAchieved;
    }

    public void setAchieved(boolean achieved) {
        this.isAchieved = achieved;
    }

    @Nullable
    public LocalDate getAchievedDate() {
        return achievedDate;
    }

    public void setAchievedDate(@Nullable LocalDate achievedDate) {
        this.achievedDate = achievedDate;
    }

    @NonNull
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NonNull LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @NonNull
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(@NonNull LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    /**
     * Equality based on goalId (primary key).
     * Note: This implementation assumes GoalWeight will not be subclassed.
     * Two goals are equal if they have the same non-zero goalId.
     * Uninitialized goals (goalId=0) are never equal to prevent false matches.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoalWeight)) return false;
        GoalWeight that = (GoalWeight) o;
        return goalId != 0 && goalId == that.goalId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(goalId);
    }

    @NonNull
    @Override
    public String toString() {
        return "GoalWeight{" +
                "goalId=" + goalId +
                ", userId=" + userId +
                ", goalWeight=" + goalWeight +
                ", goalUnit='" + goalUnit + '\'' +
                ", startWeight=" + startWeight +
                ", targetDate=" + targetDate +
                ", isAchieved=" + isAchieved +
                ", achievedDate=" + achievedDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isActive=" + isActive +
                '}';
    }
}
