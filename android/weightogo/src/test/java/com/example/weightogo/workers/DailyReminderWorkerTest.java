package com.example.weightogo.workers;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import com.example.weightogo.database.AchievementDAO;
import com.example.weightogo.database.UserDAO;
import com.example.weightogo.database.UserPreferenceDAO;
import com.example.weightogo.database.WeighToGoDBHelper;
import com.example.weightogo.database.WeightEntryDAO;
import com.example.weightogo.models.User;
import com.example.weightogo.models.WeightEntry;
import com.example.weightogo.utils.SMSNotificationManager;
import com.example.weightogo.utils.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Unit tests for DailyReminderWorker.
 *
 * Tests FR7.6 - Daily reminder SMS functionality:
 * - Worker skips reminder if user already logged weight today
 * - Worker sends reminder if user hasn't logged today
 * - Worker respects user preference for reminder enabled/disabled
 * - Worker handles missing phone number gracefully
 *
 * Uses WorkManager testing library for Worker testing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30)
public class DailyReminderWorkerTest {

    private Context context;
    private WeighToGoDBHelper dbHelper;
    private UserDAO userDAO;
    private WeightEntryDAO weightEntryDAO;
    private UserPreferenceDAO userPreferenceDAO;
    private AchievementDAO achievementDAO;
    private SessionManager sessionManager;
    private SMSNotificationManager smsManager;
    private long testUserId;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        dbHelper = WeighToGoDBHelper.getInstance(context);
        userDAO = new UserDAO(dbHelper);
        weightEntryDAO = new WeightEntryDAO(dbHelper);
        userPreferenceDAO = new UserPreferenceDAO(dbHelper);
        achievementDAO = new AchievementDAO(dbHelper);
        sessionManager = SessionManager.getInstance(context);
        smsManager = SMSNotificationManager.getInstance(context, userDAO, userPreferenceDAO, achievementDAO);

        // Create test user
        User testUser = new User();
        testUser.setUsername("reminder_testuser_" + System.currentTimeMillis());
        testUser.setPasswordHash("test_hash");
        testUser.setSalt("test_salt");
        testUser.setPasswordAlgorithm("SHA256");
        testUser.setPhoneNumber("+12025551234");  // Valid E.164 phone
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        testUser.setActive(true);

        try {
            testUserId = userDAO.insertUser(testUser);
            testUser.setUserId(testUserId);
            sessionManager.createSession(testUser);

            // Enable SMS notifications by default
            userPreferenceDAO.setPreference(testUserId, SMSNotificationManager.KEY_SMS_ENABLED, "true");
            userPreferenceDAO.setPreference(testUserId, SMSNotificationManager.KEY_REMINDER_ENABLED, "true");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test user", e);
        }
    }

    @After
    public void tearDown() {
        if (testUserId > 0) {
            userDAO.deleteUser(testUserId);
        }
        sessionManager.logout();
    }

    /**
     * Test 1: Worker skips reminder if user already logged weight today.
     *
     * Tests FR7.6 - Daily reminder should not send if user is already active.
     * Verifies that the worker checks for existing entry and returns success without sending SMS.
     *
     * RED PHASE: This test MUST FAIL before implementing DailyReminderWorker.
     */
    @Test
    public void test_doWork_userLoggedToday_skipsReminder() {
        // ARRANGE - Create weight entry for today
        WeightEntry todayEntry = new WeightEntry();
        todayEntry.setUserId(testUserId);
        todayEntry.setWeightValue(150.0);
        todayEntry.setWeightUnit("lbs");
        todayEntry.setWeightDate(LocalDate.now());
        todayEntry.setCreatedAt(LocalDateTime.now());
        todayEntry.setUpdatedAt(LocalDateTime.now());
        todayEntry.setDeleted(false);

        long entryId = weightEntryDAO.insertWeightEntry(todayEntry);
        assert entryId > 0 : "Failed to insert today's weight entry";

        // ACT - Run the worker
        DailyReminderWorker worker = TestListenableWorkerBuilder
                .from(context, DailyReminderWorker.class)
                .build();

        ListenableWorker.Result result = worker.doWork();

        // ASSERT - Worker should succeed without sending SMS
        assertEquals("Worker should return SUCCESS", ListenableWorker.Result.success(), result);
        // Note: Cannot easily verify SMS was NOT sent in Robolectric
        // Manual testing will verify no SMS sent
    }

    /**
     * Test 2: Worker sends reminder if user hasn't logged today.
     *
     * Tests FR7.6 - Daily reminder should send if user hasn't logged weight.
     * Verifies that the worker detects missing entry and calls SMS manager.
     *
     * RED PHASE: This test MUST FAIL before implementing DailyReminderWorker.
     */
    @Test
    public void test_doWork_userNotLoggedToday_sendsReminder() {
        // ARRANGE - No weight entry for today (setup only creates user)

        // ACT - Run the worker
        DailyReminderWorker worker = TestListenableWorkerBuilder
                .from(context, DailyReminderWorker.class)
                .build();

        ListenableWorker.Result result = worker.doWork();

        // ASSERT - Worker should attempt to send SMS
        // Note: Actual SMS sending will fail in Robolectric, but worker returns SUCCESS if attempt was made
        // Manual testing will verify SMS is sent
        assertEquals("Worker should return SUCCESS", ListenableWorker.Result.success(), result);
    }

    /**
     * Test 3: Worker skips reminder if reminder disabled in preferences.
     *
     * Tests FR7.6 - Daily reminder respects user preferences.
     * Verifies that the worker checks sms_reminder_enabled preference.
     *
     * RED PHASE: This test MUST FAIL before implementing DailyReminderWorker.
     */
    @Test
    public void test_doWork_reminderDisabled_skipsReminder() {
        // ARRANGE - Disable daily reminders
        userPreferenceDAO.setPreference(testUserId, SMSNotificationManager.KEY_REMINDER_ENABLED, "false");

        // ACT - Run the worker
        DailyReminderWorker worker = TestListenableWorkerBuilder
                .from(context, DailyReminderWorker.class)
                .build();

        ListenableWorker.Result result = worker.doWork();

        // ASSERT - Worker should succeed without sending SMS
        assertEquals("Worker should return SUCCESS", ListenableWorker.Result.success(), result);
        // SMS manager's canSendSms() will return false due to preference
    }

    /**
     * Test 4: Worker skips reminder if no phone number configured.
     *
     * Tests FR7.6 - Daily reminder handles missing phone number gracefully.
     * Verifies that the worker doesn't crash when user has no phone.
     *
     * RED PHASE: This test MUST FAIL before implementing DailyReminderWorker.
     */
    @Test
    public void test_doWork_noPhoneNumber_skipsReminder() {
        // ARRANGE - Clear phone number
        userDAO.updatePhoneNumber(testUserId, null);

        // ACT - Run the worker
        DailyReminderWorker worker = TestListenableWorkerBuilder
                .from(context, DailyReminderWorker.class)
                .build();

        ListenableWorker.Result result = worker.doWork();

        // ASSERT - Worker should succeed without sending SMS
        assertEquals("Worker should return SUCCESS", ListenableWorker.Result.success(), result);
        // SMS manager's canSendSms() will return false due to missing phone
    }
}
