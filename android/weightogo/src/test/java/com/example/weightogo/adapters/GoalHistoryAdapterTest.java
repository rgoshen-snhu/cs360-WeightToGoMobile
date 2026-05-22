package com.example.weightogo.adapters;

import android.content.Context;

import com.example.weightogo.models.GoalWeight;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for GoalHistoryAdapter.
 * Tests RecyclerView adapter functionality for displaying past goals.
 *
 * Tests FR5.0 - Goal Weight Management (Goal History Display)
 * Coverage: 4 tests
 *
 * NOTE: Layout inflation tests are skipped due to Robolectric/Material3 complexity.
 * Adapter functionality will be validated through GoalsActivityEspressoTest integration tests.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GoalHistoryAdapterTest {

    private GoalHistoryAdapter adapter;
    private List<GoalWeight> testGoals;
    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        testGoals = new ArrayList<>();
    }

    /**
     * Test 1: Constructor with empty list creates adapter.
     * Verifies that adapter can be created with an empty goal list.
     */
    @Test
    public void test_constructor_withEmptyList_createsAdapter() {
        // ARRANGE
        List<GoalWeight> emptyList = new ArrayList<>();

        // ACT
        adapter = new GoalHistoryAdapter(emptyList);

        // ASSERT
        assertNotNull("Adapter should not be null", adapter);
        assertEquals("Item count should be 0", 0, adapter.getItemCount());
    }

    /**
     * Test 2: getItemCount returns correct size.
     * Verifies that adapter reports correct number of items.
     */
    @Test
    public void test_getItemCount_withThreeGoals_returnsThree() {
        // ARRANGE - Create 3 test goals
        createTestGoals(3);
        adapter = new GoalHistoryAdapter(testGoals);

        // ACT
        int count = adapter.getItemCount();

        // ASSERT
        assertEquals("Should return 3 for 3 goals", 3, count);
    }

    /**
     * Test 3: updateGoals updates data and notifies adapter.
     * Verifies that updateGoals() method correctly updates the adapter's data.
     */
    @Test
    public void test_updateGoals_withNewGoals_updatesData() {
        // ARRANGE - Start with empty adapter
        adapter = new GoalHistoryAdapter(testGoals);
        assertEquals("Initial count should be 0", 0, adapter.getItemCount());

        // Create new goals
        List<GoalWeight> newGoals = new ArrayList<>();
        createTestGoals(2);
        newGoals.addAll(testGoals);

        // ACT - Update goals
        adapter.updateGoals(newGoals);

        // ASSERT - Verify count updated
        assertEquals("Count should be 2 after update", 2, adapter.getItemCount());
    }

    /**
     * Test 4: updateGoals with null list clears data.
     * Verifies that updateGoals() handles null input gracefully.
     */
    @Test
    public void test_updateGoals_withNull_clearsData() {
        // ARRANGE - Start with goals
        createTestGoals(2);
        adapter = new GoalHistoryAdapter(testGoals);
        assertEquals("Initial count should be 2", 2, adapter.getItemCount());

        // ACT - Update with null
        adapter.updateGoals(null);

        // ASSERT - Verify data cleared
        assertEquals("Count should be 0 after null update", 0, adapter.getItemCount());
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Helper method to create test goals.
     */
    private void createTestGoals(int count) {
        testGoals.clear();
        for (int i = 0; i < count; i++) {
            GoalWeight goal = new GoalWeight();
            goal.setGoalId(i + 1);
            goal.setUserId(1);
            goal.setGoalWeight(150.0 - (i * 5));  // 150, 145, 140, etc.
            goal.setStartWeight(180.0);
            goal.setGoalUnit("lbs");
            goal.setTargetDate(null);
            goal.setActive(false);  // History goals are inactive
            goal.setAchieved(true);
            goal.setCreatedAt(LocalDateTime.now().minusDays(60 + i));
            goal.setUpdatedAt(LocalDateTime.now().minusDays(15 + i));
            testGoals.add(goal);
        }
    }
}
