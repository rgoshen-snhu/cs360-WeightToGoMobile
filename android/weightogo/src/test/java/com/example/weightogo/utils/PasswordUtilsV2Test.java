package com.example.weightogo.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for PasswordUtilsV2 (Phase 8.6: bcrypt migration).
 * Tests bcrypt hashing, verification, and hybrid SHA256/bcrypt support.
 */
public class PasswordUtilsV2Test {

    // =================================================================================
    // bcrypt Hashing Tests
    // =================================================================================

    /**
     * Test 1: bcrypt hashing with valid password returns non-null hash
     */
    @Test
    public void test_hashPasswordBcrypt_withValidInput_returnsNonNullHash() {
        // ARRANGE
        String password = "Test123!";

        // ACT
        String hash = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ASSERT
        assertNotNull("bcrypt hash should not be null", hash);
        assertTrue("bcrypt hash should start with $2a$", hash.startsWith("$2a$"));
        assertTrue("bcrypt hash should include cost factor 12", hash.contains("$12$"));
    }

    /**
     * Test 2: bcrypt generates unique hashes for same password (salt is random)
     */
    @Test
    public void test_hashPasswordBcrypt_withSamePassword_generatesUniqueHashes() {
        // ARRANGE
        String password = "Test123!";

        // ACT
        String hash1 = PasswordUtilsV2.hashPasswordBcrypt(password);
        String hash2 = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ASSERT
        assertNotNull("First hash should not be null", hash1);
        assertNotNull("Second hash should not be null", hash2);
        assertNotEquals("Hashes should be different (bcrypt uses random salt)", hash1, hash2);
    }

    /**
     * Test 3: bcrypt hashing with empty password returns null
     */
    @Test
    public void test_hashPasswordBcrypt_withEmptyPassword_returnsNull() {
        // ARRANGE
        String password = "";

        // ACT
        String hash = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ASSERT
        assertNull("Empty password should return null", hash);
    }

    /**
     * Test 4: bcrypt hashing with null password returns null (defensive programming)
     */
    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_hashPasswordBcrypt_withNullPassword_returnsNull() {
        // ACT
        String hash = PasswordUtilsV2.hashPasswordBcrypt(null);

        // ASSERT
        assertNull("Null password should return null", hash);
    }

    // =================================================================================
    // bcrypt Verification Tests
    // =================================================================================

    /**
     * Test 5: bcrypt verification with correct password returns true
     */
    @Test
    public void test_verifyPasswordBcrypt_withCorrectPassword_returnsTrue() {
        // ARRANGE
        String password = "Test123!";
        String hash = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPasswordBcrypt(password, hash);

        // ASSERT
        assertTrue("Correct password should verify", verified);
    }

    /**
     * Test 6: bcrypt verification with wrong password returns false
     */
    @Test
    public void test_verifyPasswordBcrypt_withWrongPassword_returnsFalse() {
        // ARRANGE
        String correctPassword = "Test123!";
        String wrongPassword = "Wrong123!";
        String hash = PasswordUtilsV2.hashPasswordBcrypt(correctPassword);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPasswordBcrypt(wrongPassword, hash);

        // ASSERT
        assertFalse("Wrong password should not verify", verified);
    }

    /**
     * Test 7: bcrypt verification with empty password returns false
     */
    @Test
    public void test_verifyPasswordBcrypt_withEmptyPassword_returnsFalse() {
        // ARRANGE
        String password = "Test123!";
        String hash = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPasswordBcrypt("", hash);

        // ASSERT
        assertFalse("Empty password should not verify", verified);
    }

    /**
     * Test 8: bcrypt verification with empty hash returns false
     */
    @Test
    public void test_verifyPasswordBcrypt_withEmptyHash_returnsFalse() {
        // ARRANGE
        String password = "Test123!";

        // ACT
        boolean verified = PasswordUtilsV2.verifyPasswordBcrypt(password, "");

        // ASSERT
        assertFalse("Empty hash should not verify", verified);
    }

    // =================================================================================
    // Hybrid Verification Tests (SHA256 + bcrypt)
    // =================================================================================

