package com.example.weightogo.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weightogo.models.Achievement;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Achievement operations.
 * Handles all database CRUD operations for achievements table.
 *
 * <p><strong>Database Lifecycle:</strong> This DAO uses a singleton WeighToGoDBHelper instance.
 * The helper manages the database connection lifecycle, so individual methods do NOT close
 * the SQLiteDatabase instance obtained via getReadableDatabase() or getWritableDatabase().
 * The singleton pattern ensures efficient connection pooling and prevents resource leaks.</p>
 *
 * <p><strong>Achievement Types:</strong> GOAL_REACHED, FIRST_ENTRY, STREAK_7, STREAK_30,
 * MILESTONE_5, MILESTONE_10, MILESTONE_25, MILESTONE_50, NEW_LOW</p>
 */
public class AchievementDAO {

    private static final String TAG = "AchievementDAO";
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WeighToGoDBHelper dbHelper;

    public AchievementDAO(@NonNull WeighToGoDBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a new achievement.
     *
     * @param achievement Achievement to insert
     * @return achievement_id if successful, -1 on error (including foreign key constraint violations)
     */
    public long insertAchievement(@NonNull Achievement achievement) {
        Log.d(TAG, "insertAchievement: Inserting achievement type=" + achievement.getAchievementType() +
                " for user_id=" + achievement.getUserId());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", achievement.getUserId());
        values.put("achievement_type", achievement.getAchievementType());
        values.put("title", achievement.getTitle());
        values.put("achieved_at", achievement.getAchievedAt().format(ISO_DATETIME_FORMATTER));
        values.put("is_notified", achievement.isNotified() ? 1 : 0);

        // Optional fields
        if (achievement.getGoalId() != null) {
            values.put("goal_id", achievement.getGoalId());
        }
        if (achievement.getDescription() != null) {
            values.put("description", achievement.getDescription());
        }
        if (achievement.getValue() != null) {
            values.put("value", achievement.getValue());
        }

        try {
            long achievementId = db.insert(WeighToGoDBHelper.TABLE_ACHIEVEMENTS, null, values);
            if (achievementId > 0) {
                Log.i(TAG, "insertAchievement: Successfully inserted achievement_id=" + achievementId);
            } else if (achievementId == -1) {
                // Foreign key constraint violation (invalid user_id or goal_id)
                Log.e(TAG, "insertAchievement: Foreign key constraint violated for user_id=" +
                        achievement.getUserId());
            }
            return achievementId;
        } catch (Exception e) {
            Log.e(TAG, "insertAchievement: Exception", e);
            return -1;
        }
    }

    /**
     * Gets all achievements for a user, ordered by achieved_at DESC (most recent first).
     *
     * @param userId User ID
     * @return List of achievements (empty list if none found)
     */
    @NonNull
    public List<Achievement> getAchievementsForUser(long userId) {
        Log.d(TAG, "getAchievementsForUser: user_id=" + userId);

        List<Achievement> achievements = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_ACHIEVEMENTS,
            null,
            "user_id = ?",
            new String[]{String.valueOf(userId)},
            null, null,
            "achieved_at DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                achievements.add(mapCursorToAchievement(cursor));
            }
            Log.i(TAG, "getAchievementsForUser: Found " + achievements.size() + " achievements");
        } catch (Exception e) {
            Log.e(TAG, "getAchievementsForUser: Exception", e);
        }

