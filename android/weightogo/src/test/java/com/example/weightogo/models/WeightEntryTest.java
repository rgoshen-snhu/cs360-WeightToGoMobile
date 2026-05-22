package com.example.weightogo.models;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Unit tests for WeightEntry model class.
 * Tests FR1.2 - Weight Entry data model.
 */
public class WeightEntryTest {

    @Test
    public void test_defaultConstructor_createsWeightEntryObject() {
        // ARRANGE & ACT
        WeightEntry entry = new WeightEntry();

        // ASSERT
        assertNotNull("WeightEntry object should not be null", entry);
    }

    @Test
    public void test_getWeightId_defaultValue_returnsZero() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();

        // ACT
        long weightId = entry.getWeightId();

        // ASSERT
        assertEquals("Default weightId should be 0", 0L, weightId);
    }

    @Test
    public void test_setWeightId_withValidId_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        long expectedId = 42L;

        // ACT
        entry.setWeightId(expectedId);

        // ASSERT
        assertEquals("WeightId should be set correctly", expectedId, entry.getWeightId());
    }

    @Test
    public void test_setUserId_withValidId_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        long expectedUserId = 123L;

        // ACT
        entry.setUserId(expectedUserId);

        // ASSERT
        assertEquals("UserId should be set correctly", expectedUserId, entry.getUserId());
    }

    @Test
    public void test_setWeightValue_withValidValue_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        double expectedWeight = 175.5;

        // ACT
        entry.setWeightValue(expectedWeight);

        // ASSERT
        assertEquals("Weight value should be set correctly", expectedWeight, entry.getWeightValue(), 0.01);
    }

    @Test
    public void test_setWeightUnit_withValidUnit_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        String expectedUnit = "lbs";

        // ACT
        entry.setWeightUnit(expectedUnit);

        // ASSERT
        assertEquals("Weight unit should be set correctly", expectedUnit, entry.getWeightUnit());
    }

    @Test
    public void test_setWeightDate_withValidDate_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        LocalDate expectedDate = LocalDate.of(2025, 12, 10);

        // ACT
        entry.setWeightDate(expectedDate);

        // ASSERT
        assertEquals("Weight date should be set correctly", expectedDate, entry.getWeightDate());
    }

    @Test
    public void test_setNotes_withValidNotes_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        String expectedNotes = "Feeling good today!";

        // ACT
        entry.setNotes(expectedNotes);

        // ASSERT
        assertEquals("Notes should be set correctly", expectedNotes, entry.getNotes());
    }

    @Test
    public void test_setCreatedAt_withValidDateTime_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        LocalDateTime expectedDateTime = LocalDateTime.of(2025, 12, 10, 8, 30, 0);

        // ACT
        entry.setCreatedAt(expectedDateTime);

        // ASSERT
        assertEquals("CreatedAt should be set correctly", expectedDateTime, entry.getCreatedAt());
    }

    @Test
    public void test_setUpdatedAt_withValidDateTime_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        LocalDateTime expectedDateTime = LocalDateTime.of(2025, 12, 10, 9, 15, 0);

        // ACT
        entry.setUpdatedAt(expectedDateTime);

        // ASSERT
        assertEquals("UpdatedAt should be set correctly", expectedDateTime, entry.getUpdatedAt());
    }

    @Test
    public void test_setIsDeleted_withValidFlag_setsValue() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        boolean expectedFlag = true;

        // ACT
        entry.setDeleted(expectedFlag);

        // ASSERT
        assertEquals("IsDeleted should be set correctly", expectedFlag, entry.isDeleted());
    }

    @Test
    public void test_toString_returnsNonNullString() {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        entry.setWeightId(1L);
        entry.setUserId(123L);
        entry.setWeightValue(175.5);
        entry.setWeightUnit("lbs");
        entry.setWeightDate(LocalDate.of(2025, 12, 10));

        // ACT
        String result = entry.toString();

        // ASSERT
        assertNotNull("toString should return non-null value", result);
        assertTrue("toString should contain weightId", result.contains("1"));
        assertTrue("toString should contain userId", result.contains("123"));
        assertTrue("toString should contain weightValue", result.contains("175.5"));
        assertTrue("toString should contain weightUnit", result.contains("lbs"));
        assertTrue("toString should contain weightDate", result.contains("2025-12-10"));
    }

    @Test
    public void test_equals_withSameWeightId_returnsTrue() {
        // ARRANGE
        WeightEntry entry1 = new WeightEntry();
        entry1.setWeightId(42L);
        entry1.setWeightValue(175.5);

        WeightEntry entry2 = new WeightEntry();
        entry2.setWeightId(42L);
        entry2.setWeightValue(180.0);

        // ACT & ASSERT
        assertEquals("Entries with same weightId should be equal", entry1, entry2);
    }

    @Test
    public void test_equals_withDifferentWeightId_returnsFalse() {
        // ARRANGE
        WeightEntry entry1 = new WeightEntry();
        entry1.setWeightId(42L);

        WeightEntry entry2 = new WeightEntry();
        entry2.setWeightId(99L);

        // ACT & ASSERT
        assertNotEquals("Entries with different weightId should not be equal", entry1, entry2);
    }

    @Test
    public void test_hashCode_withSameWeightId_returnsSameHash() {
        // ARRANGE
        WeightEntry entry1 = new WeightEntry();
        entry1.setWeightId(42L);

        WeightEntry entry2 = new WeightEntry();
        entry2.setWeightId(42L);

        // ACT & ASSERT
        assertEquals("Entries with same weightId should have same hashCode", entry1.hashCode(), entry2.hashCode());
    }

    @Test
    public void test_equals_withUninitializedEntries_returnsFalse() {
        // ARRANGE
        WeightEntry entry1 = new WeightEntry();  // weightId = 0 (uninitialized)
        WeightEntry entry2 = new WeightEntry();  // weightId = 0 (uninitialized)

        // ACT & ASSERT
        assertNotEquals("Uninitialized entries (weightId=0) should not be equal", entry1, entry2);
    }
}
