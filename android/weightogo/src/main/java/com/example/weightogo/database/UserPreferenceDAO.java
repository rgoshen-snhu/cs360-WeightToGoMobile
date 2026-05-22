package com.example.weightogo.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.weightogo.models.UserPreference;
import com.example.weightogo.utils.DateTimeConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for user_preferences table.
 *
 * <p>Provides generic key-value preference storage with UPSERT pattern.
 * Preserves created_at timestamp on updates using transactional INSERT/UPDATE.</p>
 *
 * <p><strong>Database Lifecycle:</strong> This DAO uses a singleton WeighToGoDBHelper instance.
 * The helper manages the database connection lifecycle, so individual methods do NOT close
 * the SQLiteDatabase instance obtained via getReadableDatabase() or getWritableDatabase().
 * The singleton pattern ensures efficient connection pooling and prevents resource leaks.</p>
 *
 * <p>This class follows the DAO pattern used by UserDAO, WeightEntryDAO, and GoalWeightDAO.</p>
 */
public class UserPreferenceDAO {

    private static final String TAG = "UserPreferenceDAO";

    // Query limits
    private static final String LIMIT_ONE = "1";

    // Preference keys
    public static final String KEY_WEIGHT_UNIT = "weight_unit";

    // Valid weight units
    private static final String UNIT_LBS = "lbs";
    private static final String UNIT_KG = "kg";

    private final WeighToGoDBHelper dbHelper;

    /**
     * Constructor.
     *
     * @param dbHelper the database helper instance
     */
    public UserPreferenceDAO(@NonNull WeighToGoDBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Gets a preference value for a user, returning default if not found.
     *
     * @param userId the user ID
     * @param key the preference key
     * @param defaultValue the default value to return if key not found
     * @return the preference value, or defaultValue if not found
     */
    @NonNull
    public String getPreference(long userId, @NonNull String key, @NonNull String defaultValue) {
        Log.d(TAG, "getPreference: user_id=" + userId + ", key=" + key);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(
                WeighToGoDBHelper.TABLE_USER_PREFERENCES,
                new String[]{"pref_value"},
                "user_id = ? AND pref_key = ?",
                new String[]{String.valueOf(userId), key},
                null, null, null, LIMIT_ONE
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                Log.i(TAG, "getPreference: Found value for key=" + key);
                return value;
            }
        } catch (Exception e) {
            Log.e(TAG, "getPreference: Exception", e);
        }

        Log.d(TAG, "getPreference: Key not found, returning default");
        return defaultValue;
    }

    /**
     * Sets a preference value for a user (UPSERT).
     * Preserves created_at timestamp on updates by using transactional INSERT/UPDATE paths.
     * Uses a transaction to ensure atomicity of the check-and-upsert operation.
     *
     * @param userId the user ID
     * @param key the preference key
     * @param value the preference value
     * @return true if successful, false otherwise
     */
    public boolean setPreference(long userId, @NonNull String key, @NonNull String value) {
        Log.d(TAG, "setPreference: user_id=" + userId + ", key=" + key);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;

        // Use transaction for atomicity
        db.beginTransaction();
        try {
            String now = DateTimeConverter.toTimestamp(LocalDateTime.now());

            // Check if preference already exists
            cursor = db.query(
                    WeighToGoDBHelper.TABLE_USER_PREFERENCES,
                    new String[]{"pref_value"},
                    "user_id = ? AND pref_key = ?",
                    new String[]{String.valueOf(userId), key},
                    null, null, null, LIMIT_ONE
            );

            boolean exists = cursor != null && cursor.moveToFirst();
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }

            if (exists) {
                // UPDATE path - preserve created_at
                ContentValues updateValues = new ContentValues();
                updateValues.put("pref_value", value);
                updateValues.put("updated_at", now);

                int rowsAffected = db.update(
                        WeighToGoDBHelper.TABLE_USER_PREFERENCES,
                        updateValues,
                        "user_id = ? AND pref_key = ?",
                        new String[]{String.valueOf(userId), key}
                );

                if (rowsAffected > 0) {
                    db.setTransactionSuccessful();
                    Log.i(TAG, "setPreference: Updated existing key=" + key);
                    return true;
                }
            } else {
                // INSERT path - new preference
                ContentValues insertValues = new ContentValues();
                insertValues.put("user_id", userId);
                insertValues.put("pref_key", key);
                insertValues.put("pref_value", value);
                insertValues.put("created_at", now);
                insertValues.put("updated_at", now);

                long result = db.insert(
                        WeighToGoDBHelper.TABLE_USER_PREFERENCES,
                        null,
                        insertValues
                );

                if (result > 0) {
                    db.setTransactionSuccessful();
                    Log.i(TAG, "setPreference: Inserted new key=" + key);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "setPreference: Exception", e);
        } finally {
            db.endTransaction();
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Gets all preferences for a user (package-private for testing).
     * Used by tests to verify UPSERT behavior (no duplicate keys).
     *
     * @param userId the user ID
     * @return list of all preferences for the user (never null)
     */
    @NonNull
    List<UserPreference> getAllPreferences(long userId) {
        Log.d(TAG, "getAllPreferences: user_id=" + userId);

        List<UserPreference> preferences = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
                WeighToGoDBHelper.TABLE_USER_PREFERENCES,
                null,
                "user_id = ?",
                new String[]{String.valueOf(userId)},
                null, null, null
        )) {
            while (cursor != null && cursor.moveToNext()) {
                preferences.add(mapCursorToUserPreference(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllPreferences: Exception", e);
        }

        return preferences;
    }

    /**
     * Maps a database cursor to a UserPreference object.
     *
     * @param cursor the cursor positioned at a row
     * @return UserPreference object with data from cursor
     */
    private UserPreference mapCursorToUserPreference(@NonNull Cursor cursor) {
        UserPreference pref = new UserPreference();
        pref.setPreferenceId(cursor.getLong(cursor.getColumnIndexOrThrow("preference_id")));
        pref.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow("user_id")));
        pref.setPrefKey(cursor.getString(cursor.getColumnIndexOrThrow("pref_key")));
        pref.setPrefValue(cursor.getString(cursor.getColumnIndexOrThrow("pref_value")));

        String createdStr = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
        String updatedStr = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"));
        pref.setCreatedAt(DateTimeConverter.fromTimestamp(createdStr));
        pref.setUpdatedAt(DateTimeConverter.fromTimestamp(updatedStr));

        return pref;
    }

    /**
     * Gets the weight unit preference (defaults to "lbs").
     *
     * @param userId the user ID
     * @return the weight unit ("lbs" or "kg")
     */
    @NonNull
    public String getWeightUnit(long userId) {
        Log.d(TAG, "getWeightUnit: user_id=" + userId);
        return getPreference(userId, KEY_WEIGHT_UNIT, UNIT_LBS);
    }

    /**
     * Sets the weight unit preference with validation.
     * Accepts only "lbs" or "kg" (case-sensitive).
     *
     * @param userId the user ID
     * @param unit the weight unit ("lbs" or "kg")
     * @return true if successful, false if unit is invalid
     */
    public boolean setWeightUnit(long userId, @NonNull String unit) {
        Log.d(TAG, "setWeightUnit: user_id=" + userId + ", unit=" + unit);

        // Validate input (case-sensitive)
        if (!UNIT_LBS.equals(unit) && !UNIT_KG.equals(unit)) {
            Log.w(TAG, "setWeightUnit: Invalid unit '" + unit + "' (must be 'lbs' or 'kg')");
            return false;
        }

        return setPreference(userId, KEY_WEIGHT_UNIT, unit);
    }
}
