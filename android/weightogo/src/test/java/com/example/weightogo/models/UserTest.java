package com.example.weightogo.models;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.*;

/**
 * Unit tests for User model class.
 * Tests FR1.1 - User Registration data model.
 */
public class UserTest {

    @Test
    public void test_defaultConstructor_createsUserObject() {
        // ARRANGE & ACT
        User user = new User();

        // ASSERT
        assertNotNull("User object should not be null", user);
    }

    @Test
    public void test_getUserId_defaultValue_returnsZero() {
        // ARRANGE
        User user = new User();

        // ACT
        long userId = user.getUserId();

        // ASSERT
        assertEquals("Default userId should be 0", 0L, userId);
    }

    @Test
    public void test_setUserId_withValidId_setsValue() {
        // ARRANGE
        User user = new User();
        long expectedId = 42L;

        // ACT
        user.setUserId(expectedId);

        // ASSERT
        assertEquals("UserId should be set correctly", expectedId, user.getUserId());
    }

    @Test
    public void test_setUsername_withValidUsername_setsValue() {
        // ARRANGE
        User user = new User();
        String expectedUsername = "johndoe";

        // ACT
        user.setUsername(expectedUsername);

        // ASSERT
        assertEquals("Username should be set correctly", expectedUsername, user.getUsername());
    }

    @Test
    public void test_setPasswordHash_withValidHash_setsValue() {
        // ARRANGE
        User user = new User();
        String expectedHash = "abc123def456";

        // ACT
        user.setPasswordHash(expectedHash);

        // ASSERT
        assertEquals("PasswordHash should be set correctly", expectedHash, user.getPasswordHash());
    }

    @Test
    public void test_setSalt_withValidSalt_setsValue() {
        // ARRANGE
        User user = new User();
        String expectedSalt = "random16byteSalt";

        // ACT
        user.setSalt(expectedSalt);

        // ASSERT
        assertEquals("Salt should be set correctly", expectedSalt, user.getSalt());
    }

    @Test
    public void test_setCreatedAt_withValidDateTime_setsValue() {
        // ARRANGE
        User user = new User();
        LocalDateTime expectedDateTime = LocalDateTime.of(2025, 12, 9, 10, 30, 0);

        // ACT
        user.setCreatedAt(expectedDateTime);

        // ASSERT
        assertEquals("CreatedAt should be set correctly", expectedDateTime, user.getCreatedAt());
    }

    @Test
    public void test_setLastLogin_withValidDateTime_setsValue() {
        // ARRANGE
        User user = new User();
        LocalDateTime expectedDateTime = LocalDateTime.of(2025, 12, 9, 14, 45, 0);

        // ACT
        user.setLastLogin(expectedDateTime);

        // ASSERT
        assertEquals("LastLogin should be set correctly", expectedDateTime, user.getLastLogin());
    }

    @Test
    public void test_toString_returnsNonNullString() {
        // ARRANGE
        User user = new User();
        user.setUserId(1L);
        user.setUsername("johndoe");

        // ACT
        String result = user.toString();

        // ASSERT
        assertNotNull("toString should return non-null value", result);
        assertTrue("toString should contain username", result.contains("johndoe"));
        assertTrue("toString should contain userId", result.contains("1"));
    }

    @Test
    public void test_toString_doesNotExposePasswordHash() {
        // ARRANGE
        User user = new User();
        user.setPasswordHash("supersecret123");
        user.setSalt("randomsalt456");

        // ACT
        String result = user.toString();

        // ASSERT
        assertFalse("toString should NOT expose password hash", result.contains("supersecret123"));
        assertFalse("toString should NOT expose salt", result.contains("randomsalt456"));
    }

    @Test
    public void test_setEmail_withValidEmail_setsValue() {
        // ARRANGE
        User user = new User();
        String expectedEmail = "john@example.com";

        // ACT
        user.setEmail(expectedEmail);

        // ASSERT
        assertEquals("Email should be set correctly", expectedEmail, user.getEmail());
    }

