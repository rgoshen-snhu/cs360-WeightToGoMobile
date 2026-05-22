package com.example.weightogo.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for PasswordUtils class.
 * Tests SHA-256 password hashing with salted cryptographic security.
 */
public class PasswordUtilsTest {

    /**
     * Tests that generateSalt() returns a non-empty string.
     * Salt should be Base64-encoded random bytes.
     */
    @Test
    public void test_generateSalt_returnsNonEmptyString() {
        // ACT
        String salt = PasswordUtils.generateSalt();

        // ASSERT
        assertNotNull("Salt should not be null", salt);
        assertFalse("Salt should not be empty", salt.isEmpty());
    }

    /**
     * Tests that generateSalt() returns different salts on each call.
     * Each salt must be cryptographically unique.
     */
    @Test
    public void test_generateSalt_returnsDifferentSalts() {
        // ACT
        String salt1 = PasswordUtils.generateSalt();
        String salt2 = PasswordUtils.generateSalt();

        // ASSERT
        assertNotNull("First salt should not be null", salt1);
        assertNotNull("Second salt should not be null", salt2);
        assertNotEquals("Salts should be different (cryptographically unique)", salt1, salt2);
    }

    /**
     * Tests that hashPassword() returns the same hash for the same password and salt.
     * SHA-256 hashing should be deterministic.
     */
    @Test
    public void test_hashPassword_withSameInput_returnsSameHash() {
        // ARRANGE
        String password = "TestPassword123";
        String salt = "fixedSaltValue";

        // ACT
        String hash1 = PasswordUtils.hashPassword(password, salt);
        String hash2 = PasswordUtils.hashPassword(password, salt);

        // ASSERT
        assertNotNull("First hash should not be null", hash1);
        assertNotNull("Second hash should not be null", hash2);
        assertEquals("Same password + same salt should produce identical hash", hash1, hash2);
    }

    /**
     * Tests that hashPassword() returns different hashes for different salts.
     * Different salts must produce different hashes even with same password.
     */
    @Test
    public void test_hashPassword_withDifferentSalt_returnsDifferentHash() {
        // ARRANGE
        String password = "TestPassword123";
        String salt1 = "salt1";
        String salt2 = "salt2";

        // ACT
        String hash1 = PasswordUtils.hashPassword(password, salt1);
        String hash2 = PasswordUtils.hashPassword(password, salt2);

        // ASSERT
        assertNotNull("First hash should not be null", hash1);
        assertNotNull("Second hash should not be null", hash2);
        assertNotEquals("Different salts should produce different hashes", hash1, hash2);
    }

    /**
     * Tests that verifyPassword() returns true for correct password.
     * Verification should match the hash generated from the same password and salt.
     */
    @Test
    public void test_verifyPassword_withCorrectPassword_returnsTrue() {
        // ARRANGE
        String password = "MySecurePassword456";
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(password, salt);

        // ACT
        boolean result = PasswordUtils.verifyPassword(password, salt, hash);

        // ASSERT
        assertTrue("Correct password should verify successfully", result);
    }

    /**
     * Tests that verifyPassword() returns false for incorrect password.
     * Verification should reject passwords that don't match the original.
     */
    @Test
    public void test_verifyPassword_withWrongPassword_returnsFalse() {
        // ARRANGE
        String correctPassword = "CorrectPassword789";
        String wrongPassword = "WrongPassword000";
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(correctPassword, salt);

        // ACT
        boolean result = PasswordUtils.verifyPassword(wrongPassword, salt, hash);

        // ASSERT
        assertFalse("Wrong password should fail verification", result);
    }
}
