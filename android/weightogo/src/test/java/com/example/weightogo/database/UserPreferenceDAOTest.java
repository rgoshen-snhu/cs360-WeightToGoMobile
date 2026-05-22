package com.example.weightogo.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.example.weightogo.models.User;
import com.example.weightogo.models.UserPreference;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDateTime;

/**
 * Test class for UserPreferenceDAO using Robolectric for Android SQLite testing.
 *
 * Follows strict Test-Driven Development (TDD) methodology:
 * - Tests written BEFORE implementation
 * - Red-Green-Refactor cycle
 * - 100% test coverage
 *
 * Tests the generic key-value preference storage with UPSERT pattern.
 */
@RunWith(RobolectricTestRunner.class)
public class UserPreferenceDAOTest {

    private WeighToGoDBHelper dbHelper;
    private UserPreferenceDAO userPreferenceDAO;
    private UserDAO userDAO;
    private long testUserId;

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        userPreferenceDAO = new UserPreferenceDAO(dbHelper);
        userDAO = new UserDAO(dbHelper);

        // Create test user for foreign key relationships
        User testUser = createTestUser("testuser");
        try {
            testUserId = userDAO.insertUser(testUser);
            assertTrue("Test user should be created", testUserId > 0);
        } catch (DatabaseException e) {
            throw new RuntimeException("Failed to create test user", e);
        }
    }

    @After
    public void tearDown() {
        if (testUserId > 0) {
            userDAO.deleteUser(testUserId);  // Cascade deletes preferences
        }
        WeighToGoDBHelper.resetInstance();
    }

    /**
     * Helper method to create a test user.
     *
     * @param username the username for the test user
     * @return a User object with valid data
     */
    private User createTestUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash("hash123");
        user.setSalt("salt123");
        user.setPasswordAlgorithm("SHA256");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        return user;
    }

    // Tests will be added here following strict TDD (one at a time)

    /**
     * Test 1: GET with non-existent key returns default value.
     *
     * Tests FR6.0.1 - UserPreferenceDAO generic key-value storage.
     * Verifies that getPreference() returns the provided default value
     * when the requested key doesn't exist in the database.
     */
    @Test
    public void test_getPreference_withNonExistentKey_returnsDefaultValue() {
        // ACT
        String result = userPreferenceDAO.getPreference(testUserId, "theme", "light");

        // ASSERT
        assertEquals("Should return default when key doesn't exist", "light", result);
    }

    /**
     * Test 2: SET with valid data returns true.
     *
     * Tests FR6.0.1 - UserPreferenceDAO UPSERT functionality.
     * Verifies that setPreference() successfully inserts a new preference
     * and returns true on success.
     */
    @Test
    public void test_setPreference_withValidData_returnsTrue() {
        // ACT
        boolean result = userPreferenceDAO.setPreference(testUserId, "theme", "dark");

        // ASSERT
        assertTrue("Should return true on successful insert", result);
    }

    /**
     * Test 3: SET then GET returns correct value.
     *
     * Tests FR6.0.1 - UserPreferenceDAO round-trip functionality.
     * Verifies that a value set with setPreference() can be retrieved
     * correctly with getPreference().
     */
    @Test
    public void test_setPreference_thenGet_returnsCorrectValue() {
        // ARRANGE
        userPreferenceDAO.setPreference(testUserId, "theme", "dark");

        // ACT
        String result = userPreferenceDAO.getPreference(testUserId, "theme", "light");

        // ASSERT
        assertEquals("Should return stored value", "dark", result);
    }

    /**
     * Test 4: SET twice updates value (UPSERT pattern).
     *
     * Tests FR6.0.1 - UserPreferenceDAO UPSERT functionality.
     * Verifies that calling setPreference() twice with the same key
     * updates the value instead of creating a duplicate row.
     * This is critical to ensure INSERT OR REPLACE works correctly.
     */
    @Test
    public void test_setPreference_twice_updatesValue() {
        // ARRANGE
        userPreferenceDAO.setPreference(testUserId, "theme", "dark");

        // ACT - Update same key
        userPreferenceDAO.setPreference(testUserId, "theme", "light");
        String result = userPreferenceDAO.getPreference(testUserId, "theme", "system");

        // ASSERT
        assertEquals("Should return updated value", "light", result);

        // Verify only one row exists (no duplicate keys)
        List<UserPreference> allPrefs = userPreferenceDAO.getAllPreferences(testUserId);
        long themeCount = allPrefs.stream()
                .filter(p -> p.getPrefKey().equals("theme"))
                .count();
        assertEquals("Should only have one 'theme' preference", 1L, themeCount);
    }

    /**
     * Test 5: GET weight unit with no preference returns default "lbs".
     *
     * Tests FR6.0.1 - Weight unit convenience method.
     * Verifies that getWeightUnit() returns "lbs" as the default
     * when no weight_unit preference exists.
     */
    @Test
    public void test_getWeightUnit_withNoPreference_returnsDefaultLbs() {
        // ACT
        String unit = userPreferenceDAO.getWeightUnit(testUserId);

        // ASSERT
        assertEquals("Should default to lbs", "lbs", unit);
    }

    /**
     * Test 6: SET weight unit "lbs" returns true.
     *
     * Tests FR6.0.1 - Weight unit validation.
     * Verifies that setWeightUnit() accepts "lbs" as a valid unit.
     */
    @Test
    public void test_setWeightUnit_withValidLbs_returnsTrue() {
        // ACT
        boolean result = userPreferenceDAO.setWeightUnit(testUserId, "lbs");

        // ASSERT
        assertTrue("Should accept 'lbs' as valid unit", result);
    }

    /**
     * Test 7: SET weight unit "kg" returns true.
     *
     * Tests FR6.0.1 - Weight unit validation.
     * Verifies that setWeightUnit() accepts "kg" as a valid unit.
     */
    @Test
    public void test_setWeightUnit_withValidKg_returnsTrue() {
        // ACT
        boolean result = userPreferenceDAO.setWeightUnit(testUserId, "kg");

        // ASSERT
        assertTrue("Should accept 'kg' as valid unit", result);
    }

    /**
     * Test 8: SET weight unit with invalid value returns false.
     *
     * Tests FR6.0.1 - Weight unit validation.
     * Verifies that setWeightUnit() rejects invalid units (case-sensitive).
     * Only "lbs" and "kg" should be accepted.
     */
    @Test
    public void test_setWeightUnit_withInvalidUnit_returnsFalse() {
        // ARRANGE
        String[] invalidUnits = {"pounds", "kilograms", "LBS", "KG", "grams", ""};

        // ACT & ASSERT
        for (String invalidUnit : invalidUnits) {
            boolean result = userPreferenceDAO.setWeightUnit(testUserId, invalidUnit);
            assertFalse("Should reject invalid unit: " + invalidUnit, result);
        }
    }

    /**
     * Test 9: SET weight unit then GET returns correct unit.
     *
     * Tests FR6.0.1 - Weight unit round-trip functionality.
     * Verifies that a weight unit set with setWeightUnit() can be
     * retrieved correctly with getWeightUnit().
     */
    @Test
    public void test_setWeightUnit_thenGet_returnsCorrectUnit() {
        // ARRANGE
        userPreferenceDAO.setWeightUnit(testUserId, "kg");

        // ACT
        String unit = userPreferenceDAO.getWeightUnit(testUserId);

        // ASSERT
        assertEquals("Should return stored unit", "kg", unit);
    }

    /**
     * Test 10: GET preference isolates data by user.
     *
     * Tests FR6.0.1 - Multi-user data isolation.
     * Verifies that preferences are isolated per user - different users
     * can have different values for the same preference key.
     */
    @Test
    public void test_getPreference_withMultipleUsers_isolatesData() {
        // ARRANGE - Create second user
        User user2 = createTestUser("user2");
        long user2Id = 0;
        try {
            user2Id = userDAO.insertUser(user2);

            // Set different preferences for each user
            userPreferenceDAO.setWeightUnit(testUserId, "lbs");
            userPreferenceDAO.setWeightUnit(user2Id, "kg");

            // ACT
            String user1Unit = userPreferenceDAO.getWeightUnit(testUserId);
            String user2Unit = userPreferenceDAO.getWeightUnit(user2Id);

            // ASSERT
            assertEquals("User 1 should have lbs", "lbs", user1Unit);
            assertEquals("User 2 should have kg", "kg", user2Unit);

        } catch (DatabaseException e) {
            throw new RuntimeException("Failed to create second test user", e);
        } finally {
            // Cleanup
            if (user2Id > 0) {
                userDAO.deleteUser(user2Id);
            }
        }
    }
}
