package com.example.weightogo.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weightogo.models.GoalWeight;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Goal Weight operations.
 * Handles all database CRUD operations for goal_weights table.
 *
 * <p><strong>Database Lifecycle:</strong> This DAO uses a singleton WeighToGoDBHelper instance.
 * The helper manages the database connection lifecycle, so individual methods do NOT close
 * the SQLiteDatabase instance obtained via getReadableDatabase() or getWritableDatabase().
 * The singleton pattern ensures efficient connection pooling and prevents resource leaks.</p>
 *
 * <p><strong>Business Rules:</strong> Only one goal should be active per user at a time.
 * Use deactivateAllGoalsForUser() before setting a new active goal to maintain this constraint.</p>
 *
 * <p><strong>Soft Deactivation:</strong> Uses soft deactivation (is_active flag) instead of deletion
 * to preserve goal history and support analytics.</p>
 */
public class GoalWeightDAO {

    private static final String TAG = "GoalWeightDAO";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WeighToGoDBHelper dbHelper;

    public GoalWeightDAO(@NonNull WeighToGoDBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a new goal weight.
     */
    public long insertGoal(@NonNull GoalWeight goal) {
        Log.d(TAG, "insertGoal: Inserting goal for user_id=" + goal.getUserId());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", goal.getUserId());
        values.put("goal_weight", goal.getGoalWeight());
        values.put("goal_unit", goal.getGoalUnit());
        values.put("start_weight", goal.getStartWeight());
        values.put("created_at", goal.getCreatedAt().format(ISO_DATETIME_FORMATTER));
        values.put("updated_at", goal.getUpdatedAt().format(ISO_DATETIME_FORMATTER));
        values.put("is_active", goal.isActive() ? 1 : 0);
        values.put("is_achieved", goal.isAchieved() ? 1 : 0);

        if (goal.getTargetDate() != null) {
            values.put("target_date", goal.getTargetDate().format(ISO_DATE_FORMATTER));
        }
        if (goal.getAchievedDate() != null) {
            values.put("achieved_date", goal.getAchievedDate().format(ISO_DATE_FORMATTER));
        }

        try {
            long goalId = db.insert(WeighToGoDBHelper.TABLE_GOAL_WEIGHTS, null, values);
            if (goalId > 0) {
                Log.i(TAG, "insertGoal: Successfully inserted goal_id=" + goalId);
            }
            return goalId;
        } catch (Exception e) {
            Log.e(TAG, "insertGoal: Exception", e);
            return -1;
        }
    }

    /**
     * Gets the active goal for a user (only one goal should be active at a time).
     */
    @Nullable
    public GoalWeight getActiveGoal(long userId) {
        Log.d(TAG, "getActiveGoal: user_id=" + userId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_GOAL_WEIGHTS,
            null,
            "user_id = ? AND is_active = 1",
            new String[]{String.valueOf(userId)},
            null, null,
            "created_at DESC",
            "1"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToGoal(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getActiveGoal: Exception", e);
        }
        return null;
    }

    /**
     * Gets a specific goal by its ID.
     */
    @Nullable
    public GoalWeight getGoalById(long goalId) {
        Log.d(TAG, "getGoalById: goal_id=" + goalId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_GOAL_WEIGHTS,
            null,
            "goal_id = ?",
            new String[]{String.valueOf(goalId)},
            null, null, null,
            "1"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToGoal(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getGoalById: Exception", e);
        }
        return null;
    }

    /**
     * Gets all goals for a user (both active and inactive).
     */
    @NonNull
    public List<GoalWeight> getGoalHistory(long userId) {
        Log.d(TAG, "getGoalHistory: user_id=" + userId);

        List<GoalWeight> goals = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_GOAL_WEIGHTS,
            null,
            "user_id = ?",
            new String[]{String.valueOf(userId)},
            null, null,
            "created_at DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                goals.add(mapCursorToGoal(cursor));
            }
            Log.i(TAG, "getGoalHistory: Found " + goals.size() + " goals");
        } catch (Exception e) {
            Log.e(TAG, "getGoalHistory: Exception", e);
        }

        return goals;
    }

    /**
     * Updates an existing goal.
     *
     * <p><strong>Return Value Semantics:</strong></p>
     * <ul>
     *   <li>Returns 1 if goal exists and was successfully updated</li>
     *   <li>Returns 0 if goal doesn't exist (goal_id not found)</li>
     *   <li>Returns 0 on database error (exception logged)</li>
     * </ul>
     * <p>Callers should check the return value to distinguish between these cases.</p>
     */
    public int updateGoal(@NonNull GoalWeight goal) {
        Log.d(TAG, "updateGoal: goal_id=" + goal.getGoalId());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("goal_weight", goal.getGoalWeight());
        values.put("goal_unit", goal.getGoalUnit());
        values.put("start_weight", goal.getStartWeight());
        values.put("updated_at", LocalDateTime.now().format(ISO_DATETIME_FORMATTER));
        values.put("is_active", goal.isActive() ? 1 : 0);
        values.put("is_achieved", goal.isAchieved() ? 1 : 0);

        // Allow explicit NULL for optional date fields
        if (goal.getTargetDate() != null) {
            values.put("target_date", goal.getTargetDate().format(ISO_DATE_FORMATTER));
        } else {
            values.putNull("target_date");
        }
        if (goal.getAchievedDate() != null) {
            values.put("achieved_date", goal.getAchievedDate().format(ISO_DATE_FORMATTER));
        } else {
            values.putNull("achieved_date");
        }

        try {
            int rows = db.update(
                WeighToGoDBHelper.TABLE_GOAL_WEIGHTS,
                values,
                "goal_id = ?",
                new String[]{String.valueOf(goal.getGoalId())}
            );
            Log.i(TAG, "updateGoal: Updated " + rows + " rows");
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "updateGoal: Exception", e);
            return 0;
        }
    }

