package com.example.weightogo.models;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;

/**
 * Model class representing a user preference setting.
 * Corresponds to the user_preferences table in the database.
 *
 * Implements key-value storage for user settings.
 *
 * Standard Preference Keys:
 * - weight_unit: Default weight unit ('lbs' or 'kg')
 * - theme: App theme ('light', 'dark', 'system')
 * - notifications_enabled: Enable push notifications ('true', 'false')
 * - sms_notifications_enabled: Enable SMS notifications ('true', 'false')
 * - sms_goal_alerts: Send SMS when goal reached ('true', 'false')
 * - sms_reminder_enabled: Send daily reminder via SMS ('true', 'false')
 * - sms_milestone_alerts: Send SMS for weight milestones ('true', 'false')
 * - reminder_time: Daily reminder time ('HH:MM')
 * - first_day_of_week: Week start day ('sunday', 'monday')
 * - date_format: Date display format
 */
public class UserPreference {

    /** Primary key - unique identifier for preference */
    private long preferenceId;

    /** Foreign key reference to users table */
    private long userId;

    /** Preference key name (e.g., 'weight_unit', 'theme') */
    @NonNull private String prefKey;

    /** Preference value (stored as string) */
    @NonNull private String prefValue;

    /** Timestamp when preference was created */
    @NonNull private LocalDateTime createdAt;

    /** Timestamp when preference was last updated */
    @NonNull private LocalDateTime updatedAt;

    /**
     * Default constructor.
     */
    public UserPreference() {
    }

    public long getPreferenceId() {
        return preferenceId;
    }

    public void setPreferenceId(long preferenceId) {
        this.preferenceId = preferenceId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @NonNull
    public String getPrefKey() {
        return prefKey;
    }

    public void setPrefKey(@NonNull String prefKey) {
        this.prefKey = prefKey;
    }

    @NonNull
    public String getPrefValue() {
        return prefValue;
    }

    public void setPrefValue(@NonNull String prefValue) {
        this.prefValue = prefValue;
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

    @NonNull
    @Override
    public String toString() {
        return "UserPreference{" +
                "preferenceId=" + preferenceId +
                ", userId=" + userId +
                ", prefKey='" + prefKey + '\'' +
                ", prefValue='" + prefValue + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
