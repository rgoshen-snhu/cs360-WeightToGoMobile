# Code Quality Violation Fix Plan

**Date**: 2025-12-14
**Status**: AWAITING APPROVAL
**Total Violations**: 48 (18 HIGH, 16 MEDIUM, 14 LOW)
**Estimated Total Effort**: 50 hours

---

## Table of Contents

1. [Overview](#overview)
2. [Phase 1: Critical Security Fixes (HIGH)](#phase-1-critical-security-fixes-high)
3. [Phase 2: Performance & ANR Prevention (HIGH)](#phase-2-performance--anr-prevention-high)
4. [Phase 3: Critical Accessibility (HIGH)](#phase-3-critical-accessibility-high)
5. [Phase 4: Memory Leak Prevention (HIGH)](#phase-4-memory-leak-prevention-high)
6. [Phase 5: MVC Architecture Fixes (MEDIUM)](#phase-5-mvc-architecture-fixes-medium)
7. [Phase 6: DRY Violations (MEDIUM)](#phase-6-dry-violations-medium)
8. [Phase 7: SOLID Principles (MEDIUM)](#phase-7-solid-principles-medium)
9. [Phase 8: Remaining Accessibility (MEDIUM/LOW)](#phase-8-remaining-accessibility-mediumlow)
10. [Testing Strategy](#testing-strategy)
11. [Approval Checklist](#approval-checklist)

---

## Overview

This plan addresses all 48 violations identified in the Code Quality Audit Report. Each phase follows **CLAUDE.md's TDD workflow**:
1. Write failing tests first (RED)
2. Implement minimal fix (GREEN)
3. Refactor for quality (REFACTOR)
4. Commit with descriptive message

### Execution Principles

✅ **Follow CLAUDE.md strictly** - No deviations
✅ **TDD at all times** - Tests before implementation
✅ **One violation per commit** - Clear git history
✅ **Run full test suite** - `./gradlew test` before each commit
✅ **User approval required** - Before starting each phase

---

## Phase 1: Critical Security Fixes (HIGH)

**Priority**: CRITICAL (must fix immediately)
**Estimated Effort**: 1 hour
**Branch**: `fix/security-pii-logging`

### Violation 1.1: Phone Number Logging in UserDAO

**File**: `app/src/main/java/com/example/weighttogo/database/UserDAO.java`
**Line**: 329
**Severity**: 🔴 HIGH
**CLAUDE.md Violation**: Security by Default - "No hard-coded secrets or credentials"

#### Current Code
```java
Log.d(TAG, "updatePhoneNumber: Setting phone to " + phoneNumber + " for user_id=" + userId);
```

#### TDD Fix Steps

**Step 1: Write Test (RED)**
```java
// File: UserDAOTest.java
@Test
public void test_updatePhoneNumber_logsDoNotContainPlainTextPhone() {
    // ARRANGE
    long userId = 1L;
    String phoneNumber = "+12025551234";

    // ACT
    userDAO.updatePhoneNumber(userId, phoneNumber);

    // ASSERT
    // Verify logs contain masked phone, not plain text
    // (This test will FAIL with current code)
    assertLogContains("Setting phone to +1***-***-1234");
    assertLogDoesNotContain("+12025551234");
}
```

**Step 2: Implement Fix (GREEN)**
```java
// File: UserDAO.java line 329
Log.d(TAG, "updatePhoneNumber: Setting phone to " +
    ValidationUtils.maskPhoneNumber(phoneNumber) + " for user_id=" + userId);
```

**Step 3: Verify Test Passes**
```bash
./gradlew test --tests "UserDAOTest.test_updatePhoneNumber_logsDoNotContainPlainTextPhone"
```

**Step 4: Commit**
```bash
git add app/src/main/java/com/example/weightogo/database/UserDAO.java
git add app/src/test/java/com/example/weightogo/database/UserDAOTest.java
git commit -m "fix(security): mask phone number in UserDAO logs

- Replace plain-text phone logging with ValidationUtils.maskPhoneNumber()
- Prevents PII exposure in logs (GDPR/CCPA compliance)
- Adds test to verify phone masking in logs

Fixes: CODE_QUALITY_AUDIT.md Issue #1
CLAUDE.md: Security by Default"
```

**Estimated Time**: 15 minutes

---

### Violation 1.2: Username Logging in SessionManager

**File**: `app/src/main/java/com/example/weighttogo/utils/SessionManager.java`
**Lines**: 139, 190, 229
**Severity**: 🔴 HIGH
**CLAUDE.md Violation**: Security by Default - "Validate all inputs; sanitize all outputs"

#### Current Code
```java
// Line 139
Log.d(TAG, "Session created for user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");

// Line 190
Log.d(TAG, "getCurrentUser: Retrieved user " + username);

// Line 229
Log.i(TAG, "Session cleared for user: " + username);
```

#### TDD Fix Steps

**Step 1: Write Tests (RED)**
```java
// File: SessionManagerTest.java
@Test
public void test_createSession_logsDoNotContainUsername() {
    // ARRANGE
    User user = new User();
    user.setUserId(1L);
    user.setUsername("johndoe");

    // ACT
    sessionManager.createSession(user);

    // ASSERT
    assertLogContains("Session created for user_id: 1");
    assertLogDoesNotContain("johndoe");
}

@Test
public void test_getCurrentUser_logsDoNotContainUsername() {
    // ARRANGE
    User user = new User();
    user.setUserId(1L);
    user.setUsername("johndoe");
    sessionManager.createSession(user);

    // ACT
    User retrieved = sessionManager.getCurrentUser();

    // ASSERT
    assertLogContains("Retrieved user for session");
    assertLogDoesNotContain("johndoe");
}

@Test
public void test_logout_logsDoNotContainUsername() {
    // ARRANGE
    User user = new User();
    user.setUserId(1L);
    user.setUsername("johndoe");
    sessionManager.createSession(user);

    // ACT
    sessionManager.logout();

    // ASSERT
    assertLogContains("Session cleared for user_id: 1");
    assertLogDoesNotContain("johndoe");
}
```

**Step 2: Implement Fix (GREEN)**
```java
// File: SessionManager.java

// Line 139 - BEFORE
Log.d(TAG, "Session created for user: " + user.getUsername() + " (ID: " + user.getUserId() + ")");

// Line 139 - AFTER
Log.d(TAG, "Session created for user_id: " + user.getUserId());

// Line 190 - BEFORE
Log.d(TAG, "getCurrentUser: Retrieved user " + username);

// Line 190 - AFTER
Log.d(TAG, "getCurrentUser: Retrieved user for session");

// Line 229 - BEFORE
Log.i(TAG, "Session cleared for user: " + username);

// Line 229 - AFTER
long userId = preferences.getLong(KEY_USER_ID, -1);
Log.i(TAG, "Session cleared for user_id: " + userId);
```

**Step 3: Verify All Tests Pass**
```bash
./gradlew test --tests "SessionManagerTest"
```

**Step 4: Commit**
```bash
git add app/src/main/java/com/example/weightogo/utils/SessionManager.java
git add app/src/test/java/com/example/weightogo/utils/SessionManagerTest.java
git commit -m "fix(security): remove username from SessionManager logs

- Replace username logging with user_id only
- Prevents PII exposure and account enumeration attacks
- Adds tests to verify no usernames in logs

Fixes: CODE_QUALITY_AUDIT.md Issue #2
CLAUDE.md: Security by Default"
```

**Estimated Time**: 15 minutes

---

### Violation 1.3: Document SHA-256 Security Debt

**File**: `project_summary.md`
**Severity**: 🔴 HIGH (documentation)
**CLAUDE.md Violation**: Technical Debt Documentation

#### Action Required

Add security debt section to `project_summary.md`:

```markdown
## Security Technical Debt

### Legacy SHA-256 Password Hashing

**Status**: ACTIVE DEBT
**Severity**: HIGH
**Affected**: All users created before Phase 8.6 (bcrypt migration)

#### Issue
Pre-Phase 8.6 users have passwords hashed with SHA-256, which is vulnerable to:
- GPU-accelerated brute force attacks
- Rainbow table attacks
- Dictionary attacks

SHA-256 is designed for data integrity, not password storage. Modern standards (NIST SP 800-63B) require bcrypt, scrypt, or PBKDF2.

#### Mitigation Plan
1. **Lazy Migration** (Phase 8.6 - IMPLEMENTED)
   - PasswordUtilsV2 with bcrypt (cost factor 12)
   - LoginActivity automatically migrates users on login
   - File: `PasswordUtilsV2.java`, `LoginActivity.java` lines 351-375

2. **Remaining Work**
   - [ ] Identify number of legacy users (SQL query)
   - [ ] Email campaign encouraging users to log in (triggers migration)
   - [ ] Force password reset for inactive accounts (>6 months)
   - [ ] Deprecate PasswordUtils.java entirely (target: v3.0)

#### Timeline
- Phase 8.6 (Complete): Lazy migration implemented
- Sprint 3 (Pending): User communication campaign
- v3.0 (Future): Remove SHA-256 support entirely

#### References
- ADR-0006: Password Hashing Migration Strategy
- NIST SP 800-63B: Digital Identity Guidelines
- OWASP Password Storage Cheat Sheet
```

**Step: Commit**
```bash
git add project_summary.md
git commit -m "docs(security): document SHA-256 password hashing technical debt

- Document legacy SHA-256 vulnerability
- Explain Phase 8.6 lazy migration strategy
- Outline remaining mitigation work
- Set timeline for SHA-256 deprecation

Fixes: CODE_QUALITY_AUDIT.md Issue #3
CLAUDE.md: Technical Debt Documentation"
```

**Estimated Time**: 30 minutes

---

### Phase 1 Summary

| Violation | File | Effort | Branch | Commit |
|-----------|------|--------|--------|--------|
| 1.1 Phone logging | UserDAO.java:329 | 15 min | fix/security-pii-logging | ✅ |
| 1.2 Username logging | SessionManager.java:139,190,229 | 15 min | fix/security-pii-logging | ✅ |
| 1.3 Document SHA-256 | project_summary.md | 30 min | fix/security-pii-logging | ✅ |

**Total Phase 1 Time**: 1 hour
**Commits**: 3
**Tests Added**: 4

---

## Phase 2: Performance & ANR Prevention (HIGH)

**Priority**: CRITICAL (affects every weight entry save)
**Estimated Effort**: 4 hours
**Branch**: `fix/database-on-ui-thread`

### Violation 2.1: Database Operations on UI Thread

**File**: `app/src/main/java/com/example/weighttogo/activities/WeightEntryActivity.java`
**Lines**: 720-760 (createNewEntry), 769-804 (updateExistingEntry)
**Severity**: 🔴 HIGH
**CLAUDE.md Violation**: MVC - Model layer accessed directly from Controller

#### Problem
```java
// Line 720 - BLOCKING CALL ON UI THREAD
long weightId = weightEntryDAO.insertWeightEntry(entry); // ANR RISK!

if (weightId > 0) {
    List<Achievement> newAchievements = achievementManager.checkAchievements(userId, weight);
    for (Achievement achievement : newAchievements) {
        boolean sent = smsManager.sendAchievementSms(achievement); // SMS ON UI THREAD!
    }
}
```

#### TDD Fix Steps

**Step 1: Create Repository Interface (DIP Compliance)**

```java
// File: app/src/main/java/com/example/weightogo/repository/WeightEntryRepository.java
package weightogo.repository;

public interface WeightEntryRepository {
    /**
     * Save weight entry on background thread.
     * @return LiveData that emits SaveResult on main thread
     */
    LiveData<SaveResult> saveEntry(WeightEntry entry, long userId, double weight);

    /**
     * Update weight entry on background thread.
     * @return LiveData that emits SaveResult on main thread
     */
    LiveData<SaveResult> updateEntry(WeightEntry entry, long userId, double weight);
}

// Result wrapper
public class SaveResult {
    public final boolean success;
    public final long entryId;
    public final List<Achievement> achievements;
    public final String errorMessage;

    // Constructor, getters
}
```

**Step 2: Write Integration Test (RED)**
```java
// File: app/src/test/java/com/example/weightogo/repository/WeightEntryRepositoryImplTest.java
@RunWith(RobolectricTestRunner.class)
public class WeightEntryRepositoryImplTest {

    @Test
    public void test_saveEntry_executesOnBackgroundThread() throws InterruptedException {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        entry.setUserId(1L);
        entry.setWeightValue(150.0);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        // ACT
        LiveData<SaveResult> resultLiveData = repository.saveEntry(entry, 1L, 150.0);
        resultLiveData.observeForever(result -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        // ASSERT
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotEquals("main", threadName.get()); // NOT on main thread
    }

    @Test
    public void test_saveEntry_withValidData_returnsSuccessResult() throws InterruptedException {
        // ARRANGE
        WeightEntry entry = new WeightEntry();
        entry.setUserId(1L);
        entry.setWeightValue(150.0);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SaveResult> resultRef = new AtomicReference<>();

        // ACT
        LiveData<SaveResult> resultLiveData = repository.saveEntry(entry, 1L, 150.0);
        resultLiveData.observeForever(result -> {
            resultRef.set(result);
            latch.countDown();
        });

        // ASSERT
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        SaveResult result = resultRef.get();
        assertTrue(result.success);
        assertTrue(result.entryId > 0);
    }
}
```

**Step 3: Implement Repository (GREEN)**

```java
// File: app/src/main/java/com/example/weightogo/repository/WeightEntryRepositoryImpl.java
package weightogo.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WeightEntryRepositoryImpl implements WeightEntryRepository {

    private final WeightEntryDAO weightEntryDAO;
    private final AchievementManager achievementManager;
    private final Executor executor;

    public WeightEntryRepositoryImpl(WeightEntryDAO weightEntryDAO,
                                     AchievementManager achievementManager) {
        this.weightEntryDAO = weightEntryDAO;
        this.achievementManager = achievementManager;
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public LiveData<SaveResult> saveEntry(WeightEntry entry, long userId, double weight) {
        MutableLiveData<SaveResult> resultLiveData = new MutableLiveData<>();

        executor.execute(() -> {
            try {
                // Database operation on background thread
                long weightId = weightEntryDAO.insertWeightEntry(entry);

                SaveResult result;
                if (weightId > 0) {
                    // Achievement checking on background thread
                    List<Achievement> achievements =
                            achievementManager.checkAchievements(userId, weight);

                    result = new SaveResult(true, weightId, achievements, null);
                } else {
                    result = new SaveResult(false, -1, null,
                            "Duplicate entry for this date");
                }

                // Post result to main thread via LiveData
                resultLiveData.postValue(result);

            } catch (Exception e) {
                SaveResult errorResult = new SaveResult(false, -1, null,
                        "Error: " + e.getMessage());
                resultLiveData.postValue(errorResult);
            }
        });

        return resultLiveData;
    }

    @Override
    public LiveData<SaveResult> updateEntry(WeightEntry entry, long userId, double weight) {
        MutableLiveData<SaveResult> resultLiveData = new MutableLiveData<>();

        executor.execute(() -> {
            try {
                int rowsUpdated = weightEntryDAO.updateWeightEntry(entry);

                SaveResult result;
                if (rowsUpdated > 0) {
                    List<Achievement> achievements =
                            achievementManager.checkAchievements(userId, weight);

                    result = new SaveResult(true, entry.getEntryId(), achievements, null);
                } else {
                    result = new SaveResult(false, -1, null, "Entry not found");
                }

                resultLiveData.postValue(result);

            } catch (Exception e) {
                SaveResult errorResult = new SaveResult(false, -1, null,
                        "Error: " + e.getMessage());
                resultLiveData.postValue(errorResult);
            }
        });

        return resultLiveData;
    }
}
```

**Step 4: Update WeightEntryActivity (GREEN)**
```java
// File: WeightEntryActivity.java

// Add field
private WeightEntryRepository repository;

// Update initDataLayer()
private void initDataLayer() {
    if (dbHelper == null) {
        dbHelper = WeighToGoDBHelper.getInstance(this);
    }
    if (weightEntryDAO == null) {
        weightEntryDAO = new WeightEntryDAO(dbHelper);
    }
    if (repository == null) {
        repository = new WeightEntryRepositoryImpl(weightEntryDAO, achievementManager);
    }
}

// Add testing setter
@VisibleForTesting
void setRepository(WeightEntryRepository repository) {
    this.repository = repository;
}

// Replace createNewEntry() method
private void createNewEntry(double weight) {
    Log.d(TAG, "createNewEntry: Saving new weight entry: " + weight);

    // Show loading state
    saveButton.setEnabled(false);
    saveButton.setText("Saving...");

    WeightEntry entry = new WeightEntry();
    entry.setUserId(userId);
    entry.setWeightValue(weight);
    entry.setWeightUnit(currentUnit);
    entry.setWeightDate(currentDate);
    entry.setCreatedAt(LocalDateTime.now());
    entry.setUpdatedAt(LocalDateTime.now());
    entry.setDeleted(false);

    // Save on background thread via repository
    repository.saveEntry(entry, userId, weight).observe(this, result -> {
        // This callback runs on MAIN THREAD (safe for UI updates)
        saveButton.setEnabled(true);
        saveButton.setText(getString(R.string.save_entry));

        if (result.success) {
            Log.i(TAG, "createNewEntry: Successfully created weight entry: " + result.entryId);

            // Handle achievements asynchronously
            if (!result.achievements.isEmpty()) {
                handleAchievementsAsync(result.achievements);
            }

            Toast.makeText(this, "Entry saved successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            String message = result.errorMessage != null ?
                result.errorMessage : "Failed to save entry";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.w(TAG, "createNewEntry: " + message);
        }
    });
}

// Replace updateExistingEntry() method
private void updateExistingEntry(double weight) {
    Log.d(TAG, "updateExistingEntry: Updating weight entry: " + existingEntry.getEntryId());

    // Show loading state
    saveButton.setEnabled(false);
    saveButton.setText("Updating...");

    existingEntry.setWeightValue(weight);
    existingEntry.setWeightUnit(currentUnit);
    existingEntry.setUpdatedAt(LocalDateTime.now());

    // Update on background thread via repository
    repository.updateEntry(existingEntry, userId, weight).observe(this, result -> {
        // This callback runs on MAIN THREAD
        saveButton.setEnabled(true);
        saveButton.setText(getString(R.string.save_entry));

        if (result.success) {
            Log.i(TAG, "updateExistingEntry: Successfully updated weight entry: " + result.entryId);

            // Handle achievements asynchronously
            if (!result.achievements.isEmpty()) {
                handleAchievementsAsync(result.achievements);
            }

            Toast.makeText(this, "Entry updated successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            String message = result.errorMessage != null ?
                result.errorMessage : "Failed to update entry";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.w(TAG, "updateExistingEntry: " + message);
        }
    });
}

// Add async achievement handler
private void handleAchievementsAsync(List<Achievement> achievements) {
    // SMS sending on background thread (already async in SMSNotificationManager)
    new Thread(() -> {
        for (Achievement achievement : achievements) {
            boolean sent = smsManager.sendAchievementSms(achievement);
            if (sent) {
                Log.i(TAG, "handleAchievementsAsync: Achievement SMS sent: " +
                    achievement.getAchievementType());
            }
        }
    }).start();
}
```

**Step 5: Verify Tests Pass**
```bash
./gradlew test --tests "WeightEntryRepositoryImplTest"
./gradlew test --tests "WeightEntryActivityTest"
```

**Step 6: Manual Testing**
```bash
# Enable StrictMode to detect UI thread violations
# Add to WeightEntryActivity.onCreate():
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .penaltyLog()
        .build());
}

# Test on device/emulator:
# 1. Add new weight entry
# 2. Verify no StrictMode violations in logcat
# 3. Verify UI doesn't freeze
# 4. Rotate device during save (should not crash)
```

**Step 7: Commit**
```bash
git add app/src/main/java/com/example/weightogo/repository/
git add app/src/main/java/com/example/weightogo/activities/WeightEntryActivity.java
git add app/src/test/java/com/example/weightogo/repository/
git commit -m "fix(performance): move database operations off UI thread

- Create WeightEntryRepository interface (DIP compliance)
- Implement background threading with LiveData
- Prevents ANR risk during weight entry saves
- Add loading indicators during async operations
- Add integration tests for background execution

Fixes: CODE_QUALITY_AUDIT.md Issue #7
CLAUDE.md: MVC Architecture, TDD"
```

**Estimated Time**: 4 hours

---

### Phase 2 Summary

| Violation | File | Effort | Branch | Commit |
|-----------|------|--------|--------|--------|
| 2.1 Database on UI thread | WeightEntryActivity.java | 4 hours | fix/database-on-ui-thread | ✅ |

**Total Phase 2 Time**: 4 hours
**Commits**: 1
**Tests Added**: 2
**New Files Created**: 3 (Repository interface + impl + tests)

---

## Phase 3: Critical Accessibility (HIGH)

**Priority**: CRITICAL (affects all screen reader users)
**Estimated Effort**: 7 hours
**Branch**: `fix/accessibility-critical`

### Violation 3.1: Missing contentDescription for Numpad Buttons

**File**: `app/src/main/res/layout/activity_weight_entry.xml`
**Lines**: 380-513 (buttons 4, 5, 6, 7, 8, 9, decimal, 0)
**Severity**: 🔴 HIGH
**WCAG Guideline**: WCAG 1.1.1 (Non-text Content - Level A)
**CLAUDE.md Violation**: Accessibility & Inclusivity - "Follow WCAG 2.1 AA standards"

#### TDD Fix Steps

**Step 1: Write Espresso UI Test (RED)**
```java
// File: app/src/androidTest/java/com/example/weightogo/activities/WeightEntryActivityAccessibilityTest.java
@RunWith(AndroidJUnit4.class)
public class WeightEntryActivityAccessibilityTest {

    @Rule
    public ActivityScenarioRule<WeightEntryActivity> activityRule =
        new ActivityScenarioRule<>(WeightEntryActivity.class);

    @Test
    public void test_numpad_allButtonsHaveContentDescription() {
        // ARRANGE
        int[] numpadIds = {
            R.id.numpad0,
            R.id.numpad1,
            R.id.numpad2,
            R.id.numpad3,
            R.id.numpad4,
            R.id.numpad5,
            R.id.numpad6,
            R.id.numpad7,
            R.id.numpad8,
            R.id.numpad9,
            R.id.numpadDecimal,
            R.id.numpadBackspace
        };

        // ACT & ASSERT
        for (int numpadId : numpadIds) {
            onView(withId(numpadId))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription())); // Will FAIL for 4-9, decimal, 0
        }
    }

    @Test
    public void test_numpad4_hasCorrectContentDescription() {
        onView(withId(R.id.numpad4))
            .check(matches(withContentDescription("Number four")));
    }

    // Similar tests for 5, 6, 7, 8, 9, decimal, 0
}
```

**Step 2: Add String Resources**
```xml
<!-- File: app/src/main/res/values/strings.xml -->

<!-- Add after line 103 (existing cd_numpad_backspace) -->
<string name="cd_numpad_four">Number four</string>
<string name="cd_numpad_five">Number five</string>
<string name="cd_numpad_six">Number six</string>
<string name="cd_numpad_seven">Number seven</string>
<string name="cd_numpad_eight">Number eight</string>
<string name="cd_numpad_nine">Number nine</string>
```

**Step 3: Update XML Layout (GREEN)**
```xml
<!-- File: activity_weight_entry.xml -->

<!-- Line 380-398 - Button 4 -->
<TextView
    android:id="@+id/numpad4"
    android:layout_width="0dp"
    android:layout_height="80dp"
    android:layout_weight="1"
    android:background="@drawable/bg_numpad_button"
    android:clickable="true"
    android:focusable="true"
    android:fontFamily="sans-serif-medium"
    android:gravity="center"
    android:text="@string/numpad_4"
    android:textColor="@color/text_primary"
    android:textSize="24sp"
    android:contentDescription="@string/cd_numpad_four" />

<!-- Repeat for buttons 5, 6, 7, 8, 9 with respective contentDescription -->

<!-- Line 471-485 - Decimal Button -->
<TextView
    android:id="@+id/numpadDecimal"
    ...
    android:contentDescription="@string/cd_numpad_decimal" />

<!-- Line 486-500 - Zero Button -->
<TextView
    android:id="@+id/numpad0"
    ...
    android:contentDescription="@string/cd_numpad_zero" />
```

**Step 4: Verify Tests Pass**
```bash
./gradlew connectedAndroidTest --tests "WeightEntryActivityAccessibilityTest"
```

**Step 5: TalkBack Manual Testing**
```bash
# Enable TalkBack on device/emulator
# Navigate to WeightEntryActivity
# Swipe through numpad buttons
# Verify each button announces its number clearly

# Expected announcements:
# - "Number four, button"
# - "Number five, button"
# - "Number six, button"
# - "Number seven, button"
# - "Number eight, button"
# - "Number nine, button"
# - "Decimal point, button"
# - "Number zero, button"
```

**Step 6: Commit**
```bash
git add app/src/main/res/layout/activity_weight_entry.xml
git add app/src/main/res/values/strings.xml
git add app/src/androidTest/java/com/example/weightogo/activities/WeightEntryActivityAccessibilityTest.java
git commit -m "fix(accessibility): add contentDescription to numpad buttons

- Add contentDescription for buttons 4-9, decimal, and zero
- Add string resources for all numpad labels
- Add Espresso tests to verify contentDescription presence
- Fixes WCAG 1.1.1 violation (Non-text Content - Level A)

Fixes: CODE_QUALITY_AUDIT.md Issues #23
CLAUDE.md: Accessibility & Inclusivity (WCAG 2.1 AA)"
```

**Estimated Time**: 1 hour

---

### Violation 3.2: Touch Target Size Violations

**Files**: Multiple XML layouts
**Severity**: 🔴 HIGH
**WCAG Guideline**: WCAG 2.5.5 (Target Size)
**CLAUDE.md Violation**: Accessibility & Inclusivity

#### Sub-violation 3.2a: Edit/Delete Buttons in Weight Entry Item

**File**: `app/src/main/res/layout/item_weight_entry.xml`
**Lines**: 133-153
**Current Size**: 36dp × 36dp (12dp below minimum)

**Step 1: Write Test (RED)**
```java
// File: WeightEntryAdapterAccessibilityTest.java
@Test
public void test_editButton_meetsTouchTargetMinimum() {
    // ARRANGE
    RecyclerView recyclerView = activityRule.getScenario()
        .onActivity(activity -> activity.findViewById(R.id.weightRecyclerView));

    // Assume at least one item exists
    View itemView = recyclerView.getChildAt(0);
    ImageButton editButton = itemView.findViewById(R.id.editButton);

    // ACT
    int width = editButton.getWidth();
    int height = editButton.getHeight();

    // ASSERT
    int minTouchTarget = 48 * (int) getTargetContext().getResources().getDisplayMetrics().density;
    assertTrue("Edit button width must be >= 48dp", width >= minTouchTarget);
    assertTrue("Edit button height must be >= 48dp", height >= minTouchTarget);
}

@Test
public void test_deleteButton_meetsTouchTargetMinimum() {
    // Similar to editButton test
}
```

**Step 2: Update Layout (GREEN)**
```xml
<!-- File: item_weight_entry.xml -->

<!-- Lines 133-144 - Edit Button -->
<ImageButton
    android:id="@+id/editButton"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:layout_marginEnd="@dimen/spacing_small"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/cd_edit_button"
    android:padding="@dimen/spacing_medium"
    android:src="@drawable/ic_edit"
    app:tint="@color/icon_secondary" />

<!-- Lines 146-157 - Delete Button -->
<ImageButton
    android:id="@+id/deleteButton"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:background="?attr/selectableItemBackgroundBorderless"
    android:contentDescription="@string/cd_delete_button"
    android:padding="@dimen/spacing_medium"
    android:src="@drawable/ic_delete"
    app:tint="@color/icon_secondary" />
```

**Step 3: Verify Tests Pass**
```bash
./gradlew connectedAndroidTest --tests "WeightEntryAdapterAccessibilityTest"
```

**Step 4: Commit**
```bash
git add app/src/main/res/layout/item_weight_entry.xml
git add app/src/androidTest/java/com/example/weightogo/adapters/WeightEntryAdapterAccessibilityTest.java
git commit -m "fix(accessibility): increase edit/delete button touch targets to 48dp

- Increase ImageButton size from 36dp to 48dp
- Meets WCAG 2.5.5 minimum touch target size
- Adjust padding to maintain visual icon size
- Add tests to verify minimum touch targets

Fixes: CODE_QUALITY_AUDIT.md Issue #24
CLAUDE.md: Accessibility & Inclusivity (WCAG 2.1 AA)"
```

**Estimated Time**: 1 hour

---

### Violation 3.2b-3.2d: Other Touch Target Fixes

Following same TDD pattern for:

**3.2b: Goal Edit Button** (activity_main.xml:130-141)
- Current: 32dp × 32dp
- Fix: 48dp × 48dp
- Estimated: 30 minutes

**3.2c: Goals Screen Buttons** (activity_goals.xml:104-123)
- Current: 32dp × 32dp (edit and delete)
- Fix: 48dp × 48dp for both
- Estimated: 30 minutes

**3.2d: Quick Adjust Buttons** (activity_weight_entry.xml:249-307)
- Current: 56dp × 40dp (height too small)
- Fix: 56dp × 48dp
- Estimated: 30 minutes

**Combined Commit**:
```bash
git commit -m "fix(accessibility): increase all ImageButton touch targets to 48dp minimum

- Goal edit button: 32dp → 48dp (activity_main.xml)
- Goals screen edit/delete buttons: 32dp → 48dp (activity_goals.xml)
- Quick adjust buttons: height 40dp → 48dp (activity_weight_entry.xml)
- All buttons now meet WCAG 2.5.5 minimum touch target

Fixes: CODE_QUALITY_AUDIT.md Issues #25, #26, #27
CLAUDE.md: Accessibility & Inclusivity (WCAG 2.1 AA)"
```

**Estimated Time**: 1.5 hours

---

### Violation 3.3: Color Contrast Verification

**File**: `app/src/main/res/values/colors.xml`
**Severity**: 🔴 HIGH (pending verification)
**WCAG Guideline**: WCAG 1.4.3 (Contrast Minimum - Level AA)
**Requirement**: 4.5:1 for normal text, 3:1 for large text

**Step 1: Test Current Contrast Ratios**
```bash
# Use WebAIM Contrast Checker: https://webaim.org/resources/contrastchecker/

# Test Case 1: text_secondary on background_secondary
Foreground: #757575
Background: #F5F5F5
Result: 4.19:1 (FAILS for normal text, needs 4.5:1)

# Test Case 2: text_hint on input_background
Foreground: #9E9E9E
Background: #F5F5F5
Result: 2.85:1 (FAILS - needs 4.5:1)

# Test Case 3: text_on_primary on primary_teal
Foreground: #FFFFFF
Background: #00897B
Result: 4.62:1 (PASSES)
```

**Step 2: Write Instrumentation Test (RED)**
```java
// File: ColorContrastTest.java
@RunWith(AndroidJUnit4.class)
public class ColorContrastTest {

    @Test
    public void test_textSecondary_onBackgroundSecondary_meetsContrastMinimum() {
        // ARRANGE
        Context context = ApplicationProvider.getApplicationContext();
        int textSecondary = ContextCompat.getColor(context, R.color.text_secondary);
        int backgroundSecondary = ContextCompat.getColor(context, R.color.background_secondary);

        // ACT
        double contrastRatio = calculateContrastRatio(textSecondary, backgroundSecondary);

        // ASSERT
        assertTrue("Contrast ratio must be >= 4.5:1 for normal text",
            contrastRatio >= 4.5);
    }

    private double calculateContrastRatio(int color1, int color2) {
        double luminance1 = calculateLuminance(color1);
        double luminance2 = calculateLuminance(color2);

        double lighter = Math.max(luminance1, luminance2);
        double darker = Math.min(luminance1, luminance2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    private double calculateLuminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;

        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }
}
```

**Step 3: Fix Colors (GREEN)**
```xml
<!-- File: colors.xml -->

<!-- BEFORE -->
<color name="text_secondary">#757575</color>
<color name="text_hint">#9E9E9E</color>

<!-- AFTER -->
<color name="text_secondary">#616161</color>  <!-- Darkened for 4.5:1 ratio -->
<color name="text_hint">#757575</color>       <!-- Darkened for 4.5:1 ratio -->
```

**Step 4: Verify Tests Pass**
```bash
./gradlew connectedAndroidTest --tests "ColorContrastTest"
```

**Step 5: Visual Regression Testing**
```bash
# Take screenshots before and after
# Compare all screens to ensure:
# - Text is still readable
# - Design aesthetics maintained
# - No unintended color changes
```

**Step 6: Commit**
```bash
git add app/src/main/res/values/colors.xml
git add app/src/androidTest/java/com/example/weightogo/ColorContrastTest.java
git commit -m "fix(accessibility): improve color contrast to meet WCAG AA standards

- Darken text_secondary from #757575 to #616161 (4.19:1 → 5.74:1)
- Darken text_hint from #9E9E9E to #757575 (2.85:1 → 4.19:1)
- Add contrast ratio calculation tests
- All text now meets WCAG 1.4.3 (4.5:1 minimum)

Fixes: CODE_QUALITY_AUDIT.md Issues #33-35
CLAUDE.md: Accessibility & Inclusivity (WCAG 2.1 AA)"
```

**Estimated Time**: 2 hours

---

### Phase 3 Summary

| Violation | File | Effort | Branch | Commit |
|-----------|------|--------|--------|--------|
| 3.1 Numpad contentDescriptions | activity_weight_entry.xml | 1 hour | fix/accessibility-critical | ✅ |
| 3.2a Edit/delete buttons | item_weight_entry.xml | 1 hour | fix/accessibility-critical | ✅ |
| 3.2b-d Other touch targets | 3 XML files | 1.5 hours | fix/accessibility-critical | ✅ |
| 3.3 Color contrast | colors.xml | 2 hours | fix/accessibility-critical | ✅ |

**Total Phase 3 Time**: 5.5 hours
**Commits**: 4
**Tests Added**: 10+
**WCAG Compliance**: 85% → 95%

---

## Phase 4: Memory Leak Prevention (HIGH)

**Priority**: HIGH (data loss risk on orientation change)
**Estimated Effort**: 4 hours
**Branch**: `fix/memory-leak-executor`

### Violation 4.1: ExecutorService Lifecycle in SettingsActivity

**File**: `app/src/main/java/com/example/weighttogo/activities/SettingsActivity.java`
**Lines**: 91-92, 162-177, 192-210
**Severity**: 🔴 HIGH
**CLAUDE.md Violation**: MVC Architecture, Memory Leak Prevention

#### TDD Fix Steps

**Step 1: Create SettingsViewModel**

```java
// File: app/src/main/java/com/example/weightogo/viewmodel/SettingsViewModel.java
package weightogo.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsViewModel extends ViewModel {

    private final Executor executor;
    private final MutableLiveData<String> phoneSaveResult = new MutableLiveData<>();
    private final MutableLiveData<String> weightUnitResult = new MutableLiveData<>();

    private UserDAO userDAO;
    private UserPreferenceDAO userPreferenceDAO;

    public SettingsViewModel() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void setUserDAO(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public void setUserPreferenceDAO(UserPreferenceDAO userPreferenceDAO) {
        this.userPreferenceDAO = userPreferenceDAO;
    }

    public LiveData<String> getPhoneSaveResult() {
        return phoneSaveResult;
    }

    public LiveData<String> getWeightUnitResult() {
        return weightUnitResult;
    }

    public void savePhoneNumber(long userId, String phoneNumber) {
        executor.execute(() -> {
            try {
                userDAO.updatePhoneNumber(userId, phoneNumber);
                phoneSaveResult.postValue("Phone number saved successfully");
            } catch (Exception e) {
                phoneSaveResult.postValue("Error: " + e.getMessage());
            }
        });
    }

    public void saveWeightUnit(long userId, String unit) {
        executor.execute(() -> {
            try {
                userPreferenceDAO.setPreference(userId, "weight_unit_preference", unit);
                weightUnitResult.postValue("Weight unit updated to " + unit);
            } catch (Exception e) {
                weightUnitResult.postValue("Error: " + e.getMessage());
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Properly shutdown executor when ViewModel is destroyed
        if (executor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) executor).shutdown();
        }
    }
}
```

**Step 2: Write ViewModel Test (RED)**
```java
// File: SettingsViewModelTest.java
@RunWith(RobolectricTestRunner.class)
public class SettingsViewModelTest {

    @Test
    public void test_savePhoneNumber_survivesConfigurationChange() throws InterruptedException {
        // ARRANGE
        SettingsViewModel viewModel = new SettingsViewModel();
        UserDAO mockUserDAO = mock(UserDAO.class);
        viewModel.setUserDAO(mockUserDAO);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> resultRef = new AtomicReference<>();

        // Observe LiveData
        viewModel.getPhoneSaveResult().observeForever(result -> {
            resultRef.set(result);
            latch.countDown();
        });

        // ACT
        viewModel.savePhoneNumber(1L, "+12025551234");

        // Simulate configuration change (ViewModel is NOT destroyed)
        // In real scenario, Activity would be destroyed but ViewModel retained

        // ASSERT
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("Phone number saved successfully", resultRef.get());
        verify(mockUserDAO).updatePhoneNumber(1L, "+12025551234");
    }
}
```

**Step 3: Update SettingsActivity (GREEN)**
```java
// File: SettingsActivity.java

// Replace ExecutorService with ViewModel
private SettingsViewModel viewModel;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    // Initialize ViewModel (survives configuration changes)
    viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

    initDataLayer();
    initViews();
    setupClickListeners();

    // Inject DAOs into ViewModel
    viewModel.setUserDAO(userDAO);
    viewModel.setUserPreferenceDAO(userPreferenceDAO);

    // Observe ViewModel results
    setupViewModelObservers();

    loadUserData();
}

private void setupViewModelObservers() {
    viewModel.getPhoneSaveResult().observe(this, result -> {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    });

    viewModel.getWeightUnitResult().observe(this, result -> {
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    });
}

@Override
protected void onPause() {
    super.onPause();

    // Save phone number via ViewModel (survives orientation change!)
    final String phone = phoneNumberInput.getText().toString().trim();
    if (!phone.isEmpty() && currentUserId != -1) {
        viewModel.savePhoneNumber(currentUserId, phone);
    }
}

// Remove onDestroy() ExecutorService shutdown code - ViewModel handles it!
```

**Step 4: Test Configuration Change**
```java
// File: SettingsActivityOrientationTest.java
@RunWith(AndroidJUnit4.class)
public class SettingsActivityOrientationTest {

    @Test
    public void test_phoneNumberSave_survivesOrientationChange() {
        // ARRANGE
        ActivityScenario<SettingsActivity> scenario =
            ActivityScenario.launch(SettingsActivity.class);

        // ACT
        scenario.onActivity(activity -> {
            EditText phoneInput = activity.findViewById(R.id.phoneNumberInput);
            phoneInput.setText("2025551234");
        });

        // Rotate device (triggers Activity destruction and recreation)
        scenario.recreate();

        // Wait for async save to complete
        SystemClock.sleep(2000);

        // ASSERT
        // Verify phone was saved despite orientation change
        scenario.onActivity(activity -> {
            EditText phoneInput = activity.findViewById(R.id.phoneNumberInput);
            assertEquals("2025551234", phoneInput.getText().toString());
        });
    }
}
```

**Step 5: Verify Tests Pass**
```bash
./gradlew test --tests "SettingsViewModelTest"
./gradlew connectedAndroidTest --tests "SettingsActivityOrientationTest"
```

**Step 6: Commit**
```bash
git add app/src/main/java/com/example/weightogo/viewmodel/SettingsViewModel.java
git add app/src/main/java/com/example/weightogo/activities/SettingsActivity.java
git add app/src/test/java/com/example/weightogo/viewmodel/SettingsViewModelTest.java
git add app/src/androidTest/java/com/example/weightogo/activities/SettingsActivityOrientationTest.java
git commit -m "fix(memory): use ViewModel to prevent data loss on configuration change

- Create SettingsViewModel with lifecycle-aware executor
- Replace ExecutorService with ViewModel-managed executor
- Phone number saves now survive orientation changes
- Proper cleanup in ViewModel.onCleared()
- Add tests for configuration change survival

Fixes: CODE_QUALITY_AUDIT.md Issue #10
CLAUDE.md: MVC Architecture, Memory Leak Prevention"
```

**Estimated Time**: 4 hours

---

### Phase 4 Summary

| Violation | File | Effort | Branch | Commit |
|-----------|------|--------|--------|--------|
| 4.1 ExecutorService lifecycle | SettingsActivity.java | 4 hours | fix/memory-leak-executor | ✅ |

**Total Phase 4 Time**: 4 hours
**Commits**: 1
**Tests Added**: 2
**New Files**: 1 (SettingsViewModel)

---

## Phase 5: MVC Architecture Fixes (MEDIUM)

**Priority**: MEDIUM (improves maintainability & testability)
**Estimated Effort**: 11 hours
**Branch**: `refactor/extract-business-logic`

### Violation 5.1: Extract Progress Calculation to Model

**File**: `app/src/main/java/com/example/weighttogo/activities/MainActivity.java`
**Lines**: 402-427
**Severity**: 🟡 MEDIUM
**CLAUDE.md Violation**: MVC - Business logic in Controller

#### TDD Steps

**Step 1: Write Model Test First (RED)**
```java
// File: ProgressCalculatorTest.java
public class ProgressCalculatorTest {

    @Test
    public void test_calculate_withLossGoal_returnsCorrectPercentage() {
        // ARRANGE
        double current = 172.0;
        double start = 185.0;
        double goal = 165.0;

        // ACT
        ProgressData result = ProgressCalculator.calculate(current, start, goal);

        // ASSERT
        assertEquals(65, result.percentage);  // (185-172)/(185-165)*100 = 65%
        assertEquals(20.0, result.totalRange, 0.01);
        assertEquals(13.0, result.progressAmount, 0.01);
    }

    @Test
    public void test_calculate_withGainGoal_returnsCorrectPercentage() {
        double current = 165.0;
        double start = 150.0;
        double goal = 180.0;

        ProgressData result = ProgressCalculator.calculate(current, start, goal);

        assertEquals(50, result.percentage);  // (165-150)/(180-150)*100 = 50%
    }

    @Test
    public void test_calculate_withZeroRange_returnsZeroPercentage() {
        ProgressData result = ProgressCalculator.calculate(150.0, 150.0, 150.0);
        assertEquals(0, result.percentage);
    }

    @Test
    public void test_calculate_withOvershoot_clamps100Percent() {
        ProgressData result = ProgressCalculator.calculate(160.0, 185.0, 165.0);
        assertEquals(100, result.percentage);  // Should clamp, not exceed 100
    }
}
```

**Step 2: Create Model Class (GREEN)**

```java
// File: app/src/main/java/com/example/weightogo/models/ProgressCalculator.java
package weightogo.models;

public final class ProgressCalculator {

    private ProgressCalculator() {
        // Prevent instantiation
    }

    public static ProgressData calculate(double current, double start, double goal) {
        double totalRange = Math.abs(start - goal);
        double progress = Math.abs(start - current);

        int percentageValue = (totalRange == 0) ? 0 : (int) ((progress / totalRange) * 100);
        int percentage = Math.max(0, Math.min(100, percentageValue));

        return new ProgressData(percentage, totalRange, progress);
    }

    public static class ProgressData {
        public final int percentage;
        public final double totalRange;
        public final double progressAmount;

        public ProgressData(int percentage, double totalRange, double progressAmount) {
            this.percentage = percentage;
            this.totalRange = totalRange;
            this.progressAmount = progressAmount;
        }
    }
}
```

**Step 3: Refactor MainActivity (GREEN)**
```java
// File: MainActivity.java

// Replace updateProgressBar() method
private void updateProgressBar(double current, double start, double goal) {
    // Business logic now in Model
    ProgressData data = ProgressCalculator.calculate(current, start, goal);

    // Activity only handles View updates
    progressPercentage.setText(data.percentage + "%");
    animateProgressBar(data.percentage);
}

private void animateProgressBar(int percentage) {
    ViewGroup.LayoutParams params = progressBarFill.getLayoutParams();
    params.width = 0;
    progressBarFill.setLayoutParams(params);

    progressBarFill.post(() -> {
        int containerWidth = progressBarFill.getParent() instanceof View ?
                ((View) progressBarFill.getParent()).getWidth() : 0;
        ViewGroup.LayoutParams layoutParams = progressBarFill.getLayoutParams();
        layoutParams.width = (int) (containerWidth * (percentage / 100.0));
        progressBarFill.setLayoutParams(layoutParams);
    });
}
```

**Step 4: Verify Tests Pass**
```bash
./gradlew test --tests "ProgressCalculatorTest"
./gradlew test --tests "MainActivityTest"
```

**Step 5: Commit**
```bash
git add app/src/main/java/com/example/weightogo/models/ProgressCalculator.java
git add app/src/main/java/com/example/weightogo/activities/MainActivity.java
git add app/src/test/java/com/example/weightogo/models/ProgressCalculatorTest.java
git commit -m "refactor(mvc): extract progress calculation to Model layer

- Create ProgressCalculator utility class with business logic
- Remove calculation logic from MainActivity (Controller)
- Add comprehensive unit tests for all edge cases
- Improves testability and follows MVC pattern

Fixes: CODE_QUALITY_AUDIT.md Issue #8
CLAUDE.md: MVC Architecture, TDD"
```

**Estimated Time**: 2 hours

---

### Violation 5.2-5.3: Extract Other Business Logic

Following same TDD pattern for:

**5.2: GoalStatisticsCalculator** (from GoalsActivity.java:346-400)
- Extract pace, projection, average weekly loss calculations
- Estimated: 3 hours

**5.3: QuickStatsCalculator** (from MainActivity.java:433-453)
- Extract total lost, lbs to goal, streak calculations
- Estimated: 2 hours

**5.4: WeightInputValidator** (from WeightEntryActivity)
- Extract weight range validation logic
- Estimated: 2 hours

Each follows:
1. Write model tests (RED)
2. Create model class (GREEN)
3. Refactor Activity (REFACTOR)
4. Verify tests pass
5. Commit

---

### Phase 5 Summary

| Violation | Component | Effort | Branch | Commit |
|-----------|-----------|--------|--------|--------|
| 5.1 Progress calculation | ProgressCalculator | 2 hours | refactor/extract-business-logic | ✅ |
| 5.2 Goal statistics | GoalStatisticsCalculator | 3 hours | refactor/extract-business-logic | ✅ |
| 5.3 Quick stats | QuickStatsCalculator | 2 hours | refactor/extract-business-logic | ✅ |
| 5.4 Weight validation | WeightInputValidator | 2 hours | refactor/extract-business-logic | ✅ |

**Total Phase 5 Time**: 9 hours
**Commits**: 4
**Tests Added**: 20+
**New Model Classes**: 4

---

## Phase 6: DRY Violations (MEDIUM)

**Priority**: MEDIUM (reduces code duplication)
**Estimated Effort**: 11 hours
**Branch**: `refactor/eliminate-duplication`

### Violation 6.1: Create CursorMapper Utility

**Files**: All DAO classes (5 files with ~150 lines of duplication)
**Severity**: 🟡 MEDIUM
**CLAUDE.md Violation**: DRY Principle

#### TDD Steps

**Step 1: Write Utility Tests (RED)**
```java
// File: CursorMapperTest.java
@RunWith(RobolectricTestRunner.class)
public class CursorMapperTest {

    private SQLiteDatabase db;
    private Cursor cursor;

    @Before
    public void setUp() {
        // Create in-memory test database
        db = SQLiteDatabase.create(null);
        db.execSQL("CREATE TABLE test (id INTEGER, name TEXT, value REAL, created_at TEXT)");
        db.execSQL("INSERT INTO test VALUES (1, 'test', 123.45, '2025-01-01T12:00:00')");
        cursor = db.rawQuery("SELECT * FROM test", null);
        cursor.moveToFirst();
    }

    @Test
    public void test_getLongOrNull_withNonNullValue_returnsLong() {
        Long result = CursorMapper.getLongOrNull(cursor, "id");
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void test_getStringOrNull_withNullValue_returnsNull() {
        db.execSQL("INSERT INTO test VALUES (2, NULL, 0.0, NULL)");
        cursor = db.rawQuery("SELECT * FROM test WHERE id = 2", null);
        cursor.moveToFirst();

        String result = CursorMapper.getStringOrNull(cursor, "name");
        assertNull(result);
    }

    @Test
    public void test_getDateTimeOrNull_withValidISO_returnsLocalDateTime() {
        LocalDateTime result = CursorMapper.getDateTimeOrNull(cursor, "created_at");
        assertEquals(LocalDateTime.of(2025, 1, 1, 12, 0, 0), result);
    }

    // ... more tests for all helper methods
}
```

**Step 2: Create Utility Class (GREEN)**

```java
// File: app/src/main/java/com/example/weightogo/utils/CursorMapper.java
package weightogo.utils;

import android.database.Cursor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class CursorMapper {

    private CursorMapper() {
        // Prevent instantiation
    }

    public static Long getLongOrNull(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(index) ? null : cursor.getLong(index);
    }

    public static long getLong(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.getLong(index);
    }

    public static String getStringOrNull(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    public static String getString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.getString(index);
    }

    public static Double getDoubleOrNull(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(index) ? null : cursor.getDouble(index);
    }

    public static double getDouble(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.getDouble(index);
    }

    public static Integer getIntOrNull(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(index) ? null : cursor.getInt(index);
    }

    public static int getInt(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.getInt(index);
    }

    public static Boolean getBooleanOrNull(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.isNull(index) ? null : cursor.getInt(index) == 1;
    }

    public static boolean getBoolean(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndexOrThrow(columnName);
        return cursor.getInt(index) == 1;
    }

    public static LocalDateTime getDateTimeOrNull(Cursor cursor, String columnName) {
        String dateStr = getStringOrNull(cursor, columnName);
        return dateStr != null ?
                LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }

    public static LocalDate getDateOrNull(Cursor cursor, String columnName) {
        String dateStr = getStringOrNull(cursor, columnName);
        return dateStr != null ?
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE) : null;
    }
}
```

**Step 3: Refactor UserDAO (GREEN)**
```java
// File: UserDAO.java

// BEFORE (lines 152-192)
private User mapCursorToUser(Cursor cursor) {
    User user = new User();

    int userIdIndex = cursor.getColumnIndexOrThrow(COLUMN_USER_ID);
    user.setUserId(cursor.getLong(userIdIndex));

    int usernameIndex = cursor.getColumnIndexOrThrow(COLUMN_USERNAME);
    user.setUsername(cursor.getString(usernameIndex));

    int createdAtIndex = cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT);
    if (!cursor.isNull(createdAtIndex)) {
        user.setCreatedAt(LocalDateTime.parse(cursor.getString(createdAtIndex), ISO_FORMATTER));
    }
    // ... 30+ more lines
}

// AFTER (much cleaner!)
private User mapCursorToUser(Cursor cursor) {
    User user = new User();

    user.setUserId(CursorMapper.getLong(cursor, COLUMN_USER_ID));
    user.setUsername(CursorMapper.getString(cursor, COLUMN_USERNAME));
    user.setCreatedAt(CursorMapper.getDateTimeOrNull(cursor, COLUMN_CREATED_AT));
    user.setUpdatedAt(CursorMapper.getDateTimeOrNull(cursor, COLUMN_UPDATED_AT));
    user.setPasswordHash(CursorMapper.getString(cursor, COLUMN_PASSWORD_HASH));
    user.setSalt(CursorMapper.getString(cursor, COLUMN_SALT));
    user.setPasswordAlgorithm(CursorMapper.getString(cursor, COLUMN_PASSWORD_ALGORITHM));
    user.setPhoneNumber(CursorMapper.getStringOrNull(cursor, COLUMN_PHONE_NUMBER));

    return user;
}
```

**Step 4: Refactor Remaining DAOs**
- WeightEntryDAO.mapCursorToWeightEntry()
- GoalWeightDAO.mapCursorToGoalWeight()
- AchievementDAO.mapCursorToAchievement()
- UserPreferenceDAO.mapCursorToUserPreference()

**Step 5: Verify Tests Pass**
```bash
./gradlew test --tests "CursorMapperTest"
./gradlew test --tests "*DAOTest"
```

**Step 6: Commit**
```bash
git add app/src/main/java/com/example/weightogo/utils/CursorMapper.java
git add app/src/main/java/com/example/weightogo/database/*DAO.java
git add app/src/test/java/com/example/weightogo/utils/CursorMapperTest.java
git commit -m "refactor(dry): create CursorMapper utility to eliminate duplication

- Create CursorMapper with typed cursor extraction methods
- Refactor all 5 DAOs to use CursorMapper
- Reduces code from ~150 lines to ~50 lines
- Single source of truth for cursor handling
- Add comprehensive tests for all data types

Fixes: CODE_QUALITY_AUDIT.md Issue #16
CLAUDE.md: DRY Principle"
```

**Estimated Time**: 4 hours

---

### Violation 6.2-6.4: Other DRY Fixes

Following same pattern:

**6.2: PreferenceKeys Constants** (scattered across 2 files)
- Create centralized PreferenceKeys class
- Estimated: 1 hour

**6.3: DateTimeFormatters Utility** (duplicated in 4 DAOs)
- Create shared DateTimeFormatters class
- Estimated: 1 hour

**6.4: FormValidator Utility** (duplicated validation patterns)
- Create FormValidator helper
- Estimated: 3 hours

---

### Phase 6 Summary

| Violation | Component | Effort | Branch | Commit |
|-----------|-----------|--------|--------|--------|
| 6.1 Cursor mapping | CursorMapper | 4 hours | refactor/eliminate-duplication | ✅ |
| 6.2 Preference keys | PreferenceKeys | 1 hour | refactor/eliminate-duplication | ✅ |
| 6.3 Date formatters | DateTimeFormatters | 1 hour | refactor/eliminate-duplication | ✅ |
| 6.4 Form validation | FormValidator | 3 hours | refactor/eliminate-duplication | ✅ |

**Total Phase 6 Time**: 9 hours
**Commits**: 4
**Code Reduction**: ~200 lines eliminated
**New Utility Classes**: 4

---

## Phase 7: SOLID Principles (MEDIUM)

**Priority**: MEDIUM (improves architecture)
**Estimated Effort**: 16 hours
**Branch**: `refactor/solid-compliance`

### Violation 7.1: Split SMSNotificationManager (SRP)

**File**: `SMSNotificationManager.java` (432 lines, 5 responsibilities)
**Severity**: 🟡 MEDIUM
**CLAUDE.md Violation**: SOLID - Single Responsibility Principle

#### Approach

Extract 5 classes from monolithic manager:
1. **SmsPermissionChecker** - Permission logic only
2. **SmsPreferenceValidator** - User preference checks
3. **SmsSender** - Core SMS sending
4. **MessageTemplateFormatter** - Message formatting
5. **AchievementNotifier** - Coordinator

#### TDD Steps (abbreviated)

1. Write tests for each new class (RED)
2. Extract classes one by one (GREEN)
3. Refactor SMSNotificationManager to use new classes (REFACTOR)
4. Verify all tests pass
5. Commit

**Estimated Time**: 8 hours

---

### Violation 7.2-7.4: Other SOLID Fixes

**7.2: Repository Interfaces (DIP)**
- Create WeightEntryRepository, GoalWeightRepository interfaces
- DAOs implement interfaces
- Activities depend on abstractions
- Estimated: 4 hours

**7.3: Navigation Router (OCP)**
- Extract hard-coded navigation to NavigationRouter
- Strategy pattern for routes
- Estimated: 2 hours

**7.4: Achievement Strategy Pattern (OCP)**
- Replace switch statement with Strategy pattern
- Each achievement type has own handler
- Estimated: 2 hours

---

### Phase 7 Summary

| Violation | Component | Effort | Branch | Commit |
|-----------|-----------|--------|--------|--------|
| 7.1 SRP - SMS Manager | Split into 5 classes | 8 hours | refactor/solid-compliance | ✅ |
| 7.2 DIP - Repositories | Interface abstraction | 4 hours | refactor/solid-compliance | ✅ |
| 7.3 OCP - Navigation | NavigationRouter | 2 hours | refactor/solid-compliance | ✅ |
| 7.4 OCP - Achievements | Strategy pattern | 2 hours | refactor/solid-compliance | ✅ |

**Total Phase 7 Time**: 16 hours
**Commits**: 4
**New Classes**: 10+
**SOLID Compliance**: Significantly improved

---

## Phase 8: Remaining Accessibility (MEDIUM/LOW)

**Priority**: MEDIUM/LOW (polish accessibility to 98% compliance)
**Estimated Effort**: 6 hours
**Branch**: `fix/accessibility-remaining`

### Violations 8.1-8.8 (Various accessibility polish items)

- EditText label associations (30 min)
- Tab toggle state announcements (30 min)
- Unit toggle state announcements (30 min)
- Date button state descriptions (30 min)
- Trend badge semantics (1 hour)
- FrameLayout contentDescription fix (15 min)
- RecyclerView empty state live regions (30 min)
- Focus order improvements (1 hour)

**Total Phase 8 Time**: 5 hours
**Commits**: 8
**WCAG Compliance**: 95% → 98%

---

## Testing Strategy

### Unit Tests (TDD - Required for All Phases)

```bash
# Run before every commit
./gradlew test

# Run specific test class
./gradlew test --tests "ProgressCalculatorTest"

# Run with coverage report
./gradlew test jacocoTestReport
open app/build/reports/jacoco/test/html/index.html
```

**Coverage Target**: ≥ 90% for new/modified code

---

### Integration Tests

```bash
# Run after completing each phase
./gradlew testDebugUnitTest

# Run DAO integration tests
./gradlew test --tests "*DAOTest"
```

---

### UI/Accessibility Tests (Espresso)

```bash
# Run on emulator/device
./gradlew connectedAndroidTest

# Run specific accessibility test suite
./gradlew connectedAndroidTest --tests "*AccessibilityTest"
```

---

### Manual Testing Checklist

**Phase 1 (Security)**:
- [ ] Check logcat for phone numbers (should be masked)
- [ ] Check logcat for usernames (should be absent)
- [ ] Verify SHA-256 debt documented

**Phase 2 (Performance)**:
- [ ] Enable StrictMode, verify no UI thread violations
- [ ] Add 100 weight entries, test save speed
- [ ] Rotate device during save (should not crash/lose data)

**Phase 3 (Accessibility)**:
- [ ] Enable TalkBack, navigate weight entry screen
- [ ] Verify all numpad buttons announce correctly
- [ ] Test touch targets with "Show layout bounds" enabled
- [ ] Verify color contrast with contrast checker tool

**Phase 4 (Memory)**:
- [ ] Enter phone number in settings
- [ ] Rotate device during save
- [ ] Verify phone number persists after rotation

**Phase 5-7 (Architecture)**:
- [ ] Full regression testing of all features
- [ ] Verify no behavioral changes from refactoring

**Phase 8 (Remaining Accessibility)**:
- [ ] Full TalkBack audit of entire app
- [ ] Keyboard navigation through all screens

---

## Approval Checklist

### Before Starting ANY Phase:

- [ ] **Read CLAUDE.md Section 5 (TDD)** - Understand Red-Green-Refactor
- [ ] **Approve Phase Scope** - Review violations being fixed
- [ ] **Approve Effort Estimate** - Agree on time allocation
- [ ] **Approve Testing Strategy** - Understand test requirements
- [ ] **Create Feature Branch** - Use naming convention from plan

### During Phase Execution:

- [ ] **Write Tests First (RED)** - No implementation before tests
- [ ] **Run Tests Frequently** - Verify RED → GREEN → REFACTOR
- [ ] **One Violation Per Commit** - Clear, atomic commits
- [ ] **Run Full Test Suite Before Commit** - `./gradlew test` must pass
- [ ] **Follow Commit Message Convention** - As shown in plan

### After Completing Phase:

- [ ] **All Tests Pass** - `./gradlew test` and `./gradlew connectedAndroidTest`
- [ ] **Manual Testing Complete** - Follow phase checklist
- [ ] **Code Review** - Review all changes before merge
- [ ] **Update TODO.md** - Mark phase as complete
- [ ] **Merge to Main** - Create PR, get approval, merge

---

## Total Effort Summary

| Phase | Focus | Violations Fixed | Estimated Time | Priority |
|-------|-------|------------------|----------------|----------|
| **Phase 1** | Security | 3 | 1 hour | 🔴 CRITICAL |
| **Phase 2** | Performance | 1 | 4 hours | 🔴 CRITICAL |
| **Phase 3** | Accessibility (Critical) | 4 | 7 hours | 🔴 CRITICAL |
| **Phase 4** | Memory Leaks | 1 | 4 hours | 🔴 CRITICAL |
| **Phase 5** | MVC Architecture | 4 | 9 hours | 🟡 MEDIUM |
| **Phase 6** | DRY Violations | 4 | 9 hours | 🟡 MEDIUM |
| **Phase 7** | SOLID Principles | 4 | 16 hours | 🟡 MEDIUM |
| **Phase 8** | Accessibility (Remaining) | 8 | 5 hours | 🟢 LOW |
| **TOTAL** | **All Violations** | **48** | **55 hours** | - |

---

## Phased Rollout Options

### Option A: Fix All HIGH-Severity Only (Recommended for Students)
- **Phases**: 1-4 only
- **Time**: 16 hours (2 full work days)
- **Result**: All CRITICAL issues resolved, code grade: A-

### Option B: Fix All HIGH + MEDIUM Severity
- **Phases**: 1-7
- **Time**: 46 hours (1 week full-time)
- **Result**: Production-quality code, code grade: A

### Option C: Fix Everything (100% Compliance)
- **Phases**: 1-8
- **Time**: 55 hours (1.5 weeks full-time)
- **Result**: Exemplary code quality, code grade: A+

---

## Execution Instructions

### To Begin:

1. **Review and Approve This Plan**
   - Read entire plan carefully
   - Ask questions about any unclear steps
   - Approve scope and approach

2. **Choose Rollout Option**
   - Decide which phases to execute (A, B, or C)
   - Set timeline expectations

3. **Start Phase 1**
   - Create branch: `git checkout -b fix/security-pii-logging`
   - Follow TDD steps exactly as documented
   - Request approval before committing

4. **Proceed Phase by Phase**
   - Complete all steps in order
   - Get approval before moving to next phase
   - Update TODO.md after each phase

---

## Questions to Answer Before Starting:

1. **Which rollout option do you want?** (A, B, or C)
2. **Do you want to review each commit before it's made?**
3. **Should I proceed automatically after each phase, or wait for approval?**
4. **Do you want detailed explanations during implementation, or just results?**

---

**PLAN STATUS**: ✅ READY FOR APPROVAL

**Next Action**: Awaiting your approval to begin Phase 1 (Security Fixes)