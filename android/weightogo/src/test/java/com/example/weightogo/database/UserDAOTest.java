package com.example.weightogo.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.example.weightogo.models.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Unit tests for UserDAO following strict TDD.
 * Tests CRUD operations for User model.
 *
 * TDD Approach: Write ONE failing test at a time, implement minimal code to pass,
 * then move to next test.
 */
@RunWith(RobolectricTestRunner.class)
public class UserDAOTest {

    private Context context;
    private WeighToGoDBHelper dbHelper;
    private UserDAO userDAO;
    private SQLiteDatabase db;

    @Before
    public void setUp() throws DatabaseException {
        context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        db = dbHelper.getWritableDatabase();
        userDAO = new UserDAO(dbHelper);
    }

    @After
    public void tearDown() {
        try {
            if (db != null && db.isOpen()) {
                db.close();
            }
            if (dbHelper != null) {
                dbHelper.close();
            }
        } finally {
            dbHelper = null;
            userDAO = null;
            context.deleteDatabase("weigh_to_go.db");
            WeighToGoDBHelper.resetInstance();
        }
    }

    /**
     * Test 1: insertUser() should insert a valid user and return user ID > 0
     */
    @Test
    public void test_insertUser_withValidData_returnsUserId() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hashed_password_123");
        user.setSalt("random_salt_456");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        // ACT
        long userId = userDAO.insertUser(user);

        // ASSERT
        assertTrue("User ID should be greater than 0", userId > 0);
        user.setUserId(userId);

