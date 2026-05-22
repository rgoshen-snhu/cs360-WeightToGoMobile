package com.example.weightogo.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for password hashing and verification using SHA-256 with cryptographic salts.
 *
 * This class provides secure password handling for user authentication. Passwords are never
 * stored in plain text. Instead, they are hashed using SHA-256 with a cryptographic salt.
 *
 * **Security Features:**
 * - SHA-256 cryptographic hashing algorithm
 * - Cryptographically secure random salt generation (16 bytes)
 * - Base64 encoding for storage compatibility
 * - Deterministic verification (same password + salt = same hash)
 *
 * **Usage Example:**
 * <pre>
 * // Registration:
 * String salt = PasswordUtils.generateSalt();
 * String hash = PasswordUtils.hashPassword(plainPassword, salt);
 * // Store hash and salt in database (never store plain password)
 *
 * // Login verification:
 * String storedHash = getUserHashFromDatabase();
 * String storedSalt = getUserSaltFromDatabase();
 * boolean isValid = PasswordUtils.verifyPassword(plainPassword, storedSalt, storedHash);
 * </pre>
 *
 * **Thread Safety:** All methods are static and thread-safe.
 *
 * **SECURITY WARNING:**
 * - NEVER log passwords, hashes, or salts
 * - NEVER store plain text passwords
 * - NEVER transmit plain text passwords
 * - Always use secure connections (HTTPS) when transmitting credentials
 */
public final class PasswordUtils {

    private static final String TAG = "PasswordUtils";

    /**
     * Salt length in bytes (16 bytes = 128 bits of entropy).
     * Base64 encoding will result in a 24-character string.
     */
    private static final int SALT_LENGTH_BYTES = 16;

    /**
     * SHA-256 algorithm name for MessageDigest.
     */
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class with only static methods.
     *
     * @throws AssertionError if instantiation is attempted via reflection
     */
    private PasswordUtils() {
        throw new AssertionError("PasswordUtils is a utility class and should not be instantiated");
    }

    /**
     * Generates a cryptographically secure random salt.
     *
     * The salt is a 16-byte random value generated using {@link SecureRandom},
     * then Base64-encoded for storage compatibility.
     *
     * **Security Note:** Each call generates a new unique salt. The same salt
     * should never be reused across different users or passwords.
     *
     * @return Base64-encoded random salt (24 characters), never null
     */
    @NonNull
    public static String generateSalt() {
        try {
            // Use SecureRandom for cryptographically secure random bytes
            SecureRandom random = new SecureRandom();
            byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
            random.nextBytes(saltBytes);

            // Encode to Base64 for storage
            String salt = Base64.getEncoder().encodeToString(saltBytes);

            Log.d(TAG, "Generated new salt (length: " + salt.length() + " chars)");
            return salt;

        } catch (Exception e) {
            Log.e(TAG, "Error generating salt: " + e.getMessage(), e);
            // Re-throw as runtime exception - salt generation failure is critical
            throw new RuntimeException("Failed to generate salt", e);
        }
    }

    /**
     * Hashes a password with a salt using SHA-256.
     *
     * The password and salt are concatenated, then hashed using SHA-256.
     * The resulting hash is Base64-encoded for storage.
     *
     * **Deterministic:** Same password + same salt will always produce the same hash.
     *
     * **Security Note:** Never log the password parameter. Always use the salt
     * from {@link #generateSalt()} to ensure cryptographic security.
     *
     * @param password plain text password to hash (never null)
     * @param salt Base64-encoded salt from {@link #generateSalt()} (never null)
     * @return Base64-encoded SHA-256 hash, or null if inputs are invalid
     */
    @Nullable
    public static String hashPassword(@Nullable String password, @Nullable String salt) {
        // Validate inputs
        if (ValidationUtils.isNullOrEmpty(password)) {
            Log.w(TAG, "hashPassword: password is null or empty");
            return null;
        }
        if (ValidationUtils.isNullOrEmpty(salt)) {
            Log.w(TAG, "hashPassword: salt is null or empty");
            return null;
        }

        try {
            // Concatenate password and salt
            String combined = password + salt;

            // Hash using SHA-256
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(combined.getBytes(StandardCharsets.UTF_8));

            // Encode to Base64 for storage
            String hash = Base64.getEncoder().encodeToString(hashBytes);

            Log.d(TAG, "Password hashed successfully (hash length: " + hash.length() + " chars)");
            return hash;

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA-256 algorithm not available: " + e.getMessage(), e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error hashing password: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Verifies a password against a stored hash and salt.
     *
     * Re-hashes the provided password with the stored salt, then compares
     * the result with the stored hash. Returns true if they match.
     *
     * **Security Note:** This method uses constant-time comparison to prevent
     * timing attacks. Never log the password, hash, or salt parameters.
     *
     * @param password plain text password to verify (never null)
     * @param salt stored Base64-encoded salt (never null)
     * @param storedHash stored Base64-encoded hash to compare against (never null)
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(@Nullable String password, @Nullable String salt, @Nullable String storedHash) {
        // Validate inputs
        if (ValidationUtils.isNullOrEmpty(password)) {
            Log.w(TAG, "verifyPassword: password is null or empty");
            return false;
        }
        if (ValidationUtils.isNullOrEmpty(salt)) {
            Log.w(TAG, "verifyPassword: salt is null or empty");
            return false;
        }
        if (ValidationUtils.isNullOrEmpty(storedHash)) {
            Log.w(TAG, "verifyPassword: storedHash is null or empty");
            return false;
        }

        try {
            // Hash the provided password with the stored salt
            String computedHash = hashPassword(password, salt);

            if (computedHash == null) {
                Log.e(TAG, "verifyPassword: failed to compute hash");
                return false;
            }

            // Decode both hashes to byte arrays for constant-time comparison
            // This prevents timing attacks that could leak hash information
            byte[] storedBytes = Base64.getDecoder().decode(storedHash);
            byte[] computedBytes = Base64.getDecoder().decode(computedHash);

            // Use MessageDigest.isEqual() for constant-time comparison
            // String.equals() is vulnerable to timing attacks
            boolean isMatch = MessageDigest.isEqual(storedBytes, computedBytes);

            // Log result (never log the actual password or hashes)
            if (isMatch) {
                Log.i(TAG, "Password verification successful");
            } else {
                Log.w(TAG, "Password verification failed");
            }

            return isMatch;

        } catch (Exception e) {
            Log.e(TAG, "Error verifying password: " + e.getMessage(), e);
            return false;
        }
    }
}
