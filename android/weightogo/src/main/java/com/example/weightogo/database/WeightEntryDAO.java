package com.example.weightogo.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weightogo.models.WeightEntry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Weight Entry operations.
 * Handles all database CRUD operations for daily_weights table.
 *
 * <p><strong>Naming Note:</strong> This class is named "WeightEntryDAO" and works with "WeightEntry"
 * model objects, but the underlying database table is named "daily_weights" per the schema specification.
 * This naming difference is intentional - Java uses "WeightEntry" for clarity, while SQL uses "daily_weights"
 * to reflect that each entry represents a single day's weight measurement.</p>
 *
 * <p><strong>Database Lifecycle:</strong> This DAO uses a singleton WeighToGoDBHelper instance.
 * The helper manages the database connection lifecycle, so individual methods do NOT close
 * the SQLiteDatabase instance obtained via getReadableDatabase() or getWritableDatabase().
 * The singleton pattern ensures efficient connection pooling and prevents resource leaks.</p>
 *
 * <p><strong>Soft Delete:</strong> Uses soft delete (is_deleted flag) instead of hard delete
 * to preserve data and support undo functionality.</p>
 */
public class WeightEntryDAO {

    private static final String TAG = "WeightEntryDAO";
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WeighToGoDBHelper dbHelper;

    public WeightEntryDAO(@NonNull WeighToGoDBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a new weight entry.
     */
    public long insertWeightEntry(@NonNull WeightEntry entry) {
        Log.d(TAG, "insertWeightEntry: Inserting entry for user_id=" + entry.getUserId());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("user_id", entry.getUserId());
        values.put("weight_value", entry.getWeightValue());
        values.put("weight_unit", entry.getWeightUnit());
        values.put("weight_date", entry.getWeightDate().format(ISO_DATE_FORMATTER));
        values.put("created_at", entry.getCreatedAt().format(ISO_DATETIME_FORMATTER));
        values.put("updated_at", entry.getUpdatedAt().format(ISO_DATETIME_FORMATTER));
        values.put("is_deleted", entry.isDeleted() ? 1 : 0);

        if (entry.getNotes() != null) {
            values.put("notes", entry.getNotes());
        }

        try {
            long weightId = db.insert(WeighToGoDBHelper.TABLE_DAILY_WEIGHTS, null, values);
            if (weightId > 0) {
                Log.i(TAG, "insertWeightEntry: Successfully inserted weight_id=" + weightId);
            }
            return weightId;
        } catch (Exception e) {
            Log.e(TAG, "insertWeightEntry: Exception", e);
            return -1;
        }
    }

    /**
     * Gets all non-deleted weight entries for a user, ordered by date descending.
     */
    @NonNull
    public List<WeightEntry> getWeightEntriesForUser(long userId) {
        Log.d(TAG, "getWeightEntriesForUser: user_id=" + userId);

        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
            null,
            "user_id = ? AND is_deleted = 0",
            new String[]{String.valueOf(userId)},
            null, null,
            "weight_date DESC"
        )) {
            while (cursor != null && cursor.moveToNext()) {
                entries.add(mapCursorToEntry(cursor));
            }
            Log.i(TAG, "getWeightEntriesForUser: Found " + entries.size() + " entries");
        } catch (Exception e) {
            Log.e(TAG, "getWeightEntriesForUser: Exception", e);
        }