        return achievements;
    }

    /**
     * Gets achievements of a specific type for a user.
     *
     * @param userId User ID
     * @param achievementType Achievement type (e.g., "GOAL_REACHED", "MILESTONE_5")
     * @return List of matching achievements
     */
    @NonNull
    public List<Achievement> getAchievementsByType(long userId, @NonNull String achievementType) {
        Log.d(TAG, "getAchievementsByType: user_id=" + userId + ", type=" + achievementType);

        List<Achievement> achievements = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_ACHIEVEMENTS,
            null,
            "user_id = ? AND achievement_type = ?",
            new String[]{String.valueOf(userId), achievementType},
            null, null,
            "achieved_at DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                achievements.add(mapCursorToAchievement(cursor));
            }
            Log.i(TAG, "getAchievementsByType: Found " + achievements.size() + " " +
                    achievementType + " achievements");
        } catch (Exception e) {
            Log.e(TAG, "getAchievementsByType: Exception", e);
        }

        return achievements;
    }

    /**
     * Gets all unnotified achievements for a user.
     *
     * @param userId User ID
     * @return List of unnotified achievements
     */
    @NonNull
    public List<Achievement> getUnnotifiedAchievements(long userId) {
        Log.d(TAG, "getUnnotifiedAchievements: user_id=" + userId);

        List<Achievement> achievements = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_ACHIEVEMENTS,
            null,
            "user_id = ? AND is_notified = 0",
            new String[]{String.valueOf(userId)},
            null, null,
            "achieved_at DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                achievements.add(mapCursorToAchievement(cursor));
            }
            Log.i(TAG, "getUnnotifiedAchievements: Found " + achievements.size() +
                    " unnotified achievements");
        } catch (Exception e) {
            Log.e(TAG, "getUnnotifiedAchievements: Exception", e);
        }

        return achievements;
    }

    /**
     * Checks if a user has an achievement of a specific type (for duplicate prevention).
     *
     * @param userId User ID
     * @param achievementType Achievement type to check
     * @return true if achievement exists, false otherwise
     */
    public boolean hasAchievementType(long userId, @NonNull String achievementType) {
        Log.d(TAG, "hasAchievementType: user_id=" + userId + ", type=" + achievementType);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_ACHIEVEMENTS,
            new String[]{"achievement_id"},
            "user_id = ? AND achievement_type = ?",
            new String[]{String.valueOf(userId), achievementType},
            null, null, null,
            "1"
        )) {
            boolean exists = cursor != null && cursor.getCount() > 0;
            Log.d(TAG, "hasAchievementType: " + achievementType + " exists=" + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "hasAchievementType: Exception", e);
            return false;
        }
    }

    /**
     * Updates the is_notified flag for an achievement.
     *
     * @param achievementId Achievement ID
     * @param isNotified New notification status
     * @return Number of rows updated (1 if successful, 0 if not found)
     */
    public int updateIsNotified(long achievementId, boolean isNotified) {
        Log.d(TAG, "updateIsNotified: achievement_id=" + achievementId + ", isNotified=" + isNotified);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("is_notified", isNotified ? 1 : 0);

        try {
            int rowsUpdated = db.update(
                WeighToGoDBHelper.TABLE_ACHIEVEMENTS,
                values,
                "achievement_id = ?",
                new String[]{String.valueOf(achievementId)}
            );

            if (rowsUpdated > 0) {
                Log.i(TAG, "updateIsNotified: Successfully updated achievement_id=" + achievementId);
            }
            return rowsUpdated;
        } catch (Exception e) {
            Log.e(TAG, "updateIsNotified: Exception", e);
            return 0;
        }
    }

    /**
     * Gets the most recent achievement for a user.
     *
     * @param userId User ID
     * @return Latest achievement, or null if no achievements exist
     */
    @Nullable
    public Achievement getLatestAchievement(long userId) {
        Log.d(TAG, "getLatestAchievement: user_id=" + userId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_ACHIEVEMENTS,
            null,
            "user_id = ?",
            new String[]{String.valueOf(userId)},
            null, null,
            "achieved_at DESC",
            "1"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToAchievement(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getLatestAchievement: Exception", e);
        }
        return null;
    }

    /**
     * Maps a database cursor row to an Achievement object.
     *
     * @param cursor Cursor positioned at a row
     * @return Achievement object
     */
    private Achievement mapCursorToAchievement(@NonNull Cursor cursor) {
        Achievement achievement = new Achievement();

        achievement.setAchievementId(cursor.getLong(cursor.getColumnIndexOrThrow("achievement_id")));
        achievement.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow("user_id")));
        achievement.setAchievementType(cursor.getString(cursor.getColumnIndexOrThrow("achievement_type")));
        achievement.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        achievement.setNotified(cursor.getInt(cursor.getColumnIndexOrThrow("is_notified")) == 1);

        // Parse achieved_at timestamp
        String achievedAtStr = cursor.getString(cursor.getColumnIndexOrThrow("achieved_at"));
        achievement.setAchievedAt(LocalDateTime.parse(achievedAtStr, ISO_DATETIME_FORMATTER));

        // Optional fields
        int goalIdIndex = cursor.getColumnIndexOrThrow("goal_id");
        if (!cursor.isNull(goalIdIndex)) {
            achievement.setGoalId(cursor.getLong(goalIdIndex));
        }

        int descriptionIndex = cursor.getColumnIndexOrThrow("description");
        if (!cursor.isNull(descriptionIndex)) {
            achievement.setDescription(cursor.getString(descriptionIndex));
        }

        int valueIndex = cursor.getColumnIndexOrThrow("value");
        if (!cursor.isNull(valueIndex)) {
            achievement.setValue(cursor.getDouble(valueIndex));
        }

        return achievement;
    }
}
