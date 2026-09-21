package com.hxlxz.tomatoclock

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ProcessDeathE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    // Use empty compose rule so Activity is not launched automatically
    @get:Rule(order = 1)
    val composeTestRule = createEmptyComposeRule()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Inject
    lateinit var timeConfig: TimeConfig

    private lateinit var dataStore: SettingsDataStore

    @Inject
    lateinit var timerRepository: TimerRepository

    @Before
    fun setup() {
        dataStore = SettingsDataStore(ApplicationProvider.getApplicationContext())
    }

    @After
    fun teardown() {
        if (this::timeConfig.isInitialized) {
            timeConfig.multiplier = 60L
        }
        if (this::timerRepository.isInitialized) {
            kotlinx.coroutines.runBlocking {
                timerRepository.destroyForTesting()
            }
        }
        kotlinx.coroutines.runBlocking {
            dataStore.clearTimerState()
        }
    }

    @Test
    fun testProcessDeathAndRestoreFlow() = kotlinx.coroutines.test.runTest {
        // Simulate Process Death & Restore by writing to DataStore directly, BEFORE launching the UI and BEFORE injecting Hilt singletons.
        // We set the state to RUNNING with a target end time 2 minutes in the future.
        val futureTime = System.currentTimeMillis() + 120_000L
        val savedState = SavedTimerState(
            state = TimerState.RUNNING,
            mode = TimerMode.FOCUS,
            targetEndTimeWallClock = futureTime,
            pausedTimeRemaining = 0L,
            currentCycle = 1,
            lastSavedTimestamp = System.currentTimeMillis(),
            totalTimeInSeconds = 180L
        )
        dataStore.saveTimerState(savedState)

        // NOW inject Hilt so that TimerRepository is instantiated and reads the preset DataStore state.
        hiltRule.inject()
        timeConfig.multiplier = 1L

        // Now launch the activity.
        ActivityScenario.launch(MainActivity::class.java).use {
            composeTestRule.waitForIdle()

            // It should restore the state and display "专注中" immediately without clicking start.
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("专注中").fetchSemanticsNodes().isNotEmpty()
            }
            
            composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
            composeTestRule.onNodeWithText("暂停").assertIsDisplayed()
        }
    }
}
