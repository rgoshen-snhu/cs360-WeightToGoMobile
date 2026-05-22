package com.example.weightogo.database;

/**
 * Exception thrown when attempting to insert a user with a username that already exists.
 *
 * <p>This exception is thrown when a UNIQUE constraint violation occurs on the
 * username column in the users table.</p>
 */
public class DuplicateUsernameException extends DatabaseException {

    /**
     * Constructs a new DuplicateUsernameException with the specified detail message.
     *
     * @param message The detail message (typically includes the duplicate username)
     */
    public DuplicateUsernameException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateUsernameException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The underlying SQLite exception
     */
    public DuplicateUsernameException(String message, Throwable cause) {
        super(message, cause);
    }
}