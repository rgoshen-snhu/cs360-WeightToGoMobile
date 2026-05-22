package com.example.weightogo.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import com.example.weightogo.models.User;
import com.example.weightogo.models.WeightEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for WeightEntryDAO.
 * Tests all CRUD operations using Robolectric for in-memory database.
 */
@RunWith(RobolectricTestRunner.class)
public class WeightEntryDAOTest {

    private WeighToGoDBHelper dbHelper;
    private WeightEntryDAO weightEntryDAO;
    private UserDAO userDAO;
    private long testUserId;

    @Before
    public void setUp() throws DatabaseException {
        Context context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        weightEntryDAO = new WeightEntryDAO(dbHelper);
        userDAO = new UserDAO(dbHelper);

        // Create a test user for foreign key relationships
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hash123");
        testUser.setSalt("salt123");
        testUser.setPasswordAlgorithm("SHA256");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setActive(true);

        testUserId = userDAO.insertUser(testUser);
        assertTrue("Test user should be created", testUserId > 0);
    }

    @After
    public void tearDown() {
        // Clean up test data
        if (testUserId > 0) {
            userDAO.deleteUser(testUserId);
        }
        // Don't close dbHelper - it's a singleton and other tests may need it
    }

    @Test
    public void test_insertWeightEntry_withValidData_returnsWeightId() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        entry.setUserId(testUserId);
        entry.setWeightValue(175.5);
        entry.setWeightUnit("lbs");
        entry.setWeightDate(LocalDate.now());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);

        // ASSERT
        assertTrue("Weight ID should be greater than 0", weightId > 0);
    }

    @Test
    public void test_insertWeightEntry_withNotes_savesNotes() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        entry.setUserId(testUserId);
        entry.setWeightValue(180.0);
        entry.setWeightUnit("lbs");
        entry.setWeightDate(LocalDate.now());
        entry.setNotes("Feeling great today!");
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Notes should match", "Feeling great today!", retrieved.getNotes());
    }

    @Test
    public void test_getWeightEntryById_withExistingEntry_returnsEntry() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        entry.setUserId(testUserId);
        entry.setWeightValue(160.5);
        entry.setWeightUnit("lbs");
        entry.setWeightDate(LocalDate.of(2025, 12, 10));
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(false);

        long weightId = weightEntryDAO.insertWeightEntry(entry);

        // ACT
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertNotNull("Entry should be found", retrieved);
        assertEquals("Weight ID should match", weightId, retrieved.getWeightId());
        assertEquals("Weight value should match", 160.5, retrieved.getWeightValue(), 0.01);
        assertEquals("Weight unit should match", "lbs", retrieved.getWeightUnit());
        assertEquals("Weight date should match", LocalDate.of(2025, 12, 10), retrieved.getWeightDate());
    }

    @Test
    public void test_getWeightEntryById_withNonExistentEntry_returnsNull() {
        // ACT
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(99999);

        // ASSERT
        assertNull("Non-existent entry should return null", retrieved);
    }

    @Test
    public void test_getWeightEntriesForUser_withMultipleEntries_returnsAllNonDeleted() {
        // ARRANGE
        WeightEntry entry1 = createTestEntry(testUserId, 170.0, LocalDate.of(2025, 12, 8), false);
        WeightEntry entry2 = createTestEntry(testUserId, 171.0, LocalDate.of(2025, 12, 9), false);
        WeightEntry entry3 = createTestEntry(testUserId, 172.0, LocalDate.of(2025, 12, 10), true); // Deleted

        weightEntryDAO.insertWeightEntry(entry1);
        weightEntryDAO.insertWeightEntry(entry2);
        weightEntryDAO.insertWeightEntry(entry3);

        // ACT
        List<WeightEntry> entries = weightEntryDAO.getWeightEntriesForUser(testUserId);

        // ASSERT
        assertEquals("Should return 2 non-deleted entries", 2, entries.size());
        // Entries should be ordered by date DESC (most recent first)
        assertEquals("First entry should be most recent", LocalDate.of(2025, 12, 9), entries.get(0).getWeightDate());
        assertEquals("Second entry should be older", LocalDate.of(2025, 12, 8), entries.get(1).getWeightDate());
    }

    @Test
    public void test_getWeightEntriesForUser_withNoEntries_returnsEmptyList() throws DatabaseException {
        // Create a second user with no entries
        User user2 = new User();
        user2.setUsername("user2_entries_" + System.currentTimeMillis());
        user2.setPasswordHash("hash");
        user2.setSalt("salt");
        user2.setPasswordAlgorithm("SHA256");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        user2.setActive(true);
        long user2Id = userDAO.insertUser(user2);

        // ACT
        List<WeightEntry> entries = weightEntryDAO.getWeightEntriesForUser(user2Id);

        // ASSERT
        assertNotNull("List should not be null", entries);
        assertEquals("List should be empty", 0, entries.size());
    }

    @Test
    public void test_getLatestWeightEntry_withMultipleEntries_returnsMostRecent() {
        // ARRANGE
        WeightEntry entry1 = createTestEntry(testUserId, 170.0, LocalDate.of(2025, 12, 8), false);
        WeightEntry entry2 = createTestEntry(testUserId, 171.0, LocalDate.of(2025, 12, 9), false);
        WeightEntry entry3 = createTestEntry(testUserId, 172.0, LocalDate.of(2025, 12, 10), false);

        weightEntryDAO.insertWeightEntry(entry1);
        weightEntryDAO.insertWeightEntry(entry2);
        weightEntryDAO.insertWeightEntry(entry3);

        // ACT
        WeightEntry latest = weightEntryDAO.getLatestWeightEntry(testUserId);

        // ASSERT
        assertNotNull("Latest entry should be found", latest);
        assertEquals("Latest entry should have most recent date", LocalDate.of(2025, 12, 10), latest.getWeightDate());
        assertEquals("Latest entry should have correct weight", 172.0, latest.getWeightValue(), 0.01);
    }

    @Test
    public void test_getLatestWeightEntry_withNoEntries_returnsNull() throws DatabaseException {
        // Create a second user with no entries
        User user2 = new User();
        user2.setUsername("user2_latest_" + System.currentTimeMillis());
        user2.setPasswordHash("hash");
        user2.setSalt("salt");
        user2.setPasswordAlgorithm("SHA256");
        user2.setCreatedAt(LocalDateTime.now());
        user2.setUpdatedAt(LocalDateTime.now());
        user2.setActive(true);
        long user2Id = userDAO.insertUser(user2);

        // ACT
        WeightEntry latest = weightEntryDAO.getLatestWeightEntry(user2Id);

        // ASSERT
        assertNull("Latest entry should be null for user with no entries", latest);
    }

    @Test
    public void test_updateWeightEntry_withValidData_updatesEntry() {
        // ARRANGE
        WeightEntry entry = createTestEntry(testUserId, 175.0, LocalDate.now(), false);
        long weightId = weightEntryDAO.insertWeightEntry(entry);

        entry.setWeightId(weightId);
        entry.setWeightValue(176.5);
        entry.setNotes("Updated notes");

        // ACT
        int rowsUpdated = weightEntryDAO.updateWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsUpdated);
        assertNotNull("Entry should still exist", retrieved);
        assertEquals("Weight value should be updated", 176.5, retrieved.getWeightValue(), 0.01);
        assertEquals("Notes should be updated", "Updated notes", retrieved.getNotes());
    }

    @Test
    public void test_deleteWeightEntry_softDeletes_setsIsDeletedFlag() {
        // ARRANGE
        WeightEntry entry = createTestEntry(testUserId, 175.0, LocalDate.now(), false);
        long weightId = weightEntryDAO.insertWeightEntry(entry);

        // ACT
        int rowsDeleted = weightEntryDAO.deleteWeightEntry(weightId);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);
        List<WeightEntry> userEntries = weightEntryDAO.getWeightEntriesForUser(testUserId);

        // ASSERT
        assertEquals("Should update 1 row", 1, rowsDeleted);
        assertNotNull("Entry should still exist in database", retrieved);
        assertTrue("Entry should be marked as deleted", retrieved.isDeleted());
        assertEquals("Deleted entry should not appear in user's list", 0, userEntries.size());
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    public void test_insertWeightEntry_withDuplicateUserIdAndDate_violatesUniqueConstraint() {
        // ARRANGE
        LocalDate sameDate = LocalDate.of(2025, 12, 10);
        WeightEntry entry1 = createTestEntry(testUserId, 170.0, sameDate, false);
        WeightEntry entry2 = createTestEntry(testUserId, 175.0, sameDate, false);

        // ACT
        long firstId = weightEntryDAO.insertWeightEntry(entry1);
        long secondId = weightEntryDAO.insertWeightEntry(entry2); // Should fail or return -1

        // ASSERT
        assertTrue("First entry should be inserted successfully", firstId > 0);
        assertEquals("Second entry with duplicate user_id + date should fail", -1, secondId);
    }

    @Test
    public void test_insertWeightEntry_withInvalidUserId_violatesForeignKey() {
        // ARRANGE
        long invalidUserId = 99999; // Non-existent user
        WeightEntry entry = createTestEntry(invalidUserId, 175.0, LocalDate.now(), false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);

        // ASSERT
        assertEquals("Insert with invalid user_id should fail due to foreign key constraint", -1, weightId);
    }

    @Test
    public void test_updateWeightEntry_withNonExistentEntry_returnsZero() {
        // ARRANGE
        WeightEntry nonExistent = createTestEntry(testUserId, 175.0, LocalDate.now(), false);
        nonExistent.setWeightId(99999); // Non-existent ID

        // ACT
        int rowsUpdated = weightEntryDAO.updateWeightEntry(nonExistent);

        // ASSERT
        assertEquals("Updating non-existent entry should return 0 rows updated", 0, rowsUpdated);
    }

    @Test
    public void test_deleteWeightEntry_withNonExistentEntry_returnsZero() {
        // ACT
        int rowsDeleted = weightEntryDAO.deleteWeightEntry(99999);

        // ASSERT
        assertEquals("Deleting non-existent entry should return 0 rows updated", 0, rowsDeleted);
    }

    @Test
    public void test_insertWeightEntry_withSpecialCharactersInNotes_savesCorrectly() {
        // ARRANGE
        WeightEntry entry = createTestEntry(testUserId, 170.0, LocalDate.now(), false);
        entry.setNotes("SQL injection attempt: '; DROP TABLE daily_weights; -- \nEmojis: 🎉💪🏋️\nUnicode: 你好世界");

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Entry should be inserted", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Special characters should be preserved", entry.getNotes(), retrieved.getNotes());
    }

    @Test
    public void test_insertWeightEntry_withVeryLongNotes_savesCorrectly() {
        // ARRANGE
        StringBuilder longNotes = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longNotes.append("This is a very long note. ");
        }
        WeightEntry entry = createTestEntry(testUserId, 170.0, LocalDate.now(), false);
        entry.setNotes(longNotes.toString());

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Entry should be inserted", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Long notes should be preserved", entry.getNotes(), retrieved.getNotes());
    }

    @Test
    public void test_insertWeightEntry_withNegativeWeight_insertsSuccessfully() {
        // ARRANGE - Negative weight is technically valid (database allows it, app should validate)
        WeightEntry entry = createTestEntry(testUserId, -10.0, LocalDate.now(), false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Database should allow negative weight (app validation should prevent this)", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Negative weight should be preserved", -10.0, retrieved.getWeightValue(), 0.01);
    }

    @Test
    public void test_insertWeightEntry_withZeroWeight_insertsSuccessfully() {
        // ARRANGE
        WeightEntry entry = createTestEntry(testUserId, 0.0, LocalDate.now(), false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Database should allow zero weight (app validation should prevent this)", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Zero weight should be preserved", 0.0, retrieved.getWeightValue(), 0.01);
    }

    @Test
    public void test_insertWeightEntry_withExtremelyLargeWeight_insertsSuccessfully() {
        // ARRANGE
        WeightEntry entry = createTestEntry(testUserId, 999999.99, LocalDate.now(), false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Database should allow extremely large weight", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Large weight should be preserved", 999999.99, retrieved.getWeightValue(), 0.01);
    }

    @Test
    public void test_insertWeightEntry_withFarPastDate_insertsSuccessfully() {
        // ARRANGE
        LocalDate farPast = LocalDate.of(1900, 1, 1);
        WeightEntry entry = createTestEntry(testUserId, 170.0, farPast, false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Database should allow far past dates", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Far past date should be preserved", farPast, retrieved.getWeightDate());
    }

    @Test
    public void test_insertWeightEntry_withFarFutureDate_insertsSuccessfully() {
        // ARRANGE
        LocalDate farFuture = LocalDate.of(2099, 12, 31);
        WeightEntry entry = createTestEntry(testUserId, 170.0, farFuture, false);

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Database should allow far future dates", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Far future date should be preserved", farFuture, retrieved.getWeightDate());
    }

    @Test
    public void test_insertWeightEntry_withEmptyStringNotes_savesEmpty() {
        // ARRANGE
        WeightEntry entry = createTestEntry(testUserId, 170.0, LocalDate.now(), false);
        entry.setNotes(""); // Empty string (not null)

        // ACT
        long weightId = weightEntryDAO.insertWeightEntry(entry);
        WeightEntry retrieved = weightEntryDAO.getWeightEntryById(weightId);

        // ASSERT
        assertTrue("Entry should be inserted", weightId > 0);
        assertNotNull("Entry should be retrieved", retrieved);
        assertEquals("Empty notes should be preserved", "", retrieved.getNotes());
    }

    @Test
    public void test_getLatestWeightEntry_excludesDeletedEntries() {
        // ARRANGE
        WeightEntry entry1 = createTestEntry(testUserId, 170.0, LocalDate.of(2025, 12, 8), false);
        WeightEntry entry2 = createTestEntry(testUserId, 171.0, LocalDate.of(2025, 12, 9), false);
        WeightEntry entry3 = createTestEntry(testUserId, 172.0, LocalDate.of(2025, 12, 10), true); // Most recent but deleted

        weightEntryDAO.insertWeightEntry(entry1);
        weightEntryDAO.insertWeightEntry(entry2);
        weightEntryDAO.insertWeightEntry(entry3);

        // ACT
        WeightEntry latest = weightEntryDAO.getLatestWeightEntry(testUserId);

        // ASSERT
        assertNotNull("Latest entry should be found", latest);
        assertEquals("Latest non-deleted entry should be from 12/9", LocalDate.of(2025, 12, 9), latest.getWeightDate());
        assertEquals("Latest weight should be 171.0", 171.0, latest.getWeightValue(), 0.01);
    }

    // Helper method to create test entries
    private WeightEntry createTestEntry(long userId, double weight, LocalDate date, boolean isDeleted) {
        WeightEntry entry = new WeightEntry();
        entry.setUserId(userId);
        entry.setWeightValue(weight);
        entry.setWeightUnit("lbs");
        entry.setWeightDate(date);
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        entry.setDeleted(isDeleted);
        return entry;
    }
}