        return entries;
    }

    /**
     * Gets recent weight entries for streak calculation (optimized).
     * Only fetches the most recent entries needed for streak detection.
     * Prevents N+1 query problem by limiting data retrieval.
     *
     * @param userId user ID
     * @param limit  maximum number of entries to retrieve (e.g., 30 for STREAK_30)
     * @return list of recent weight entries, sorted by date descending
     */
    public List<WeightEntry> getRecentWeightEntriesForUser(long userId, int limit) {
        Log.d(TAG, "getRecentWeightEntriesForUser: user_id=" + userId + ", limit=" + limit);

        List<WeightEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
            null,
            "user_id = ? AND is_deleted = 0",
            new String[]{String.valueOf(userId)},
            null, null,
            "weight_date DESC",
            String.valueOf(limit)  // LIMIT clause for optimization
        )) {
            while (cursor != null && cursor.moveToNext()) {
                entries.add(mapCursorToEntry(cursor));
            }
            Log.i(TAG, "getRecentWeightEntriesForUser: Found " + entries.size() + " recent entries");
        } catch (Exception e) {
            Log.e(TAG, "getRecentWeightEntriesForUser: Exception", e);
        }

        return entries;
    }

    /**
     * Gets a weight entry by ID.
     */
    @Nullable
    public WeightEntry getWeightEntryById(long weightId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
            null,
            "weight_id = ?",
            new String[]{String.valueOf(weightId)},
            null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToEntry(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getWeightEntryById: Exception", e);
        }
        return null;
    }

    /**
     * Gets a weight entry for a specific user and date.
     * Used by DailyReminderWorker to check if user logged weight today.
     *
     * @param userId user ID
     * @param date   the date to check
     * @return WeightEntry for the date, or null if no entry exists
     */
    @Nullable
    public WeightEntry getWeightEntryForDate(long userId, java.time.LocalDate date) {
        Log.d(TAG, "getWeightEntryForDate: user_id=" + userId + ", date=" + date);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
            null,
            "user_id = ? AND weight_date = ? AND is_deleted = 0",
            new String[]{String.valueOf(userId), date.toString()},
            null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                WeightEntry entry = mapCursorToEntry(cursor);
                Log.d(TAG, "getWeightEntryForDate: Found entry weight_id=" + entry.getWeightId());
                return entry;
            }
        } catch (Exception e) {
            Log.e(TAG, "getWeightEntryForDate: Exception", e);
        }

        Log.d(TAG, "getWeightEntryForDate: No entry found for date");
        return null;
    }

    /**
     * Gets the minimum weight value for a user (optimized for NEW_LOW achievement).
     * Uses SQL MIN() instead of fetching all entries.
     *
     * @param userId user ID
     * @return minimum weight value, or null if no entries exist
     */
    @Nullable
    public Double getMinWeightForUser(long userId) {
        Log.d(TAG, "getMinWeightForUser: user_id=" + userId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.rawQuery(
            "SELECT MIN(weight_value) as min_weight FROM " + WeighToGoDBHelper.TABLE_DAILY_WEIGHTS +
            " WHERE user_id = ? AND is_deleted = 0",
            new String[]{String.valueOf(userId)}
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex("min_weight");
                if (columnIndex != -1 && !cursor.isNull(columnIndex)) {
                    double minWeight = cursor.getDouble(columnIndex);
                    Log.i(TAG, "getMinWeightForUser: Found min weight = " + minWeight);
                    return minWeight;
                }
            }
            Log.i(TAG, "getMinWeightForUser: No entries found");
        } catch (Exception e) {
            Log.e(TAG, "getMinWeightForUser: Exception", e);
        }

        return null;
    }

    /**
     * Gets the most recent weight entry for a user.
     */
    @Nullable
    public WeightEntry getLatestWeightEntry(long userId) {
        Log.d(TAG, "getLatestWeightEntry: user_id=" + userId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
            null,
            "user_id = ? AND is_deleted = 0",
            new String[]{String.valueOf(userId)},
            null, null,
            "weight_date DESC, created_at DESC",
            "1"
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                return mapCursorToEntry(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getLatestWeightEntry: Exception", e);
        }
        return null;
    }

    /**
     * Updates an existing weight entry.
     *
     * <p><strong>Return Value Semantics:</strong></p>
     * <ul>
     *   <li>Returns 1 if entry exists and was successfully updated</li>
     *   <li>Returns 0 if entry doesn't exist (weight_id not found)</li>
     *   <li>Returns 0 on database error (exception logged)</li>
     * </ul>
     * <p>Callers should check the return value to distinguish between these cases.</p>
     */
    public int updateWeightEntry(@NonNull WeightEntry entry) {
        Log.d(TAG, "updateWeightEntry: weight_id=" + entry.getWeightId());

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("weight_value", entry.getWeightValue());
        values.put("weight_unit", entry.getWeightUnit());
        values.put("weight_date", entry.getWeightDate().format(ISO_DATE_FORMATTER));
        values.put("updated_at", LocalDateTime.now().format(ISO_DATETIME_FORMATTER));

        // Allow explicit NULL for notes
        if (entry.getNotes() != null) {
            values.put("notes", entry.getNotes());
        } else {
            values.putNull("notes");
        }

        try {
            int rows = db.update(
                WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
                values,
                "weight_id = ?",
                new String[]{String.valueOf(entry.getWeightId())}
            );
            Log.i(TAG, "updateWeightEntry: Updated " + rows + " rows");
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "updateWeightEntry: Exception", e);
            return 0;
        }
    }

    /**
     * Soft deletes a weight entry (sets is_deleted = 1).
     */
    public int deleteWeightEntry(long weightId) {
        Log.d(TAG, "deleteWeightEntry: weight_id=" + weightId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("is_deleted", 1);
        values.put("updated_at", LocalDateTime.now().format(ISO_DATETIME_FORMATTER));

        try {
            int rows = db.update(
                WeighToGoDBHelper.TABLE_DAILY_WEIGHTS,
                values,
                "weight_id = ?",
                new String[]{String.valueOf(weightId)}
            );
            Log.i(TAG, "deleteWeightEntry: Soft deleted " + rows + " rows");
            return rows;
        } catch (Exception e) {
            Log.e(TAG, "deleteWeightEntry: Exception", e);
            return 0;
        }
    }

    /**
     * Maps cursor to WeightEntry object.
     */
    private WeightEntry mapCursorToEntry(@NonNull Cursor cursor) {
        WeightEntry entry = new WeightEntry();

        entry.setWeightId(cursor.getLong(cursor.getColumnIndexOrThrow("weight_id")));
        entry.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow("user_id")));
        entry.setWeightValue(cursor.getDouble(cursor.getColumnIndexOrThrow("weight_value")));
        entry.setWeightUnit(cursor.getString(cursor.getColumnIndexOrThrow("weight_unit")));

        String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("weight_date"));
        entry.setWeightDate(LocalDate.parse(dateStr, ISO_DATE_FORMATTER));

        String createdStr = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
        entry.setCreatedAt(LocalDateTime.parse(createdStr, ISO_DATETIME_FORMATTER));

        String updatedStr = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"));
        entry.setUpdatedAt(LocalDateTime.parse(updatedStr, ISO_DATETIME_FORMATTER));

        entry.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow("is_deleted")) == 1);

        int notesIndex = cursor.getColumnIndexOrThrow("notes");
        if (!cursor.isNull(notesIndex)) {
            entry.setNotes(cursor.getString(notesIndex));
        }

        return entry;
    }
}
