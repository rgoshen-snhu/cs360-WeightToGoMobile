package com.example.weightogo.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

/**
 * Unit tests for WeighToGoDBHelper database helper class.
 * Uses Robolectric for Android framework components (SQLite).
 *
 * Following strict TDD: one failing test at a time.
 */
@RunWith(RobolectricTestRunner.class)
public class WeighToGoDBHelperTest {

    private Context context;
    private WeighToGoDBHelper dbHelper;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        // Get fresh instance for each test
        dbHelper = WeighToGoDBHelper.getInstance(context);
    }

    @After
    public void tearDown() {
        try {
            if (dbHelper != null) {
                dbHelper.close();
            }
        } finally {
            dbHelper = null;  // Explicit null assignment to prevent accidental reuse
            // Clean up database file (guaranteed to run even if close() fails)
            context.deleteDatabase("weigh_to_go.db");

            // Reset singleton instance for test isolation
            // This ensures each test gets a fresh database instance
            WeighToGoDBHelper.resetInstance();
        }
    }

    /**
     * Test 1: getInstance returns a non-null instance
     */
    @Test
    public void test_getInstance_returnsSingletonInstance() {
        // ACT
        WeighToGoDBHelper instance = WeighToGoDBHelper.getInstance(context);

        // ASSERT
        assertNotNull("getInstance should return non-null WeighToGoDBHelper", instance);
    }

    /**
     * Test 2: getInstance called twice returns the same instance (Singleton pattern)
     */
    @Test
    public void test_getInstance_calledTwice_returnsSameInstance() {
        // ACT
        WeighToGoDBHelper instance1 = WeighToGoDBHelper.getInstance(context);
        WeighToGoDBHelper instance2 = WeighToGoDBHelper.getInstance(context);

        // ASSERT
        assertSame("getInstance should return the same instance (Singleton)", instance1, instance2);
    }

    /**
     * Test 3: onCreate creates users table with correct schema
     */
    @Test
    public void test_onCreate_createsUsersTable() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='users'",
            null
        )) {
            assertTrue("users table should exist", cursor.moveToFirst());
        }

        // ASSERT - Check table schema
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(users)", null)) {
            int columnCount = cursor.getCount();
            assertEquals("users table should have 12 columns", 12, columnCount);

            // Verify column names (order matters in PRAGMA table_info)
            String[] expectedColumns = {"user_id", "username", "password_hash", "salt", "password_algorithm", "created_at", "last_login", "email", "phone_number", "display_name", "updated_at", "is_active"};
            int index = 0;
            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                assertEquals("Column " + index + " should be " + expectedColumns[index],
                    expectedColumns[index], columnName);
                index++;
            }
        }
    }

    /**
     * Test 4: onCreate creates daily_weights table with correct schema
     */
    @Test
    public void test_onCreate_createsDailyWeightsTable() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='daily_weights'",
            null
        )) {
            assertTrue("daily_weights table should exist", cursor.moveToFirst());
        }

        // ASSERT - Check table schema
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(daily_weights)", null)) {
            int columnCount = cursor.getCount();
            assertEquals("daily_weights table should have 9 columns", 9, columnCount);

            // Verify required columns exist
            boolean hasWeightId = false;
            boolean hasUserId = false;
            boolean hasWeightValue = false;
            boolean hasWeightDate = false;
            boolean hasIsDeleted = false;

            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (columnName.equals("weight_id")) hasWeightId = true;
                if (columnName.equals("user_id")) hasUserId = true;
                if (columnName.equals("weight_value")) hasWeightValue = true;
                if (columnName.equals("weight_date")) hasWeightDate = true;
                if (columnName.equals("is_deleted")) hasIsDeleted = true;
            }

            assertTrue("daily_weights should have weight_id column", hasWeightId);
            assertTrue("daily_weights should have user_id column", hasUserId);
            assertTrue("daily_weights should have weight_value column", hasWeightValue);
            assertTrue("daily_weights should have weight_date column", hasWeightDate);
            assertTrue("daily_weights should have is_deleted column", hasIsDeleted);
        }
    }

    /**
     * Test 5: onCreate creates goal_weights table with correct schema
     */
    @Test
    public void test_onCreate_createsGoalWeightsTable() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='goal_weights'",
            null
        )) {
            assertTrue("goal_weights table should exist", cursor.moveToFirst());
        }

        // ASSERT - Check table schema
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(goal_weights)", null)) {
            int columnCount = cursor.getCount();
            assertEquals("goal_weights table should have 11 columns", 11, columnCount);

            // Verify required columns exist
            boolean hasGoalId = false;
            boolean hasUserId = false;
            boolean hasGoalWeight = false;
            boolean hasIsActive = false;

            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (columnName.equals("goal_id")) hasGoalId = true;
                if (columnName.equals("user_id")) hasUserId = true;
                if (columnName.equals("goal_weight")) hasGoalWeight = true;
                if (columnName.equals("is_active")) hasIsActive = true;
            }

            assertTrue("goal_weights should have goal_id column", hasGoalId);
            assertTrue("goal_weights should have user_id column", hasUserId);
            assertTrue("goal_weights should have goal_weight column", hasGoalWeight);
            assertTrue("goal_weights should have is_active column", hasIsActive);
        }
    }

    /**
     * Test 6: onConfigure enables foreign keys
     */
    @Test
    public void test_onConfigure_enablesForeignKeys() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check foreign_keys pragma is ON
        try (Cursor cursor = db.rawQuery("PRAGMA foreign_keys", null)) {
            assertTrue("PRAGMA foreign_keys should return a result", cursor.moveToFirst());
            int foreignKeysEnabled = cursor.getInt(0);
            assertEquals("Foreign keys should be enabled (1)", 1, foreignKeysEnabled);
        }
    }

    // ========== EDGE CASE TESTS ==========

    /**
     * Test 7: Foreign key constraint prevents orphaned weight entries
     * Attempting to insert daily_weight with non-existent user_id should fail
     */
    @Test(expected = android.database.sqlite.SQLiteConstraintException.class)
    public void test_foreignKey_preventOrphanedRecords() {
        // ARRANGE
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int nonExistentUserId = 999;

        // ACT - Try to insert daily_weight with invalid user_id
        // This should throw SQLiteConstraintException due to FOREIGN KEY constraint
        db.execSQL(
            "INSERT INTO daily_weights (user_id, weight_value, weight_unit, weight_date, created_at, updated_at, is_deleted) " +
            "VALUES (" + nonExistentUserId + ", 175.5, 'lbs', '2025-12-10', '2025-12-10 10:00:00', '2025-12-10 10:00:00', 0)"
        );

        // ASSERT - Exception should be thrown (test passes if exception is thrown)
    }

    /**
     * Test 8: Foreign key CASCADE DELETE removes child records when parent is deleted
     * Deleting a user should automatically delete their daily_weights and goal_weights
     */
    @Test
    public void test_foreignKey_cascadeDelete() {
        // ARRANGE
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Insert test user
        db.execSQL(
            "INSERT INTO users (username, password_hash, salt, created_at, updated_at, is_active) " +
            "VALUES ('testuser', 'hash123', 'salt456', '2025-12-10 10:00:00', '2025-12-10 10:00:00', 1)"
        );

        // Get the inserted user ID
        Cursor userCursor = db.rawQuery("SELECT user_id FROM users WHERE username = 'testuser'", null);
        assertTrue("User should be inserted", userCursor.moveToFirst());
        int userId = userCursor.getInt(0);
        userCursor.close();

        // Insert daily_weight for this user
        db.execSQL(
            "INSERT INTO daily_weights (user_id, weight_value, weight_unit, weight_date, created_at, updated_at, is_deleted) " +
            "VALUES (" + userId + ", 175.5, 'lbs', '2025-12-10', '2025-12-10 10:00:00', '2025-12-10 10:00:00', 0)"
        );

        // Insert goal_weight for this user
        db.execSQL(
            "INSERT INTO goal_weights (user_id, goal_weight, goal_unit, start_weight, created_at, updated_at, is_active) " +
            "VALUES (" + userId + ", 160.0, 'lbs', 175.5, '2025-12-10 10:00:00', '2025-12-10 10:00:00', 1)"
        );

        // Verify records exist before deletion
        Cursor entryCursor = db.rawQuery("SELECT COUNT(*) FROM daily_weights WHERE user_id = " + userId, null);
        entryCursor.moveToFirst();
        assertEquals("Should have 1 daily weight", 1, entryCursor.getInt(0));
        entryCursor.close();

        Cursor goalCursor = db.rawQuery("SELECT COUNT(*) FROM goal_weights WHERE user_id = " + userId, null);
        goalCursor.moveToFirst();
        assertEquals("Should have 1 goal weight", 1, goalCursor.getInt(0));
        goalCursor.close();

        // ACT - Delete the user
        db.execSQL("DELETE FROM users WHERE user_id = " + userId);

        // ASSERT - Child records should be automatically deleted via CASCADE DELETE
        entryCursor = db.rawQuery("SELECT COUNT(*) FROM daily_weights WHERE user_id = " + userId, null);
        entryCursor.moveToFirst();
        assertEquals("Daily weights should be cascade deleted", 0, entryCursor.getInt(0));
        entryCursor.close();

        goalCursor = db.rawQuery("SELECT COUNT(*) FROM goal_weights WHERE user_id = " + userId, null);
        goalCursor.moveToFirst();
        assertEquals("Goal weights should be cascade deleted", 0, goalCursor.getInt(0));
        goalCursor.close();
    }

    /**
     * Test 9: onCreate creates unique index on users.username
     * Username must be unique and queries should be fast
     */
    @Test
    public void test_onCreate_createsUniqueIndexOnUsername() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_users_username'",
            null
        )) {
            assertTrue("Index idx_users_username should exist", cursor.moveToFirst());
        }

        // ASSERT - Verify it's a unique index by checking sql column
        try (Cursor cursor = db.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type='index' AND name='idx_users_username'",
            null
        )) {
            assertTrue("Should find index definition", cursor.moveToFirst());
            String sql = cursor.getString(0);
            assertTrue("Index should be UNIQUE", sql.toUpperCase().contains("UNIQUE"));
        }
    }

    /**
     * Test 10: onCreate creates index on users.email (conditional)
     */
    @Test
    public void test_onCreate_createsIndexOnUsersEmail() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_users_email'",
            null
        )) {
            assertTrue("Index idx_users_email should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 11: onCreate creates index on users.is_active
     */
    @Test
    public void test_onCreate_createsIndexOnUsersActive() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_users_active'",
            null
        )) {
            assertTrue("Index idx_users_active should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 12: onCreate creates composite index on daily_weights(user_id, weight_date)
     * Critical for user-specific date range queries
     */
    @Test
    public void test_onCreate_createsIndexOnWeightsUserDate() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_weights_user_date'",
            null
        )) {
            assertTrue("Index idx_weights_user_date should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 13: onCreate creates index on daily_weights.weight_date
     * Optimizes date sorting and range queries
     */
    @Test
    public void test_onCreate_createsIndexOnWeightsDate() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_weights_date'",
            null
        )) {
            assertTrue("Index idx_weights_date should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 14: onCreate creates index on daily_weights(user_id, created_at DESC)
     * Optimizes queries for recent entries by user
     */
    @Test
    public void test_onCreate_createsIndexOnWeightsUserCreated() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_weights_user_created'",
            null
        )) {
            assertTrue("Index idx_weights_user_created should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 15: onCreate creates composite index on goal_weights(user_id, is_active)
     * Optimizes finding active goal for user
     */
    @Test
    public void test_onCreate_createsIndexOnGoalsUserActive() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_goals_user_active'",
            null
        )) {
            assertTrue("Index idx_goals_user_active should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 16: onCreate creates index on goal_weights.is_achieved
     */
    @Test
    public void test_onCreate_createsIndexOnGoalsAchieved() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_goals_achieved'",
            null
        )) {
            assertTrue("Index idx_goals_achieved should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 17: onCreate creates index on achievements.user_id
     */
    @Test
    public void test_onCreate_createsIndexOnAchievementsUser() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_achievements_user'",
            null
        )) {
            assertTrue("Index idx_achievements_user should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 18: onCreate creates composite index on achievements(user_id, is_notified)
     */
    @Test
    public void test_onCreate_createsIndexOnAchievementsUnnotified() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_achievements_unnotified'",
            null
        )) {
            assertTrue("Index idx_achievements_unnotified should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 19: onCreate creates index on achievements.achievement_type
     */
    @Test
    public void test_onCreate_createsIndexOnAchievementsType() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_achievements_type'",
            null
        )) {
            assertTrue("Index idx_achievements_type should exist", cursor.moveToFirst());
        }
    }

    /**
     * Test 20: onCreate creates unique composite index on user_preferences(user_id, pref_key)
     */
    @Test
    public void test_onCreate_createsUniqueIndexOnPrefsUserKey() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check index exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_prefs_user_key'",
            null
        )) {
            assertTrue("Index idx_prefs_user_key should exist", cursor.moveToFirst());
        }

        // ASSERT - Verify it's a unique index
        try (Cursor cursor = db.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type='index' AND name='idx_prefs_user_key'",
            null
        )) {
            assertTrue("Should find index definition", cursor.moveToFirst());
            String sql = cursor.getString(0);
            assertTrue("Index should be UNIQUE", sql.toUpperCase().contains("UNIQUE"));
        }
    }

    // ========== EDGE CASE TESTS ==========

    /**
     * Test 21: onUpgrade v1->v2 adds password_algorithm column (Phase 8.6)
     * Simulates incremental migration that preserves user data
     */
    @Test
    public void test_onUpgrade_v1ToV2_addsPasswordAlgorithmColumn() {
        // ARRANGE - Create a v1 database by manually creating the old schema
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Drop existing table and create v1 schema (without password_algorithm)
        db.execSQL("DROP TABLE IF EXISTS users");
        db.execSQL(
            "CREATE TABLE users (" +
            "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT NOT NULL UNIQUE, " +
            "password_hash TEXT NOT NULL, " +
            "salt TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, " +
            "last_login TEXT, " +
            "email TEXT, " +
            "phone_number TEXT, " +
            "display_name TEXT, " +
            "updated_at TEXT NOT NULL, " +
            "is_active INTEGER NOT NULL DEFAULT 1)"
        );

        // Insert test user into v1 schema
        db.execSQL(
            "INSERT INTO users (username, password_hash, salt, created_at, updated_at, is_active) " +
            "VALUES ('testuser', 'hash123', 'salt456', '2025-12-10 10:00:00', '2025-12-10 10:00:00', 1)"
        );

        // Verify v1 schema has 11 columns
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(users)", null)) {
            assertEquals("v1 schema should have 11 columns", 11, cursor.getCount());
        }

        // ACT - Trigger onUpgrade (v1 -> v2 adds password_algorithm)
        dbHelper.onUpgrade(db, 1, 2);

        // ASSERT - User data should be preserved
        try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM users", null)) {
            cursor.moveToFirst();
            int userCountAfter = cursor.getInt(0);
            assertEquals("User should be preserved after upgrade", 1, userCountAfter);
        }

        // ASSERT - password_algorithm column should exist with default value
        try (Cursor cursor = db.rawQuery("SELECT password_algorithm FROM users WHERE username = 'testuser'", null)) {
            assertTrue("User should exist", cursor.moveToFirst());
            String algorithm = cursor.getString(0);
            assertEquals("password_algorithm should default to SHA256", "SHA256", algorithm);
        }

        // ASSERT - v2 schema should have 12 columns
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(users)", null)) {
            assertEquals("v2 schema should have 12 columns", 12, cursor.getCount());
        }

        // Check daily_weights table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='daily_weights'",
            null
        )) {
            assertTrue("daily_weights table should exist after upgrade", cursor.moveToFirst());
        }

        // Check goal_weights table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='goal_weights'",
            null
        )) {
            assertTrue("goal_weights table should exist after upgrade", cursor.moveToFirst());
        }

        // Check achievements table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='achievements'",
            null
        )) {
            assertTrue("achievements table should exist after upgrade", cursor.moveToFirst());
        }

        // Check user_preferences table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='user_preferences'",
            null
        )) {
            assertTrue("user_preferences table should exist after upgrade", cursor.moveToFirst());
        }
    }

    /**
     * Test 22: onCreate creates achievements table with correct schema
     */
    @Test
    public void test_onCreate_createsAchievementsTable() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='achievements'",
            null
        )) {
            assertTrue("achievements table should exist", cursor.moveToFirst());
        }

        // ASSERT - Check table schema
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(achievements)", null)) {
            int columnCount = cursor.getCount();
            assertEquals("achievements table should have 9 columns", 9, columnCount);

            // Verify required columns exist
            boolean hasAchievementId = false;
            boolean hasUserId = false;
            boolean hasGoalId = false;
            boolean hasAchievementType = false;
            boolean hasIsNotified = false;

            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (columnName.equals("achievement_id")) hasAchievementId = true;
                if (columnName.equals("user_id")) hasUserId = true;
                if (columnName.equals("goal_id")) hasGoalId = true;
                if (columnName.equals("achievement_type")) hasAchievementType = true;
                if (columnName.equals("is_notified")) hasIsNotified = true;
            }

            assertTrue("achievements should have achievement_id column", hasAchievementId);
            assertTrue("achievements should have user_id column", hasUserId);
            assertTrue("achievements should have goal_id column", hasGoalId);
            assertTrue("achievements should have achievement_type column", hasAchievementType);
            assertTrue("achievements should have is_notified column", hasIsNotified);
        }
    }

    /**
     * Test 23: onCreate creates user_preferences table with correct schema
     */
    @Test
    public void test_onCreate_createsUserPreferencesTable() {
        // ACT
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // ASSERT - Check table exists
        try (Cursor cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='user_preferences'",
            null
        )) {
            assertTrue("user_preferences table should exist", cursor.moveToFirst());
        }

        // ASSERT - Check table schema
        try (Cursor cursor = db.rawQuery("PRAGMA table_info(user_preferences)", null)) {
            int columnCount = cursor.getCount();
            assertEquals("user_preferences table should have 6 columns", 6, columnCount);

            // Verify required columns exist
            boolean hasPreferenceId = false;
            boolean hasUserId = false;
            boolean hasPrefKey = false;
            boolean hasPrefValue = false;

            while (cursor.moveToNext()) {
                String columnName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                if (columnName.equals("preference_id")) hasPreferenceId = true;
                if (columnName.equals("user_id")) hasUserId = true;
                if (columnName.equals("pref_key")) hasPrefKey = true;
                if (columnName.equals("pref_value")) hasPrefValue = true;
            }

            assertTrue("user_preferences should have preference_id column", hasPreferenceId);
            assertTrue("user_preferences should have user_id column", hasUserId);
            assertTrue("user_preferences should have pref_key column", hasPrefKey);
            assertTrue("user_preferences should have pref_value column", hasPrefValue);
        }
    }
}
