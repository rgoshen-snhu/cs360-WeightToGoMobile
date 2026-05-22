package com.example.weightogo.database;

/**
 * Exception thrown when database operations fail.
 *
 * <p>This exception wraps SQLite errors and provides more context
 * about what operation failed and why.</p>
 */
public class DatabaseException extends Exception {

    /**
     * Constructs a new DatabaseException with the specified detail message.
     *
     * @param message The detail message
     */
    public DatabaseException(String message) {
        super(message);
    }

    /**
     * Constructs a new DatabaseException with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause The cause of this exception
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}