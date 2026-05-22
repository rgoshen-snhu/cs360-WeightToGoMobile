package com.example.weightogo.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLite database helper for Weigh to Go application.
 *
 * Implements Singleton pattern for thread-safe single database instance.
 * Manages database creation, upgrades, and foreign key enforcement.
 *
 * Database Schema (per WeighToGo_Database_Architecture.md):
 * - users: User authentication and profile data
 * - daily_weights: Daily weight tracking with soft delete support
 * - goal_weights: User goal weights and achievement tracking
 * - achievements: Milestone achievements and celebration events
 * - user_preferences: User settings and preferences (key-value store)
 *
 * Naming Convention:
 * - Database: snake_case (id, user_id, created_at) - Android/SQL convention
 * - Java Models: camelCase (userId, createdAt) - Java convention
 * - DAO Layer: Handles mapping between DB snake_case and Java camelCase
 *   Example: cursor.getLong(cursor.getColumnIndexOrThrow("user_id")) → user.setUserId(value)
 *
 * Performance Optimization:
 * - Indexes on foreign key columns (user_id) for faster JOIN and WHERE queries
 * - Unique index on username for faster login lookups and uniqueness enforcement
 *
 * Security:
 * - Uses foreign keys for referential integrity
 * - Passwords stored as salted SHA-256 hashes (never plain text)
 * - All user input should use parameterized queries (handled in DAOs)
 */
