package com.example.weightogo.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utility class for executing background tasks with result callbacks on the UI thread.
 * Uses ExecutorService for background work and Handler for UI thread callbacks.
 *
 * <p><strong>Use Case:</strong> CPU-intensive operations like password hashing that should
 * not block the UI thread.</p>
 *
 * <p><strong>Thread Safety:</strong> Results are always delivered on the main/UI thread
 * via Handler, making it safe to update UI components in callbacks.</p>
 *
 * @param <R> The type of result returned by the background task
 */
public class BackgroundTask<R> {

    /**
     * Callback interface for background task results.
     *
     * @param <R> The type of result
     */
    public interface Callback<R> {
        /**
         * Called on UI thread when background task completes successfully.
         *
         * @param result The result from the background task
         */
        void onResult(R result);

        /**
         * Called on UI thread if background task throws an exception.
         *
         * @param error The exception that occurred
         */
        default void onError(Exception error) {
            // Default: do nothing (subclass can override)
        }
    }

    /**
     * Functional interface for the background work.
     *
     * @param <R> The type of result
     */
    public interface Work<R> {
        /**
         * Execute work on background thread.
         *
         * @return The result of the work
         * @throws Exception if work fails
         */
        R execute() throws Exception;
    }

    private static final Executor EXECUTOR = Executors.newFixedThreadPool(4);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /**
     * Execute work on background thread and deliver result on UI thread.
     *
     * @param work The work to execute in background
     * @param callback Callback for result (called on UI thread)
     * @param <R> The type of result
     */
    public static <R> void execute(@NonNull Work<R> work, @NonNull Callback<R> callback) {
        EXECUTOR.execute(() -> {
            try {
                // Execute work on background thread
                R result = work.execute();

                // Deliver result on UI thread
                MAIN_HANDLER.post(() -> callback.onResult(result));

            } catch (Exception e) {
                // Deliver error on UI thread
                MAIN_HANDLER.post(() -> callback.onError(e));
            }
        });
    }
}
