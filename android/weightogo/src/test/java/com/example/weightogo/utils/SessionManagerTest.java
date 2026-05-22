package com.example.weightogo.utils;

import android.content.Context;

import com.example.weightogo.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for SessionManager class.
 * Tests singleton pattern, session creation, retrieval, and logout.
 *
 * Uses Robolectric for SharedPreferences testing.
 */
@RunWith(RobolectricTestRunner.class)
public class SessionManagerTest {

    private Context context;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        sessionManager = SessionManager.getInstance(context);
        // Clear any existing session before each test
        sessionManager.logout();
    }

    @After
    public void tearDown() {
        // Clean up session after each test
        if (sessionManager != null) {
            sessionManager.logout();
        }
    }

    // =============================================================================================
    // SINGLETON PATTERN TESTS (2 tests)
    // =============================================================================================

    /**
     * Tests that getInstance() returns a non-null singleton instance.
     */
    @Test
    public void test_getInstance_returnsSingletonInstance() {
        // ACT
        SessionManager instance = SessionManager.getInstance(context);

        // ASSERT
        assertNotNull("SessionManager instance should not be null", instance);
    }

    /**
     * Tests that getInstance() called twice returns the same instance (singleton).
     */
    @Test
    public void test_getInstance_calledTwice_returnsSameInstance() {
        // ACT
        SessionManager instance1 = SessionManager.getInstance(context);
        SessionManager instance2 = SessionManager.getInstance(context);

        // ASSERT
        assertSame("getInstance should return same instance (singleton)", instance1, instance2);
    }

    // =============================================================================================
    // SESSION CREATION TESTS (1 test)
    // =============================================================================================

    /**
     * Tests that createSession() stores user session data.
     */
    @Test
    public void test_createSession_withValidUser_storesSession() {
        // ARRANGE
        User user = createTestUser(1L, "testuser", "Test User");

        // ACT
        sessionManager.createSession(user);

        // ASSERT
        assertTrue("Session should be created and marked as logged in", sessionManager.isLoggedIn());
    }

    // =============================================================================================
    // CURRENT USER RETRIEVAL TESTS (2 tests)
    // =============================================================================================

    /**
     * Tests that getCurrentUser() returns null when no session exists.
     */
    @Test
    public void test_getCurrentUser_withNoSession_returnsNull() {
        // ACT
        User user = sessionManager.getCurrentUser();

        // ASSERT
        assertNull("getCurrentUser should return null when no session exists", user);
    }

    /**
     * Tests that getCurrentUser() returns the user after createSession().
     */
    @Test
    public void test_getCurrentUser_afterCreateSession_returnsUser() {
        // ARRANGE
        User originalUser = createTestUser(1L, "testuser", "Test User");
        sessionManager.createSession(originalUser);

        // ACT
        User retrievedUser = sessionManager.getCurrentUser();

        // ASSERT
        assertNotNull("getCurrentUser should return user after session creation", retrievedUser);
        assertEquals("User ID should match", originalUser.getUserId(), retrievedUser.getUserId());
        assertEquals("Username should match", originalUser.getUsername(), retrievedUser.getUsername());
        assertEquals("Display name should match", originalUser.getDisplayName(), retrievedUser.getDisplayName());
    }

    // =============================================================================================
    // CURRENT USER ID RETRIEVAL TESTS (2 tests)
    // =============================================================================================

    /**
     * Tests that getCurrentUserId() returns -1 when no session exists (sentinel value).
     */
    @Test
    public void test_getCurrentUserId_withNoSession_returnsNegativeOne() {
        // ACT
        long userId = sessionManager.getCurrentUserId();

        // ASSERT
        assertEquals("getCurrentUserId should return -1 when no session exists", -1L, userId);
    }

    /**
     * Tests that getCurrentUserId() returns the user ID after createSession().
     */
    @Test
    public void test_getCurrentUserId_afterCreateSession_returnsUserId() {
        // ARRANGE
        User user = createTestUser(123L, "testuser", "Test User");
        sessionManager.createSession(user);

        // ACT
        long userId = sessionManager.getCurrentUserId();

        // ASSERT
        assertEquals("getCurrentUserId should return correct user ID", 123L, userId);
    }

    // =============================================================================================
    // LOGIN STATUS TESTS (2 tests)
    // =============================================================================================

    /**
     * Tests that isLoggedIn() returns false when no session exists.
     */
    @Test
    public void test_isLoggedIn_withNoSession_returnsFalse() {
        // ACT
        boolean isLoggedIn = sessionManager.isLoggedIn();

        // ASSERT
        assertFalse("isLoggedIn should return false when no session exists", isLoggedIn);
    }

    /**
     * Tests that isLoggedIn() returns true after createSession().
     */
    @Test
    public void test_isLoggedIn_afterCreateSession_returnsTrue() {
        // ARRANGE
        User user = createTestUser(1L, "testuser", "Test User");
        sessionManager.createSession(user);

        // ACT
        boolean isLoggedIn = sessionManager.isLoggedIn();

        // ASSERT
        assertTrue("isLoggedIn should return true after session creation", isLoggedIn);
    }

    // =============================================================================================
    // LOGOUT TESTS (1 test)
    // =============================================================================================

    /**
     * Tests that logout() clears the session completely.
     */
    @Test
    public void test_logout_clearsSession() {
        // ARRANGE
        User user = createTestUser(1L, "testuser", "Test User");
        sessionManager.createSession(user);
        assertTrue("Session should be created before logout", sessionManager.isLoggedIn());

        // ACT
        sessionManager.logout();

        // ASSERT
        assertFalse("isLoggedIn should return false after logout", sessionManager.isLoggedIn());
        assertNull("getCurrentUser should return null after logout", sessionManager.getCurrentUser());
        assertEquals("getCurrentUserId should return -1 after logout", -1L, sessionManager.getCurrentUserId());
    }

    // =============================================================================================
    // HELPER METHODS
    // =============================================================================================

    /**
     * Creates a test User object with minimal required fields.
     * Does NOT include sensitive data (passwordHash, salt).
     */
    private User createTestUser(long userId, String username, String displayName) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash("dummy_hash");  // Required field but not stored in session
        user.setSalt("dummy_salt");  // Required field but not stored in session
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        return user;
    }
}
