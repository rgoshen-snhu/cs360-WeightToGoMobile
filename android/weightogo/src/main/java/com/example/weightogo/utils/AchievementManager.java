package com.example.weightogo.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.weightogo.database.AchievementDAO;
import com.example.weightogo.database.GoalWeightDAO;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.Achievement;
import com.example.weightogo.models.GoalWeight;
import com.example.weightogo.models.WeightEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager class for detecting and awarding user achievements.
 * Implements FR3.1 - Achievement detection and tracking.
 *
 * Achievement Types:
 * - GOAL_REACHED: User reaches their goal weight
 * - FIRST_ENTRY: User logs their first weight entry
 * - STREAK_7: 7 consecutive days of logging
 * - STREAK_30: 30 consecutive days of logging
 * - MILESTONE_5: Lost 5 lbs/kg
 * - MILESTONE_10: Lost 10 lbs/kg
 * - MILESTONE_25: Lost 25 lbs/kg
 * - NEW_LOW: Reached new lowest weight
 */
public class AchievementManager {

    private static final String TAG = "AchievementManager";

    private final AchievementDAO achievementDAO;
    private final GoalWeightDAO goalWeightDAO;
    private final WeightEntryDAO weightEntryDAO;

    public AchievementManager(
            @NonNull AchievementDAO achievementDAO,
            @NonNull GoalWeightDAO goalWeightDAO,
            @NonNull WeightEntryDAO weightEntryDAO
    ) {
        this.achievementDAO = achievementDAO;
        this.goalWeightDAO = goalWeightDAO;
        this.weightEntryDAO = weightEntryDAO;
    }

    /**
     * Main entry point for checking all achievements for a user.
     * Called after a new weight entry is added.
     *
     * @param userId    User ID
     * @param newWeight New weight value just logged
     * @return List of newly awarded achievements
     */
    public List<Achievement> checkAchievements(long userId, double newWeight) {
        Log.d(TAG, "checkAchievements: Checking achievements for user_id=" + userId + ", new_weight=" + newWeight);

        List<Achievement> newAchievements = new ArrayList<>();

        // Check all achievement types
        checkGoalReached(userId, newWeight, newAchievements);
        checkFirstEntry(userId, newAchievements);
        checkStreaks(userId, newAchievements);
        checkMilestones(userId, newWeight, newAchievements);
        checkNewLow(userId, newWeight, newAchievements);

        Log.i(TAG, "checkAchievements: Awarded " + newAchievements.size() + " new achievements");
        return newAchievements;
    }

