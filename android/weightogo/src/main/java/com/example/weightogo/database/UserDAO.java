package com.example.weightogo.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weightogo.models.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Access Object for User operations.
 * Handles all database CRUD operations for users table.
 *
 * <p><strong>Database Lifecycle:</strong> This DAO uses a singleton WeighToGoDBHelper instance.
 * The helper manages the database connection lifecycle, so individual methods do NOT close
 * the SQLiteDatabase instance obtained via getReadableDatabase() or getWritableDatabase().
 * The singleton pattern ensures efficient connection pooling and prevents resource leaks.</p>
 *
 * <p><strong>Security:</strong> NEVER log passwordHash or salt values.
 * All database operations use parameterized queries to prevent SQL injection.</p>
 */
public class UserDAO {

    private static final String TAG = "UserDAO";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final WeighToGoDBHelper dbHelper;

    /**
     * Constructor.
     *
     * @param dbHelper Database helper instance
     */
    public UserDAO(@NonNull WeighToGoDBHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Inserts a new user into the database.
     *
     * @param user User object to insert (user_id will be ignored, auto-generated)
     * @return user_id of inserted user
     * @throws DuplicateUsernameException if username already exists
     * @throws DatabaseException if database operation fails
     */
    public long insertUser(@NonNull User user) throws DuplicateUsernameException, DatabaseException {
        Log.d(TAG, "insertUser: Inserting user with username=" + user.getUsername());

        // Check for duplicate username first
        if (usernameExists(user.getUsername())) {
            String msg = "Username '" + user.getUsername() + "' already exists";
            Log.e(TAG, "insertUser: " + msg);
            throw new DuplicateUsernameException(msg);
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("username", user.getUsername());
        values.put("password_hash", user.getPasswordHash());  // Already hashed by caller
        values.put("salt", user.getSalt());
        values.put("password_algorithm", user.getPasswordAlgorithm());  // Phase 8.6: bcrypt migration
        values.put("created_at", user.getCreatedAt().format(ISO_FORMATTER));
        values.put("updated_at", LocalDateTime.now().format(ISO_FORMATTER));
        values.put("is_active", user.isActive() ? 1 : 0);

        // Optional fields
        if (user.getEmail() != null) {
            values.put("email", user.getEmail());
        }
        if (user.getPhoneNumber() != null) {
            values.put("phone_number", user.getPhoneNumber());
        }
        if (user.getDisplayName() != null) {
            values.put("display_name", user.getDisplayName());
        }
        if (user.getLastLogin() != null) {
            values.put("last_login", user.getLastLogin().format(ISO_FORMATTER));
        }

        try {
            long userId = db.insert(WeighToGoDBHelper.TABLE_USERS, null, values);

            if (userId > 0) {
                Log.i(TAG, "insertUser: Successfully inserted user with user_id=" + userId);
                return userId;
            } else {
                throw new DatabaseException("Insert failed - database returned -1");
            }

        } catch (SQLiteConstraintException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                String msg = "Username '" + user.getUsername() + "' already exists";
                Log.e(TAG, "insertUser: " + msg, e);
                throw new DuplicateUsernameException(msg, e);
            }
            Log.e(TAG, "insertUser: Constraint violation", e);
            throw new DatabaseException("Constraint violation", e);
        } catch (SQLException e) {
            Log.e(TAG, "insertUser: Database error", e);
            throw new DatabaseException("Database error during insert", e);
        }
    }

    /**
     * Retrieves a user by their user_id.
     *
     * @param userId The user_id to search for
     * @return User object if found, null otherwise
     */
    @Nullable
    public User getUserById(long userId) {
        Log.d(TAG, "getUserById: Querying user_id=" + userId);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_USERS,
            null,  // all columns
            "user_id = ?",
            new String[]{String.valueOf(userId)},
            null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                User user = mapCursorToUser(cursor);
                Log.i(TAG, "getUserById: Found user with username=" + user.getUsername());
                return user;
            } else {
                Log.w(TAG, "getUserById: No user found with user_id=" + userId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getUserById: Exception querying user", e);
            return null;
        }
    }

    /**
     * Maps a database cursor to a User object.
     *
     * @param cursor Cursor positioned at a valid row
     * @return User object populated from cursor
     */
    private User mapCursorToUser(@NonNull Cursor cursor) {
        User user = new User();

        user.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow("user_id")));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow("username")));
        user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow("password_hash")));
        user.setSalt(cursor.getString(cursor.getColumnIndexOrThrow("salt")));
        user.setPasswordAlgorithm(cursor.getString(cursor.getColumnIndexOrThrow("password_algorithm")));  // Phase 8.6

        String createdAtStr = cursor.getString(cursor.getColumnIndexOrThrow("created_at"));
        user.setCreatedAt(LocalDateTime.parse(createdAtStr, ISO_FORMATTER));

        String updatedAtStr = cursor.getString(cursor.getColumnIndexOrThrow("updated_at"));
        user.setUpdatedAt(LocalDateTime.parse(updatedAtStr, ISO_FORMATTER));

        user.setActive(cursor.getInt(cursor.getColumnIndexOrThrow("is_active")) == 1);

        // Optional fields - check for null
        int emailIndex = cursor.getColumnIndexOrThrow("email");
        if (!cursor.isNull(emailIndex)) {
            user.setEmail(cursor.getString(emailIndex));
        }

        int phoneIndex = cursor.getColumnIndexOrThrow("phone_number");
        if (!cursor.isNull(phoneIndex)) {
            user.setPhoneNumber(cursor.getString(phoneIndex));
        }

        int displayNameIndex = cursor.getColumnIndexOrThrow("display_name");
        if (!cursor.isNull(displayNameIndex)) {
            user.setDisplayName(cursor.getString(displayNameIndex));
        }

        int lastLoginIndex = cursor.getColumnIndexOrThrow("last_login");
        if (!cursor.isNull(lastLoginIndex)) {
            String lastLoginStr = cursor.getString(lastLoginIndex);
            user.setLastLogin(LocalDateTime.parse(lastLoginStr, ISO_FORMATTER));
        }

        return user;
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username to search for
     * @return User object if found, null otherwise
     */
    @Nullable
    public User getUserByUsername(@NonNull String username) {
        Log.d(TAG, "getUserByUsername: Querying username=" + username);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_USERS,
            null,  // all columns
            "username = ?",
            new String[]{username},
            null, null, null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                User user = mapCursorToUser(cursor);
                Log.i(TAG, "getUserByUsername: Found user with user_id=" + user.getUserId());
                return user;
            } else {
                Log.w(TAG, "getUserByUsername: No user found with username=" + username);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "getUserByUsername: Exception querying user", e);
            return null;
        }
    }

    /**
     * Checks if a username already exists in the database.
     * Useful for registration validation.
     *
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    public boolean usernameExists(@NonNull String username) {
        Log.d(TAG, "usernameExists: Checking username=" + username);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        try (Cursor cursor = db.query(
            WeighToGoDBHelper.TABLE_USERS,
            new String[]{"user_id"},  // Only need to check existence
            "username = ?",
            new String[]{username},
            null, null, null
        )) {
            boolean exists = cursor != null && cursor.getCount() > 0;
            Log.d(TAG, "usernameExists: Username '" + username + "' exists=" + exists);
            return exists;
        } catch (Exception e) {
            Log.e(TAG, "usernameExists: Exception checking username", e);
            return false;
        }
    }

    /**
     * Updates the last_login timestamp for a user.
     * Called when user successfully authenticates.
     *
     * @param userId User ID to update
     * @param loginTime Timestamp of login
     * @return Number of rows updated (should be 1 if successful)
     */
    public int updateLastLogin(long userId, @NonNull LocalDateTime loginTime) {
        Log.d(TAG, "updateLastLogin: Updating last_login for user_id=" + userId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("last_login", loginTime.format(ISO_FORMATTER));

        try {
            int rowsAffected = db.update(
                WeighToGoDBHelper.TABLE_USERS,
                values,
                "user_id = ?",
                new String[]{String.valueOf(userId)}
            );

            if (rowsAffected > 0) {
                Log.i(TAG, "updateLastLogin: Successfully updated last_login for user_id=" + userId);
            } else {
                Log.w(TAG, "updateLastLogin: No rows updated for user_id=" + userId);
            }

            return rowsAffected;

        } catch (Exception e) {
            Log.e(TAG, "updateLastLogin: Exception updating last_login", e);
            return 0;
        }
    }

    /**
     * Updates user's phone number.
     * Accepts null to clear phone number.
     *
     * **Note:** Phone should be E.164 format (+12025551234).
     * Use ValidationUtils.formatPhoneE164() before calling.
     *
     * **Usage Example:**
     * <pre>
     * // Validate and format phone
     * String phone = ValidationUtils.formatPhoneE164(userInput);
     * if (phone != null) {
     *     boolean success = userDAO.updatePhoneNumber(userId, phone);
     * }
     *
     * // Clear phone number
     * boolean cleared = userDAO.updatePhoneNumber(userId, null);
     * </pre>
     *
     * @param userId User ID to update
     * @param phoneNumber E.164 phone number or null to clear
     * @return true if successful (1 row updated), false if user not found (0 rows)
     */
    public boolean updatePhoneNumber(long userId, @Nullable String phoneNumber) {
        Log.d(TAG, "updatePhoneNumber: Updating phone for user_id=" + userId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        // Handle null phone number (clear field)
        if (phoneNumber == null) {
            values.putNull("phone_number");
            Log.d(TAG, "updatePhoneNumber: Clearing phone number for user_id=" + userId);
        } else {
            values.put("phone_number", phoneNumber);
            Log.d(TAG, "updatePhoneNumber: Setting phone to " + phoneNumber + " for user_id=" + userId);
        }

        // Always update the updated_at timestamp
        values.put("updated_at", LocalDateTime.now().format(ISO_FORMATTER));

        try {
            int rowsAffected = db.update(
                WeighToGoDBHelper.TABLE_USERS,
                values,
                "user_id = ?",
                new String[]{String.valueOf(userId)}
            );

            if (rowsAffected > 0) {
                Log.i(TAG, "updatePhoneNumber: Successfully updated phone for user_id=" + userId);
                return true;
            } else {
                Log.w(TAG, "updatePhoneNumber: No rows updated for user_id=" + userId + " (user not found)");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "updatePhoneNumber: Exception updating phone", e);
            return false;
        }
    }

    /**
     * Updates user password and algorithm (for bcrypt migration).
     * Used during lazy migration from SHA256 to bcrypt on user login.
     *
     * Phase 8.6: bcrypt migration support.
     *
     * @param userId User ID to update
     * @param passwordHash New password hash (bcrypt)
     * @param salt New salt (empty string for bcrypt)
     * @param algorithm Password algorithm ('BCRYPT')
     * @return true if successful, false if user not found
     */
    public boolean updatePassword(long userId, @NonNull String passwordHash, @NonNull String salt, @NonNull String algorithm) {
        Log.d(TAG, "updatePassword: Updating password algorithm for user_id=" + userId + " to " + algorithm);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("password_hash", passwordHash);
        values.put("salt", salt);
        values.put("password_algorithm", algorithm);
        values.put("updated_at", LocalDateTime.now().format(ISO_FORMATTER));

        try {
            int rowsAffected = db.update(
                WeighToGoDBHelper.TABLE_USERS,
                values,
                "user_id = ?",
                new String[]{String.valueOf(userId)}
            );

            if (rowsAffected > 0) {
                Log.i(TAG, "updatePassword: Successfully migrated user_id=" + userId + " to " + algorithm);
                return true;
            } else {
                Log.w(TAG, "updatePassword: No rows updated for user_id=" + userId);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "updatePassword: Exception updating password", e);
            return false;
        }
    }

    /**
     * Updates password only if the current hash matches the expected old hash.
     * This prevents race conditions during lazy migration where user changes password
     * while background migration is in progress.
     *
     * @param userId User ID
     * @param oldPasswordHash Expected current password hash (for verification)
     * @param newPasswordHash New password hash
     * @param newSalt New salt (empty string for bcrypt)
     * @param newAlgorithm New password algorithm ('BCRYPT')
     * @return true if password was updated, false if hash mismatch or user not found
     */
    public boolean updatePasswordIfUnchanged(long userId,
                                             @NonNull String oldPasswordHash,
                                             @NonNull String newPasswordHash,
                                             @NonNull String newSalt,
                                             @NonNull String newAlgorithm) {
        Log.d(TAG, "updatePasswordIfUnchanged: Checking hash for user_id=" + userId + " before migration");

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("password_hash", newPasswordHash);
        values.put("salt", newSalt);
        values.put("password_algorithm", newAlgorithm);
        values.put("updated_at", LocalDateTime.now().format(ISO_FORMATTER));

        try {
            // Update only if current password_hash matches the old one
            int rowsAffected = db.update(
                WeighToGoDBHelper.TABLE_USERS,
                values,
                "user_id = ? AND password_hash = ?",
                new String[]{String.valueOf(userId), oldPasswordHash}
            );

            if (rowsAffected > 0) {
                Log.i(TAG, "updatePasswordIfUnchanged: Successfully migrated user_id=" + userId + " to " + newAlgorithm);
                return true;
            } else {
                Log.w(TAG, "updatePasswordIfUnchanged: Password hash changed, migration skipped for user_id=" + userId);
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "updatePasswordIfUnchanged: Exception updating password", e);
            return false;
        }
    }

    /**
     * Deletes a user from the database.
     * CASCADE DELETE will automatically remove associated weight_entries and goal_weights.
     *
     * @param userId User ID to delete
     * @return Number of rows deleted (should be 1 if successful)
     */
    public int deleteUser(long userId) {
        Log.d(TAG, "deleteUser: Deleting user_id=" + userId);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int rowsDeleted = db.delete(
                WeighToGoDBHelper.TABLE_USERS,
                "user_id = ?",
                new String[]{String.valueOf(userId)}
            );

            if (rowsDeleted > 0) {
                Log.i(TAG, "deleteUser: Successfully deleted user_id=" + userId);
            } else {
                Log.w(TAG, "deleteUser: No rows deleted for user_id=" + userId);
            }

            return rowsDeleted;

        } catch (Exception e) {
            Log.e(TAG, "deleteUser: Exception deleting user", e);
            return 0;
        }
    }
}
