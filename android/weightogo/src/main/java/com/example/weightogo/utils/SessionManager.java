package com.example.weightogo.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weightogo.models.User;

import java.time.LocalDateTime;

/**
 * Session manager for user authentication and session persistence.
 *
 * Implements Singleton pattern for application-wide session management.
 * Uses SharedPreferences to persist user sessions across app restarts.
 *
 * **Session Data Stored:**
 * - User ID (long)
 * - Username (String)
 * - Display name (String, optional)
 * - Login status (boolean)
 *
 * **Security Notes:**
 * - NEVER stores password hash or salt in SharedPreferences
 * - Uses Application context to prevent memory leaks
 * - Session data is cleared on logout
 *
 * **Usage Example:**
 * <pre>
 * // After successful login/registration
 * SessionManager sessionManager = SessionManager.getInstance(context);
 * sessionManager.createSession(user);
 *
 * // Check if user is logged in
 * if (sessionManager.isLoggedIn()) {
 *     User currentUser = sessionManager.getCurrentUser();
 *     long userId = sessionManager.getCurrentUserId();
 * }
 *
 * // Logout
 * sessionManager.logout();
 * </pre>
 *
 * **Thread Safety:** All methods are synchronized for thread-safe access.
 */
public class SessionManager {

    private static final String TAG = "SessionManager";

    /**
     * SharedPreferences file name for session storage.
     */
    private static final String PREF_NAME = "WeightOgOSession";

    /**
     * SharedPreferences keys for session data.
     */
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    /**
     * Sentinel value for user ID when no session exists.
     */
    private static final long NO_SESSION_USER_ID = -1L;

    /**
     * Singleton instance.
     */
    private static SessionManager instance;

    /**
     * SharedPreferences for session persistence.
     */
    private final SharedPreferences preferences;

    /**
     * Private constructor to enforce Singleton pattern.
     *
     * Uses Application context to prevent memory leaks. See WeighToGoDBHelper
     * documentation for detailed explanation of context handling.
     *
     * @param context any Context (Activity or Application) - will use Application context internally
     */
    private SessionManager(Context context) {
        // Use Application context to prevent memory leaks
        this.preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        Log.d(TAG, "SessionManager initialized");
    }

    /**
     * Get singleton instance of SessionManager (thread-safe).
     *
     * Thread Safety:
     * - Method is synchronized to prevent race conditions during initialization
     * - Safe to call from multiple threads concurrently
     * - Always returns same instance regardless of calling thread
     *
     * Context Handling & Memory Leak Prevention:
     * - Uses Application context via context.getApplicationContext()
     * - Safe to pass Activity or Application context
     * - See WeighToGoDBHelper.getInstance() for detailed context handling explanation
     *
     * @param context any Context (Activity or Application) - will use Application context internally
     * @return singleton SessionManager instance
     */
    public static synchronized SessionManager getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
            Log.i(TAG, "Created new SessionManager instance");
        }
        return instance;
    }

    /**
     * Create a user session after successful login or registration.
     *
     * Stores minimal user data in SharedPreferences for session persistence.
     * Does NOT store sensitive data (password hash, salt, email, phone).
     *
     * @param user the authenticated user
     */
    public synchronized void createSession(@NonNull User user) {
        SharedPreferences.Editor editor = preferences.edit();

        editor.putLong(KEY_USER_ID, user.getUserId());
        editor.putString(KEY_USERNAME, user.getUsername());
        editor.putString(KEY_DISPLAY_NAME, user.getDisplayName());
        editor.putBoolean(KEY_IS_LOGGED_IN, true);

        editor.apply();  // Asynchronous write

        // Use Log.d() for sensitive data (username) - automatically stripped in release builds by R8/ProGuard
        Log.d(TAG, "Session created for user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
    }

    /**
     * Get the current logged-in user.
     *
     * **IMPORTANT:** Returns a partial User object with ONLY session data:
     * - ✅ Valid fields: userId, username, displayName
     * - ❌ Invalid fields: passwordHash="", salt="", createdAt=now(), updatedAt=now(), isActive=true (dummy values)
     *
     * **If you need full User data, query UserDAO.getUserById(userId) instead.**
     *
     * **Rationale for dummy fields:**
     * - SharedPreferences should never store password hashes or salts (security)
     * - Timestamps are not needed for session management (performance)
     * - User object requires @NonNull fields, so dummy values are used as placeholders
     *
     * **TECHNICAL DEBT:** This design will be refactored in Phase 7.7 to use a dedicated
     * SessionUser class (userId, username, displayName only) instead of full User object.
     *
     * @return User object with session data if logged in, null otherwise
     */
    @Nullable
    public synchronized User getCurrentUser() {
        if (!isLoggedIn()) {
            Log.d(TAG, "getCurrentUser: No active session");
            return null;
        }

        long userId = preferences.getLong(KEY_USER_ID, NO_SESSION_USER_ID);
        String username = preferences.getString(KEY_USERNAME, null);
        String displayName = preferences.getString(KEY_DISPLAY_NAME, null);

        if (userId == NO_SESSION_USER_ID || username == null) {
            Log.w(TAG, "getCurrentUser: Invalid session data");
            return null;
        }

        // Reconstruct partial User object (session data only)
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setDisplayName(displayName);

        // Set dummy values for required @NonNull fields (not persisted in session)
        user.setPasswordHash("");
        user.setSalt("");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        Log.d(TAG, "getCurrentUser: Retrieved user " + username);
        return user;
    }

    /**
     * Get the current logged-in user's ID.
     *
     * @return user ID if session exists, -1 otherwise (sentinel value)
     */
    public synchronized long getCurrentUserId() {
        long userId = preferences.getLong(KEY_USER_ID, NO_SESSION_USER_ID);
        Log.d(TAG, "getCurrentUserId: " + userId);
        return userId;
    }

    /**
     * Check if a user is currently logged in.
     *
     * @return true if session exists, false otherwise
     */
    public synchronized boolean isLoggedIn() {
        boolean loggedIn = preferences.getBoolean(KEY_IS_LOGGED_IN, false);
        Log.d(TAG, "isLoggedIn: " + loggedIn);
        return loggedIn;
    }

    /**
     * Clear the current user session (logout).
     *
     * Removes all session data from SharedPreferences.
     * User must login again to access protected features.
     */
    public synchronized void logout() {
        String username = preferences.getString(KEY_USERNAME, "unknown");

        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();  // Remove all session data
        editor.apply();  // Asynchronous write

        Log.i(TAG, "Session cleared for user: " + username);
    }
}