    /**
     * Test 9: Hybrid verification with SHA256 algorithm uses legacy verification
     */
    @Test
    public void test_verifyPassword_withSHA256Algorithm_usesLegacyVerification() {
        // ARRANGE
        String password = "Test123!";
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(password, salt);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPassword(password, "SHA256", hash, salt);

        // ASSERT
        assertTrue("SHA256 password should verify using legacy method", verified);
    }

    /**
     * Test 10: Hybrid verification with BCRYPT algorithm uses bcrypt verification
     */
    @Test
    public void test_verifyPassword_withBcryptAlgorithm_usesBcryptVerification() {
        // ARRANGE
        String password = "Test123!";
        String hash = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPassword(password, "BCRYPT", hash, "");

        // ASSERT
        assertTrue("bcrypt password should verify using bcrypt method", verified);
    }

    /**
     * Test 11: Hybrid verification with unknown algorithm returns false
     */
    @Test
    public void test_verifyPassword_withUnknownAlgorithm_returnsFalse() {
        // ARRANGE
        String password = "Test123!";
        String hash = "somehash";

        // ACT
        boolean verified = PasswordUtilsV2.verifyPassword(password, "MD5", hash, "salt");

        // ASSERT
        assertFalse("Unknown algorithm should return false", verified);
    }

    /**
     * Test 12: Hybrid verification with SHA256 but wrong password returns false
     */
    @Test
    public void test_verifyPassword_withSHA256AndWrongPassword_returnsFalse() {
        // ARRANGE
        String correctPassword = "Test123!";
        String wrongPassword = "Wrong123!";
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(correctPassword, salt);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPassword(wrongPassword, "SHA256", hash, salt);

        // ASSERT
        assertFalse("Wrong password should not verify even with correct algorithm", verified);
    }

    /**
     * Test 13: Hybrid verification with BCRYPT but wrong password returns false
     */
    @Test
    public void test_verifyPassword_withBcryptAndWrongPassword_returnsFalse() {
        // ARRANGE
        String correctPassword = "Test123!";
        String wrongPassword = "Wrong123!";
        String hash = PasswordUtilsV2.hashPasswordBcrypt(correctPassword);

        // ACT
        boolean verified = PasswordUtilsV2.verifyPassword(wrongPassword, "BCRYPT", hash, "");

        // ASSERT
        assertFalse("Wrong password should not verify", verified);
    }

    // =================================================================================
    // Edge Case Tests
    // =================================================================================

    /**
     * Test 14: bcrypt works with complex passwords (special characters)
     */
    @Test
    public void test_hashPasswordBcrypt_withComplexPassword_works() {
        // ARRANGE
        String complexPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':,.<>?/`~";

        // ACT
        String hash = PasswordUtilsV2.hashPasswordBcrypt(complexPassword);
        boolean verified = PasswordUtilsV2.verifyPasswordBcrypt(complexPassword, hash);

        // ASSERT
        assertNotNull("Complex password should hash successfully", hash);
        assertTrue("Complex password should verify", verified);
    }

    /**
     * Test 15: bcrypt works with long passwords (64+ characters)
     */
    @Test
    public void test_hashPasswordBcrypt_withLongPassword_works() {
        // ARRANGE
        // bcrypt has 72-byte limit, so use exactly 72 characters (ASCII = 72 bytes)
        String longPassword = "ThisIsAVeryLongPasswordWithExactly72CharactersToTestBcryptHandling123!";

        // ACT
        String hash = PasswordUtilsV2.hashPasswordBcrypt(longPassword);
        boolean verified = PasswordUtilsV2.verifyPasswordBcrypt(longPassword, hash);

        // ASSERT
        assertNotNull("Long password (72 chars) should hash successfully", hash);
        assertTrue("Long password should verify", verified);
    }

    /**
     * Test 16: bcrypt is case-sensitive
     */
    @Test
    public void test_verifyPasswordBcrypt_isCaseSensitive() {
        // ARRANGE
        String password = "Test123!";
        String hash = PasswordUtilsV2.hashPasswordBcrypt(password);

        // ACT
        boolean verifiedLower = PasswordUtilsV2.verifyPasswordBcrypt("test123!", hash);
        boolean verifiedUpper = PasswordUtilsV2.verifyPasswordBcrypt("TEST123!", hash);

        // ASSERT
        assertFalse("Lowercase password should not verify", verifiedLower);
        assertFalse("Uppercase password should not verify", verifiedUpper);
    }
}