    /**
     * Check if user has reached their goal weight.
     */
    private void checkGoalReached(long userId, double newWeight, List<Achievement> newAchievements) {
        // Skip if already awarded
        if (achievementDAO.hasAchievementType(userId, "GOAL_REACHED")) {
            return;
        }

        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(userId);
        if (activeGoal == null) {
            return;
        }

        // Check if goal reached (within 0.5 lb tolerance)
        double goalWeight = activeGoal.getGoalWeight();
        if (Math.abs(newWeight - goalWeight) <= 0.5) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setGoalId(activeGoal.getGoalId());
            achievement.setAchievementType("GOAL_REACHED");
            achievement.setTitle("Goal Reached!");
            achievement.setDescription("Congratulations! You've reached your goal weight of " +
                    WeightUtils.formatWeightWithUnit(goalWeight, activeGoal.getGoalUnit()));
            achievement.setValue(goalWeight);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkGoalReached: Awarded GOAL_REACHED achievement");
            }
        }
    }

    /**
     * Check if this is user's first weight entry.
     */
    private void checkFirstEntry(long userId, List<Achievement> newAchievements) {
        // Skip if already awarded
        if (achievementDAO.hasAchievementType(userId, "FIRST_ENTRY")) {
            return;
        }

        List<WeightEntry> entries = weightEntryDAO.getWeightEntriesForUser(userId);

        // If no entries exist, this will be the first one
        if (entries.isEmpty()) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setAchievementType("FIRST_ENTRY");
            achievement.setTitle("First Entry!");
            achievement.setDescription("You've logged your first weight. Great start on your journey!");
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkFirstEntry: Awarded FIRST_ENTRY achievement");
            }
        }
    }

    /**
     * Check for consecutive day streaks (7-day and 30-day).
     */
    private void checkStreaks(long userId, List<Achievement> newAchievements) {
        // Optimization: Only fetch recent 31 entries instead of all entries
        // This prevents N+1 query problem for users with many entries
        List<WeightEntry> entries = weightEntryDAO.getRecentWeightEntriesForUser(userId, 31);

        // Need at least 7 entries total for STREAK_7 (checkAchievements called after entry saved)
        if (entries.size() < 7) {
            return;
        }

        // Calculate current streak (new entry already saved to DB)
        int currentStreak = calculateConsecutiveDaysIncludingToday(entries);

        // Check STREAK_7
        if (currentStreak >= 7 && !achievementDAO.hasAchievementType(userId, "STREAK_7")) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setAchievementType("STREAK_7");
            achievement.setTitle("7-Day Streak!");
            achievement.setDescription("You've logged your weight for 7 consecutive days. Keep it up!");
            achievement.setValue((double) currentStreak);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkStreaks: Awarded STREAK_7 achievement");
            }
        }

        // Check STREAK_30
        if (currentStreak >= 30 && !achievementDAO.hasAchievementType(userId, "STREAK_30")) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setAchievementType("STREAK_30");
            achievement.setTitle("30-Day Streak!");
            achievement.setDescription("Amazing! You've logged your weight for 30 consecutive days!");
            achievement.setValue((double) currentStreak);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkStreaks: Awarded STREAK_30 achievement");
            }
        }
    }

    /**
     * Calculate consecutive days with entries, including today.
     * Called after the new entry has been saved to the database.
     * Assumes entries are sorted by date descending.
     *
     * Note: Only counts streaks if there's an entry for today OR yesterday.
     * Backfilled entries (older than yesterday) don't start/continue a streak.
     */
    private int calculateConsecutiveDaysIncludingToday(List<WeightEntry> entries) {
        if (entries.isEmpty()) {
            return 0; // No entries, no streak
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate mostRecentDate = entries.get(0).getWeightDate();

        // Check if most recent entry is from today or yesterday
        // If it's older (backfilled), don't count as part of active streak
        if (mostRecentDate.isBefore(yesterday)) {
            // Most recent entry is older than yesterday - this is a backfilled entry
            // Don't award streak achievements for backfilled data
            return 0;
        }

        // Check the gap between most recent entry and today
        long daysSinceLastEntry = ChronoUnit.DAYS.between(mostRecentDate, today);

        if (daysSinceLastEntry > 1) {
            // Streak is broken - gap of more than 1 day
            return 0;
        }

        // Start counting streak from most recent entry (today or yesterday)
        int streak = 1;
        LocalDate previousDate = mostRecentDate;

        for (int i = 1; i < entries.size(); i++) {
            LocalDate currentDate = entries.get(i).getWeightDate();
            long daysBetween = ChronoUnit.DAYS.between(currentDate, previousDate);

            if (daysBetween == 1) {
                // Consecutive day
                streak++;
                previousDate = currentDate;
            } else {
                // Streak broken
                break;
            }
        }

        return streak;
    }

    /**
     * Check for weight loss milestones (5, 10, 25 lbs).
     */
    private void checkMilestones(long userId, double newWeight, List<Achievement> newAchievements) {
        GoalWeight activeGoal = goalWeightDAO.getActiveGoal(userId);
        if (activeGoal == null) {
            return;
        }

        double startWeight = activeGoal.getStartWeight();
        double goalWeight = activeGoal.getGoalWeight();
        String unit = activeGoal.getGoalUnit();

        // Determine if this is a weight loss or weight gain goal
        boolean isLossGoal = goalWeight < startWeight;
        double weightChange = startWeight - newWeight;  // Positive = lost, Negative = gained

        // Get absolute progress in correct direction
        double progress = Math.abs(weightChange);

        // Only award milestones if progressing in the right direction
        boolean progressingCorrectly = (isLossGoal && weightChange > 0) || (!isLossGoal && weightChange < 0);

        if (!progressingCorrectly) {
            return;  // User is moving in wrong direction, don't award milestones
        }

        // Check MILESTONE_5
        if (progress >= 5.0 && !achievementDAO.hasAchievementType(userId, "MILESTONE_5")) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setGoalId(activeGoal.getGoalId());
            achievement.setAchievementType("MILESTONE_5");
            achievement.setTitle(isLossGoal
                ? String.format("5 %s Lost!", unit)
                : String.format("5 %s Gained!", unit));
            achievement.setDescription(isLossGoal
                ? String.format("You've lost 5 %s! You're making great progress!", unit)
                : String.format("You've gained 5 %s! You're making great progress!", unit));
            achievement.setValue(5.0);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkMilestones: Awarded MILESTONE_5 achievement");
            }
        }

        // Check MILESTONE_10
        if (progress >= 10.0 && !achievementDAO.hasAchievementType(userId, "MILESTONE_10")) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setGoalId(activeGoal.getGoalId());
            achievement.setAchievementType("MILESTONE_10");
            achievement.setTitle(isLossGoal
                ? String.format("10 %s Lost!", unit)
                : String.format("10 %s Gained!", unit));
            achievement.setDescription(isLossGoal
                ? String.format("Amazing! You've lost 10 %s!", unit)
                : String.format("Amazing! You've gained 10 %s!", unit));
            achievement.setValue(10.0);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkMilestones: Awarded MILESTONE_10 achievement");
            }
        }

        // Check MILESTONE_25
        if (progress >= 25.0 && !achievementDAO.hasAchievementType(userId, "MILESTONE_25")) {
            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setGoalId(activeGoal.getGoalId());
            achievement.setAchievementType("MILESTONE_25");
            achievement.setTitle(isLossGoal
                ? String.format("25 %s Lost!", unit)
                : String.format("25 %s Gained!", unit));
            achievement.setDescription(isLossGoal
                ? String.format("Incredible! You've lost 25 %s! You're a superstar!", unit)
                : String.format("Incredible! You've gained 25 %s! You're a superstar!", unit));
            achievement.setValue(25.0);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkMilestones: Awarded MILESTONE_25 achievement");
            }
        }
    }

    /**
     * Check if user reached a new lowest weight.
     * Uses optimized SQL MIN() query instead of fetching all entries.
     */
    private void checkNewLow(long userId, double newWeight, List<Achievement> newAchievements) {
        // Optimized: Use SQL MIN() instead of loading all entries
        Double minPreviousWeight = weightEntryDAO.getMinWeightForUser(userId);

        if (minPreviousWeight == null) {
            // This is the first entry, so it's automatically a new low
            // But we don't award NEW_LOW for the first entry (it's not meaningful)
            return;
        }

        // Check if new weight is lower than previous minimum
        if (newWeight < minPreviousWeight) {
            // Get unit from latest weight entry
            WeightEntry latestEntry = weightEntryDAO.getLatestWeightEntry(userId);
            String unit = (latestEntry != null) ? latestEntry.getWeightUnit() : "lbs";

            Achievement achievement = new Achievement();
            achievement.setUserId(userId);
            achievement.setAchievementType("NEW_LOW");
            achievement.setTitle("New Low!");
            achievement.setDescription("You've reached a new lowest weight of " +
                    WeightUtils.formatWeightWithUnit(newWeight, unit) + "!");
            achievement.setValue(newWeight);
            achievement.setAchievedAt(LocalDateTime.now());
            achievement.setNotified(false);

            long achievementId = achievementDAO.insertAchievement(achievement);
            if (achievementId > 0) {
                achievement.setAchievementId(achievementId);
                newAchievements.add(achievement);
                Log.i(TAG, "checkNewLow: Awarded NEW_LOW achievement");
            }
        }
    }
}