public class WeighToGoDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "WeighToGoDBHelper";

    // Database configuration
    private static final String DATABASE_NAME = "weigh_to_go.db";
    public static final String DATABASE_TEST_NAME = "weigh_to_go_test.db";
    private static final int DATABASE_VERSION = 2;  // Phase 8.6: bcrypt migration

    // Singleton instance
    private static WeighToGoDBHelper instance;

    // Table names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_DAILY_WEIGHTS = "daily_weights";
    public static final String TABLE_GOAL_WEIGHTS = "goal_weights";
    public static final String TABLE_ACHIEVEMENTS = "achievements";
    public static final String TABLE_USER_PREFERENCES = "user_preferences";

    // SQL: Create users table
    private static final String CREATE_TABLE_USERS =
        "CREATE TABLE " + TABLE_USERS + " (" +
            "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "username TEXT NOT NULL UNIQUE, " +
            "password_hash TEXT NOT NULL, " +
            "salt TEXT NOT NULL, " +
            "password_algorithm TEXT NOT NULL DEFAULT 'SHA256', " +  // Phase 8.6: bcrypt migration
            "created_at TEXT NOT NULL, " +
            "last_login TEXT, " +
            "email TEXT, " +
            "phone_number TEXT, " +
            "display_name TEXT, " +
            "updated_at TEXT NOT NULL, " +
            "is_active INTEGER NOT NULL DEFAULT 1" +
        ")";

    // SQL: Create daily_weights table
    private static final String CREATE_TABLE_DAILY_WEIGHTS =
        "CREATE TABLE " + TABLE_DAILY_WEIGHTS + " (" +
            "weight_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "weight_value REAL NOT NULL, " +
            "weight_unit TEXT NOT NULL, " +
            "weight_date TEXT NOT NULL, " +
            "notes TEXT, " +
            "created_at TEXT NOT NULL, " +
            "updated_at TEXT NOT NULL, " +
            "is_deleted INTEGER NOT NULL DEFAULT 0, " +
            "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE" +
        ")";

    // SQL: Create goal_weights table
    private static final String CREATE_TABLE_GOAL_WEIGHTS =
        "CREATE TABLE " + TABLE_GOAL_WEIGHTS + " (" +
            "goal_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "goal_weight REAL NOT NULL, " +
            "goal_unit TEXT NOT NULL, " +
            "start_weight REAL NOT NULL, " +
            "target_date TEXT, " +
            "is_achieved INTEGER NOT NULL DEFAULT 0, " +
            "achieved_date TEXT, " +
            "created_at TEXT NOT NULL, " +
            "updated_at TEXT NOT NULL, " +
            "is_active INTEGER NOT NULL DEFAULT 1, " +
            "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE" +
        ")";

    // SQL: Create achievements table
    private static final String CREATE_TABLE_ACHIEVEMENTS =
        "CREATE TABLE " + TABLE_ACHIEVEMENTS + " (" +
            "achievement_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "goal_id INTEGER, " +
            "achievement_type TEXT NOT NULL, " +
            "title TEXT NOT NULL, " +
            "description TEXT, " +
            "value REAL, " +
            "achieved_at TEXT NOT NULL, " +
            "is_notified INTEGER NOT NULL DEFAULT 0, " +
            "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE, " +
            "FOREIGN KEY (goal_id) REFERENCES " + TABLE_GOAL_WEIGHTS + "(goal_id) ON DELETE SET NULL" +
        ")";

    // SQL: Create user_preferences table
    private static final String CREATE_TABLE_USER_PREFERENCES =
        "CREATE TABLE " + TABLE_USER_PREFERENCES + " (" +
            "preference_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "user_id INTEGER NOT NULL, " +
            "pref_key TEXT NOT NULL, " +
            "pref_value TEXT NOT NULL, " +
            "created_at TEXT NOT NULL, " +
            "updated_at TEXT NOT NULL, " +
            "FOREIGN KEY (user_id) REFERENCES " + TABLE_USERS + "(user_id) ON DELETE CASCADE, " +
            "UNIQUE (user_id, pref_key)" +
        ")";

    /**
     * Private constructor to enforce Singleton pattern.
     *
     * @param context application context
     */
    private WeighToGoDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "WeighToGoDBHelper constructor called");
    }

     /**
     * Private constructor to enforce Singleton pattern.
     *
     * @param context application context
     * @param dbName the name of the database
     */
    private WeighToGoDBHelper(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
        Log.d(TAG, "WeighToGoDBHelper constructor called with dbName: " + dbName);
    }

    /**
     * Get singleton instance of database helper (thread-safe).
     *
     * Thread Safety:
     * - Method is synchronized to prevent race conditions during initialization
     * - Safe to call from multiple threads concurrently
     * - Always returns same instance regardless of calling thread
     *
     * Context Handling & Memory Leak Prevention:
     * - CRITICAL: Uses Application context via context.getApplicationContext()
     * - Application context lives for entire app lifecycle (safe to hold statically)
     * - Activity context would leak if Activity destroyed but singleton persists
     * - DO NOT REMOVE getApplicationContext() call - it prevents memory leaks!
     * - Safe to pass Activity or Application context - both work correctly
     *
     * Why This Matters:
     * - Singleton pattern holds static reference to database helper instance
     * - Static references are never garbage collected
     * - If we stored Activity context, the Activity could never be garbage collected
     * - This would leak entire Activity + View hierarchy on every configuration change
     * - Application context is designed to be held statically (no leak)
     *
     * @param context any Context (Activity or Application) - will use Application context internally
     * @return singleton WeighToGoDBHelper instance
     */
    public static synchronized WeighToGoDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new WeighToGoDBHelper(context.getApplicationContext());
            Log.i(TAG, "Created new WeighToGoDBHelper instance for database: " + DATABASE_NAME);
        }
        return instance;
    }
    
    /**
     * Get singleton instance of a test database helper (thread-safe).
     *
     * @param context any Context (Activity or Application) - will use Application context internally
     * @return singleton WeighToGoDBHelper instance
     */
    public static synchronized WeighToGoDBHelper getTestInstance(Context context, String dbName) {
        if (instance == null) {
            instance = new WeighToGoDBHelper(context.getApplicationContext(), dbName);
            Log.i(TAG, "Created new WeighToGoDBHelper instance for database: " + dbName);
        }
        return instance;
    }


    /**
     * Reset singleton instance for testing purposes.
     * Package-private visibility ensures this is only accessible from test code.
     *
     * WARNING: This should ONLY be called from unit tests during tearDown.
     * Never call this from production code.
     */
    static synchronized void resetInstance() {
        if (instance != null) {
            instance.close();
            instance = null;
            Log.d(TAG, "Singleton instance reset for testing");
        }
    }

    /**
     * Configure database before opening.
     * Enables foreign key constraints for referential integrity.
     *
     * @param db the database
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
        Log.d(TAG, "Foreign key constraints enabled");
    }

    /**
     * Called when database is created for the first time.
     * Creates all tables with proper schema and constraints.
     *
     * @param db the database
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Creating database " + db.getPath() + " version " + DATABASE_VERSION);

        try {
            // Create users table
            db.execSQL(CREATE_TABLE_USERS);
            Log.d(TAG, "Created table: " + TABLE_USERS);

            // Create daily_weights table
            db.execSQL(CREATE_TABLE_DAILY_WEIGHTS);
            Log.d(TAG, "Created table: " + TABLE_DAILY_WEIGHTS);

            // Create goal_weights table
            db.execSQL(CREATE_TABLE_GOAL_WEIGHTS);
            Log.d(TAG, "Created table: " + TABLE_GOAL_WEIGHTS);

            // Create achievements table
            db.execSQL(CREATE_TABLE_ACHIEVEMENTS);
            Log.d(TAG, "Created table: " + TABLE_ACHIEVEMENTS);

            // Create user_preferences table
            db.execSQL(CREATE_TABLE_USER_PREFERENCES);
            Log.d(TAG, "Created table: " + TABLE_USER_PREFERENCES);

            // ================================================================================
            // INDEXES (per WeighToGo_Database_Architecture.md lines 308-336)
            // ================================================================================

            // Users table indexes
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON " + TABLE_USERS + "(username)");
            Log.d(TAG, "Created index: idx_users_username");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_email ON " + TABLE_USERS + "(email) WHERE email IS NOT NULL");
            Log.d(TAG, "Created index: idx_users_email");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_users_active ON " + TABLE_USERS + "(is_active)");
            Log.d(TAG, "Created index: idx_users_active");

            // Daily weights table indexes (most critical for performance)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_weights_user_date ON " + TABLE_DAILY_WEIGHTS +
                "(user_id, weight_date) WHERE is_deleted = 0");
            Log.d(TAG, "Created index: idx_weights_user_date");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_date ON " + TABLE_DAILY_WEIGHTS + "(weight_date)");
            Log.d(TAG, "Created index: idx_weights_date");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_user_created ON " + TABLE_DAILY_WEIGHTS +
                "(user_id, created_at DESC)");
            Log.d(TAG, "Created index: idx_weights_user_created");

            // Goal weights table indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_goals_user_active ON " + TABLE_GOAL_WEIGHTS + "(user_id, is_active)");
            Log.d(TAG, "Created index: idx_goals_user_active");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_goals_achieved ON " + TABLE_GOAL_WEIGHTS + "(is_achieved)");
            Log.d(TAG, "Created index: idx_goals_achieved");

            // Achievements table indexes
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_achievements_user ON " + TABLE_ACHIEVEMENTS + "(user_id)");
            Log.d(TAG, "Created index: idx_achievements_user");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_achievements_unnotified ON " + TABLE_ACHIEVEMENTS +
                "(user_id, is_notified) WHERE is_notified = 0");
            Log.d(TAG, "Created index: idx_achievements_unnotified");

            db.execSQL("CREATE INDEX IF NOT EXISTS idx_achievements_type ON " + TABLE_ACHIEVEMENTS + "(achievement_type)");
            Log.d(TAG, "Created index: idx_achievements_type");

            // User preferences table indexes
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_prefs_user_key ON " + TABLE_USER_PREFERENCES +
                "(user_id, pref_key)");
            Log.d(TAG, "Created index: idx_prefs_user_key");

            Log.i(TAG, "Database creation completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error creating database tables: " + e.getMessage(), e);
            throw e;  // Re-throw to ensure app doesn't continue with broken database
        }
    }

    /**
     * Called when database needs to be upgraded (version increase).
     *
     * PRODUCTION MIGRATION STRATEGY (Phase 8.6):
     * - Implements incremental migrations to preserve user data
     * - Uses switch statement with fall-through for sequential upgrades
     * - Each version upgrade is a separate method for maintainability
     *
     * @param db the database
     * @param oldVersion the old database version
     * @param newVersion the new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        try {
            // Incremental migration pattern - each case falls through to next
            switch (oldVersion) {
                case 1:
                    upgradeToV2(db);  // Add password_algorithm column
                    // Fall through to next version when available
                case 2:
                    // Future: upgradeToV3(db);
                    // Fall through
                default:
                    break;
            }

            Log.i(TAG, "Database upgrade completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database from version " + oldVersion + " to " + newVersion, e);
            throw e;
        }
    }

    /**
     * Upgrade database from version 1 to version 2.
     * Adds password_algorithm column to users table for bcrypt migration.
     *
     * Migration Strategy:
     * - Adds password_algorithm column with DEFAULT 'SHA256'
     * - All existing users automatically get 'SHA256' algorithm
     * - Lazy migration: passwords rehashed to bcrypt on next login
     *
     * @param db the database
     */
    private void upgradeToV2(SQLiteDatabase db) {
        Log.i(TAG, "Upgrading to version 2: Adding password_algorithm column");

        try {
            // Add password_algorithm column with default value for existing users
            db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN password_algorithm TEXT NOT NULL DEFAULT 'SHA256'");

            Log.i(TAG, "Successfully added password_algorithm column to users table");
            Log.i(TAG, "All existing users set to SHA256 algorithm (will migrate to bcrypt on next login)");

        } catch (Exception e) {
            Log.e(TAG, "Error upgrading to version 2", e);
            throw e;
        }
    }
}