    @Test
    public void test_setPhoneNumber_withValidPhone_setsValue() {
        // ARRANGE
        User user = new User();
        String expectedPhone = "+15551234567";

        // ACT
        user.setPhoneNumber(expectedPhone);

        // ASSERT
        assertEquals("Phone number should be set correctly", expectedPhone, user.getPhoneNumber());
    }

    @Test
    public void test_setDisplayName_withValidName_setsValue() {
        // ARRANGE
        User user = new User();
        String expectedDisplayName = "John Doe";

        // ACT
        user.setDisplayName(expectedDisplayName);

        // ASSERT
        assertEquals("Display name should be set correctly", expectedDisplayName, user.getDisplayName());
    }

    @Test
    public void test_setUpdatedAt_withValidDateTime_setsValue() {
        // ARRANGE
        User user = new User();
        LocalDateTime expectedDateTime = LocalDateTime.of(2025, 12, 10, 15, 30, 0);

        // ACT
        user.setUpdatedAt(expectedDateTime);

        // ASSERT
        assertEquals("UpdatedAt should be set correctly", expectedDateTime, user.getUpdatedAt());
    }

    @Test
    public void test_setIsActive_withValidFlag_setsValue() {
        // ARRANGE
        User user = new User();
        boolean expectedFlag = true;

        // ACT
        user.setActive(expectedFlag);

        // ASSERT
        assertEquals("IsActive should be set correctly", expectedFlag, user.isActive());
    }

    @Test
    public void test_equals_withSameUserId_returnsTrue() {
        // ARRANGE
        User user1 = new User();
        user1.setUserId(42L);
        user1.setUsername("john");

        User user2 = new User();
        user2.setUserId(42L);
        user2.setUsername("different");

        // ACT & ASSERT
        assertEquals("Users with same userId should be equal", user1, user2);
    }

    @Test
    public void test_equals_withDifferentUserId_returnsFalse() {
        // ARRANGE
        User user1 = new User();
        user1.setUserId(42L);

        User user2 = new User();
        user2.setUserId(99L);

        // ACT & ASSERT
        assertNotEquals("Users with different userId should not be equal", user1, user2);
    }

    @Test
    public void test_equals_withNull_returnsFalse() {
        // ARRANGE
        User user = new User();
        user.setUserId(42L);

        // ACT & ASSERT
        assertNotEquals("User should not equal null", user, null);
    }

    @Test
    public void test_equals_withSameInstance_returnsTrue() {
        // ARRANGE
        User user = new User();
        user.setUserId(42L);

        // ACT & ASSERT
        assertEquals("User should equal itself", user, user);
    }

    @Test
    public void test_hashCode_withSameUserId_returnsSameHash() {
        // ARRANGE
        User user1 = new User();
        user1.setUserId(42L);

        User user2 = new User();
        user2.setUserId(42L);

        // ACT & ASSERT
        assertEquals("Users with same userId should have same hashCode", user1.hashCode(), user2.hashCode());
    }

    @Test
    public void test_hashCode_withDifferentUserId_returnsDifferentHash() {
        // ARRANGE
        User user1 = new User();
        user1.setUserId(42L);

        User user2 = new User();
        user2.setUserId(99L);

        // ACT & ASSERT
        assertNotEquals("Users with different userId should have different hashCode", user1.hashCode(), user2.hashCode());
    }

    @Test
    public void test_equals_withUninitializedUsers_returnsFalse() {
        // ARRANGE
        User user1 = new User();  // userId = 0 (uninitialized)
        User user2 = new User();  // userId = 0 (uninitialized)

        // ACT & ASSERT
        assertNotEquals("Uninitialized users (userId=0) should not be equal", user1, user2);
    }

    @Test
    public void test_equals_withDifferentClass_returnsFalse() {
        // ARRANGE
        User user = new User();
        user.setUserId(42L);
        String notAUser = "not a user";

        // ACT & ASSERT
        assertNotEquals("User should not equal a String", user, notAUser);
    }
}