    /**
     * Deactivates a goal (sets is_active = 0).
     * Useful when user wants to set a new goal.
     */
    public int deactivateGoal(long goalId) {
        Log.d(TAG, "deactivateGoal: goal_id=" + goalId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("is_active", 0);
        values.put("updated_at", LocalDateTime.now().format(ISO_DATETIME_FORMATTER));

        try {
            int rows = db.update(
                WeighToGoDBHelper.TABLE_GOAL_WEIGHTS,
                values,
                "goal_id = ?",
                new String[]{String.valueOf(goalId)}
            );
            Log.i(TAG, "deactivateGoal: Deactivated " + rows + " rows");
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "deactivateGoal: Exception", e);
            return 0;
        }
    }

    /**
     * Deactivates all goals for a user.
     * Useful before setting a new active goal.
     */
    public int deactivateAllGoalsForUser(long userId) {
        Log.d(TAG, "deactivateAllGoalsForUser: user_id=" + userId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("is_active", 0);
        values.put("updated_at", LocalDateTime.now().format(ISO_DATETIME_FORMATTER));

        try {
            int rows = db.update(
                WeighToGoDBHelper.TABLE_GOAL_WEIGHTS,
                values,
                "user_id = ? AND is_active = 1",
                new String[]{String.valueOf(userId)}
            );
            Log.i(TAG, "deactivateAllGoalsForUser: Deactivated " + rows + " goals");
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "deactivateAllGoalsForUser: Exception", e);
            return 0;
        }
    }

    /**
     * Sets a new active goal for a user, automatically deactivating any existing active goals.
     *
     * <p><strong>Transaction Support:</strong> This operation uses a database transaction to ensure
     * atomicity. Either both the deactivation and insertion succeed, or neither does (rollback).</p>
     *
     * <p>This prevents race conditions where a user might temporarily have multiple active goals
     * or no active goal due to partial operation failure.</p>
     *
     * @param newGoal The new goal to set as active (must have isActive=true)
     * @return goal_id of the newly inserted goal, or -1 if transaction failed
     */
    public long setNewActiveGoal(@NonNull GoalWeight newGoal) {
        Log.d(TAG, "setNewActiveGoal: Setting new goal for user_id=" + newGoal.getUserId());

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();

        try {
            // Step 1: Deactivate all existing goals for this user
            int deactivated = deactivateAllGoalsForUser(newGoal.getUserId());
            Log.d(TAG, "setNewActiveGoal: Deactivated " + deactivated + " existing goals");

            // Step 2: Insert the new goal
            long goalId = insertGoal(newGoal);

            if (goalId > 0) {
                db.setTransactionSuccessful();
                Log.i(TAG, "setNewActiveGoal: Transaction successful, new goal_id=" + goalId);
                return goalId;
            } else {
                Log.e(TAG, "setNewActiveGoal: Insert failed, transaction will rollback");
                return -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "setNewActiveGoal: Exception during transaction, rolling back", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Maps cursor to GoalWeight object.
     */
    private GoalWeight mapCursorToGoal(@NonNull Cursor cursor) {
        GoalWeight goal = new GoalWeight();

        goal.setGoalId(cursor.getLong(cursor.getColumnIndexOrThrow("goal_id")));
        goal.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow("user_id")));
        goal.setGoalWeight(cursor.getDouble(cursor.getColumnIndexOrThrow("goal_weight")));
        goal.setGoalUnit(cursor.getString(cursor.getColumnIndexOrThrow("goal_unit")));
        goal.setStartWeight(cursor.getDouble(cursor.getColumnIndexOrThrow("start_weight")));

        String createdStr = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
        goal.setCreatedAt(LocalDateTime.parse(createdStr, ISO_DATETIME_FORMATTER));

        String updatedStr = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"));
        goal.setUpdatedAt(LocalDateTime.parse(updatedStr, ISO_DATETIME_FORMATTER));

        goal.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1);
        goal.setAchieved(cursor.getInt(cursor.getColumnIndexOrThrow("is_achieved")) == 1);

        int targetDateIndex = cursor.getColumnIndexOrThrow("target_date");
        if (!cursor.isNull(targetDateIndex)) {
            String targetStr = cursor.getString(targetDateIndex);
            goal.setTargetDate(LocalDate.parse(targetStr, ISO_DATE_FORMATTER));
        }

        int achievedDateIndex = cursor.getColumnIndexOrThrow("achieved_date");
        if (!cursor.isNull(achievedDateIndex)) {
            String achievedStr = cursor.getString(achievedDateIndex);
            goal.setAchievedDate(LocalDate.parse(achievedStr, ISO_DATE_FORMATTER));
        }

        return goal;
    }
}