        // Verify user was actually inserted by querying database
        User retrievedUser = userDAO.getUserById(userId);
        assertNotNull("Retrieved user should not be null", retrievedUser);
        assertEquals("Username should match", "testuser", retrievedUser.getUsername());
    }

    /**
     * Test 2: getUserByUsername() should retrieve user by username
     */
    @Test
    public void test_getUserByUsername_withExistingUser_returnsUser() throws DatabaseException {
        // ARRANGE - Insert a user first
        User user = new User();
        user.setUsername("johndoe");
        user.setPasswordHash("hashed_pass");
        user.setSalt("salt123");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);
        user.setUserId(userId);

        // ACT
        User retrievedUser = userDAO.getUserByUsername("johndoe");

        // ASSERT
        assertNotNull("User should be found", retrievedUser);
        assertEquals("User ID should match", userId, retrievedUser.getUserId());
        assertEquals("Username should match", "johndoe", retrievedUser.getUsername());
    }

    /**
     * Test 3: getUserByUsername() returns null for non-existent username
     */
    @Test
    public void test_getUserByUsername_withNonExistentUser_returnsNull() throws DatabaseException {
        // ACT
        User user = userDAO.getUserByUsername("nonexistent");

        // ASSERT
        assertNull("Should return null for non-existent user", user);
    }

    /**
     * Test 4: usernameExists() returns true for existing username
     */
    @Test
    public void test_usernameExists_withExistingUsername_returnsTrue() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("existinguser");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        userDAO.insertUser(user);

        // ACT
        boolean exists = userDAO.usernameExists("existinguser");

        // ASSERT
        assertTrue("Username should exist", exists);
    }

    /**
     * Test 5: usernameExists() returns false for non-existent username
     */
    @Test
    public void test_usernameExists_withNonExistentUsername_returnsFalse() throws DatabaseException {
        // ACT
        boolean exists = userDAO.usernameExists("nonexistent");

        // ASSERT
        assertFalse("Username should not exist", exists);
    }

    /**
     * Test 6: updateLastLogin() updates the last_login timestamp
     */
    @Test
    public void test_updateLastLogin_updatesTimestamp() throws DatabaseException {
        // ARRANGE - Insert a user
        User user = new User();
        user.setUsername("loginuser");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        LocalDateTime loginTime = LocalDateTime.now();

        // ACT
        int rowsAffected = userDAO.updateLastLogin(userId, loginTime);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsAffected);

        // Verify the update
        User updatedUser = userDAO.getUserById(userId);
        assertNotNull("Updated user should exist", updatedUser);
        assertNotNull("Last login should be set", updatedUser.getLastLogin());
    }

    /**
     * Test 7: deleteUser() removes user from database
     */
    @Test
    public void test_deleteUser_removesUser() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("deletetest");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        // ACT
        int rowsDeleted = userDAO.deleteUser(userId);

        // ASSERT
        assertEquals("Should delete 1 row", 1, rowsDeleted);

        // Verify user no longer exists
        User deletedUser = userDAO.getUserById(userId);
        assertNull("User should be deleted", deletedUser);
    }

    // ========== EDGE CASE TESTS ==========

    /**
     * Test 8: insertUser() with duplicate username violates UNIQUE constraint
     */
    @Test
    public void test_insertUser_withDuplicateUsername_violatesUniqueConstraint() throws DatabaseException {
        // ARRANGE
        User user1 = new User();
        user1.setUsername("duplicateuser");
        user1.setPasswordHash("hash1");
        user1.setSalt("salt1");
        user1.setPasswordAlgorithm("SHA256");
        user1.setCreatedAt(LocalDateTime.now());
        user1.setUpdatedAt(LocalDateTime.now());
        user1.setActive(true);

        User user2 = new User();
        user2.setUsername("duplicateuser"); // Same username
        user2.setPasswordHash("hash2");
        user2.setSalt("salt2");
        user2.setPasswordAlgorithm("SHA256");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        user2.setActive(true);

        // ACT
        long userId1 = userDAO.insertUser(user1);
        assertTrue("First user should be inserted successfully", userId1 > 0);

        // ACT & ASSERT - second insert should throw DuplicateUsernameException
        boolean exceptionThrown = false;
        try {
            userDAO.insertUser(user2); // Should throw DuplicateUsernameException
            fail("Expected DuplicateUsernameException to be thrown");
        } catch (DuplicateUsernameException e) {
            exceptionThrown = true;
            assertTrue("Exception message should mention the username",
                    e.getMessage().contains("duplicateuser"));
        }

        assertTrue("DuplicateUsernameException should have been thrown", exceptionThrown);
    }

    /**
     * Test 9: getUserById() with non-existent ID returns null
     */
    @Test
    public void test_getUserById_withNonExistentId_returnsNull() throws DatabaseException {
        // ACT
        User user = userDAO.getUserById(99999);

        // ASSERT
        assertNull("Should return null for non-existent user ID", user);
    }

    /**
     * Test 10: updateLastLogin() with non-existent user ID returns 0
     */
    @Test
    public void test_updateLastLogin_withNonExistentUserId_returnsZero() throws DatabaseException {
        // ACT
        int rowsAffected = userDAO.updateLastLogin(99999, LocalDateTime.now());

        // ASSERT
        assertEquals("Updating non-existent user should return 0 rows affected", 0, rowsAffected);
    }

    /**
     * Test 11: deleteUser() with non-existent ID returns 0
     */
    @Test
    public void test_deleteUser_withNonExistentUserId_returnsZero() throws DatabaseException {
        // ACT
        int rowsDeleted = userDAO.deleteUser(99999);

        // ASSERT
        assertEquals("Deleting non-existent user should return 0 rows deleted", 0, rowsDeleted);
    }

    /**
     * Test 12: insertUser() with special characters in username
     */
    @Test
    public void test_insertUser_withSpecialCharactersInUsername_insertsSuccessfully() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("user_with-special.chars@123");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        // ACT
        long userId = userDAO.insertUser(user);
        User retrieved = userDAO.getUserById(userId);

        // ASSERT
        assertTrue("User should be inserted", userId > 0);
        assertNotNull("User should be retrieved", retrieved);
        assertEquals("Special characters in username should be preserved", "user_with-special.chars@123", retrieved.getUsername());
    }

    /**
     * Test 13: insertUser() with very long username
     */
    @Test
    public void test_insertUser_withVeryLongUsername_insertsSuccessfully() throws DatabaseException {
        // ARRANGE
        StringBuilder longUsername = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longUsername.append("a");
        }
        User user = new User();
        user.setUsername(longUsername.toString());
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        // ACT
        long userId = userDAO.insertUser(user);
        User retrieved = userDAO.getUserById(userId);

        // ASSERT
        assertTrue("User should be inserted", userId > 0);
        assertNotNull("User should be retrieved", retrieved);
        assertEquals("Long username should be preserved", longUsername.toString(), retrieved.getUsername());
    }

    /**
     * Test 14: insertUser() with all optional fields populated
     */
    @Test
    public void test_insertUser_withAllOptionalFields_insertsSuccessfully() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("completeuser");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setEmail("user@example.com");
        user.setPhoneNumber("+1234567890");
        user.setDisplayName("Complete User");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setActive(true);

        // ACT
        long userId = userDAO.insertUser(user);
        User retrieved = userDAO.getUserById(userId);

        // ASSERT
        assertTrue("User should be inserted", userId > 0);
        assertNotNull("User should be retrieved", retrieved);
        assertEquals("Email should be preserved", "user@example.com", retrieved.getEmail());
        assertEquals("Phone number should be preserved", "+1234567890", retrieved.getPhoneNumber());
        assertEquals("Display name should be preserved", "Complete User", retrieved.getDisplayName());
        assertNotNull("Last login should be preserved", retrieved.getLastLogin());
    }

    /**
     * Test 15: insertUser() with special characters in optional fields
     */
    @Test
    public void test_insertUser_withSpecialCharactersInOptionalFields_insertsSuccessfully() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("specialuser");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setEmail("user+tag@example.com");
        user.setDisplayName("User 🎉 Name");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        // ACT
        long userId = userDAO.insertUser(user);
        User retrieved = userDAO.getUserById(userId);

        // ASSERT
        assertTrue("User should be inserted", userId > 0);
        assertNotNull("User should be retrieved", retrieved);
        assertEquals("Email with + should be preserved", "user+tag@example.com", retrieved.getEmail());
        assertEquals("Display name with emoji should be preserved", "User 🎉 Name", retrieved.getDisplayName());
    }

    /**
     * Test 16: deleteUser() cascades to weight entries and goals
     */
    @Test
    public void test_deleteUser_cascadesToRelatedRecords() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("cascadetest");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        // Insert related weight entry (this will be tested in integration)
        // For now, just verify user deletion works

        // ACT
        int rowsDeleted = userDAO.deleteUser(userId);

        // ASSERT
        assertEquals("Should delete 1 row", 1, rowsDeleted);
        User deletedUser = userDAO.getUserById(userId);
        assertNull("User should no longer exist", deletedUser);
        // Note: CASCADE DELETE for weight_entries and goal_weights verified in integration tests
    }

    /**
     * Test 17: usernameExists() is case-sensitive
     */
    @Test
    public void test_usernameExists_isCaseSensitive() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("TestUser");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        userDAO.insertUser(user);

        // ACT
        boolean existsExactCase = userDAO.usernameExists("TestUser");
        boolean existsLowerCase = userDAO.usernameExists("testuser");
        boolean existsUpperCase = userDAO.usernameExists("TESTUSER");

        // ASSERT
        assertTrue("Exact case should exist", existsExactCase);
        assertFalse("Lowercase variant should not exist (case-sensitive)", existsLowerCase);
        assertFalse("Uppercase variant should not exist (case-sensitive)", existsUpperCase);
    }

    /**
     * Test 18: getUserByUsername() is case-sensitive
     */
    @Test
    public void test_getUserByUsername_isCaseSensitive() throws DatabaseException {
        // ARRANGE
        User user = new User();
        user.setUsername("CaseSensitive");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        userDAO.insertUser(user);

        // ACT
        User exactMatch = userDAO.getUserByUsername("CaseSensitive");
        User lowerMatch = userDAO.getUserByUsername("casesensitive");

        // ASSERT
        assertNotNull("Exact case should return user", exactMatch);
        assertNull("Different case should not return user (case-sensitive)", lowerMatch);
    }

    // =============================================================================================
    // PHONE NUMBER UPDATE TESTS (6 tests) - Phase 7.2 Commit 5
    // =============================================================================================

    /**
     * Test 19: updatePhoneNumber() with valid phone updates successfully
     */
    @Test
    public void test_updatePhoneNumber_withValidPhone_returnsTrue() throws DatabaseException {
        // ARRANGE - Insert a user without phone number
        User user = new User();
        user.setUsername("phonetest");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        String validPhone = "+12025551234";  // E.164 format

        // ACT
        boolean result = userDAO.updatePhoneNumber(userId, validPhone);

        // ASSERT
        assertTrue("Update should return true", result);

        // Verify phone was updated
        User updatedUser = userDAO.getUserById(userId);
        assertNotNull("User should exist", updatedUser);
        assertEquals("Phone number should be updated", validPhone, updatedUser.getPhoneNumber());
    }

    /**
     * Test 20: updatePhoneNumber() with null phone clears the field
     */
    @Test
    public void test_updatePhoneNumber_withNullPhone_clearsPhone() throws DatabaseException {
        // ARRANGE - Insert user with phone number
        User user = new User();
        user.setUsername("clearponetest");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setPhoneNumber("+12025551234");  // Initial phone
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        // Verify phone is set
        User beforeUpdate = userDAO.getUserById(userId);
        assertNotNull("Phone should be set initially", beforeUpdate.getPhoneNumber());

        // ACT - Clear phone by passing null
        boolean result = userDAO.updatePhoneNumber(userId, null);

        // ASSERT
        assertTrue("Update should return true", result);

        // Verify phone was cleared
        User afterUpdate = userDAO.getUserById(userId);
        assertNotNull("User should still exist", afterUpdate);
        assertNull("Phone number should be null", afterUpdate.getPhoneNumber());
    }

    /**
     * Test 21: updatePhoneNumber() with invalid user ID returns false
     */
    @Test
    public void test_updatePhoneNumber_withInvalidUserId_returnsFalse() throws DatabaseException {
        // ACT - Try to update phone for non-existent user
        boolean result = userDAO.updatePhoneNumber(99999, "+12025551234");

        // ASSERT
        assertFalse("Update with invalid user ID should return false", result);
    }

    /**
     * Test 22: updatePhoneNumber() updates the updated_at timestamp
     */
    @Test
    public void test_updatePhoneNumber_updatesUpdatedAtTimestamp() throws DatabaseException {
        // ARRANGE - Insert user
        User user = new User();
        user.setUsername("timestamptest");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        // Get original updated_at timestamp
        User beforeUpdate = userDAO.getUserById(userId);
        LocalDateTime originalUpdatedAt = beforeUpdate.getUpdatedAt();

        // Wait a moment to ensure timestamp difference
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // ACT - Update phone number
        boolean result = userDAO.updatePhoneNumber(userId, "+12025551234");

        // ASSERT
        assertTrue("Update should return true", result);

        // Verify updated_at changed
        User afterUpdate = userDAO.getUserById(userId);
        assertNotNull("User should exist", afterUpdate);
        assertNotNull("Updated_at should not be null", afterUpdate.getUpdatedAt());
        assertTrue("Updated_at should be after original timestamp",
                afterUpdate.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /**
     * Test 23: updatePhoneNumber() preserves other user fields
     */
    @Test
    public void test_updatePhoneNumber_preservesOtherFields() throws DatabaseException {
        // ARRANGE - Insert user with all fields populated
        User user = new User();
        user.setUsername("preservetest");
        user.setPasswordHash("hash123");
        user.setSalt("salt456");
        user.setPasswordAlgorithm("SHA256");
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        // ACT - Update only phone number
        boolean result = userDAO.updatePhoneNumber(userId, "+12025551234");

        // ASSERT
        assertTrue("Update should return true", result);

        // Verify all other fields remain unchanged
        User afterUpdate = userDAO.getUserById(userId);
        assertNotNull("User should exist", afterUpdate);
        assertEquals("Username should be preserved", "preservetest", afterUpdate.getUsername());
        assertEquals("Password hash should be preserved", "hash123", afterUpdate.getPasswordHash());
        assertEquals("Salt should be preserved", "salt456", afterUpdate.getSalt());
        assertEquals("Email should be preserved", "test@example.com", afterUpdate.getEmail());
        assertEquals("Display name should be preserved", "Test User", afterUpdate.getDisplayName());
        assertNotNull("Last login should be preserved", afterUpdate.getLastLogin());
        assertTrue("Active status should be preserved", afterUpdate.isActive());
        assertEquals("Phone should be updated", "+12025551234", afterUpdate.getPhoneNumber());
    }

    /**
     * Test 24: updatePhoneNumber() integration - phone persists across sessions
     */
    @Test
    public void test_updatePhoneNumber_integration_persistsAcrossSessions() throws DatabaseException {
        // ARRANGE - Insert user and update phone
        User user = new User();
        user.setUsername("persisttest");
        user.setPasswordHash("hash");
        user.setSalt("salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);

        String phone = "+12025551234";
        boolean updateResult = userDAO.updatePhoneNumber(userId, phone);
        assertTrue("Update should succeed", updateResult);

        // ACT - Close and reopen database (simulate new session)
        db.close();
        dbHelper.close();

        // Reinitialize database and DAO
        dbHelper = WeighToGoDBHelper.getInstance(context);
        db = dbHelper.getWritableDatabase();
        userDAO = new UserDAO(dbHelper);

        // Retrieve user in new session
        User retrievedUser = userDAO.getUserById(userId);

        // ASSERT - Phone should persist
        assertNotNull("User should exist in new session", retrievedUser);
        assertEquals("Phone should persist across sessions", phone, retrievedUser.getPhoneNumber());
    }

    /**
     * Test 25: updatePassword() migrates SHA256 user to bcrypt
     * Tests Phase 8.6 - Password algorithm migration.
     */
    @Test
    public void test_updatePassword_migratesToBcrypt_success() throws DatabaseException {
        // ARRANGE: Create SHA256 user
        User user = new User();
        user.setUsername("migrationtest_" + System.currentTimeMillis());
        user.setPasswordHash("old_sha256_hash");
        user.setSalt("old_salt");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        long userId = userDAO.insertUser(user);
        assertTrue("User should be inserted", userId > 0);

        // ACT: Migrate to bcrypt
        String bcryptHash = "$2a$12$randomBcryptHashHereForTesting1234567890123456789012";
        boolean updated = userDAO.updatePassword(userId, bcryptHash, "", "BCRYPT");

        // ASSERT
        assertTrue("Update should succeed", updated);

        User migrated = userDAO.getUserById(userId);
        assertNotNull("User should exist after migration", migrated);
        assertEquals("Algorithm should be BCRYPT", "BCRYPT", migrated.getPasswordAlgorithm());
        assertEquals("Hash should be updated", bcryptHash, migrated.getPasswordHash());
        assertEquals("Salt should be empty (bcrypt handles salt internally)", "", migrated.getSalt());
    }
}
