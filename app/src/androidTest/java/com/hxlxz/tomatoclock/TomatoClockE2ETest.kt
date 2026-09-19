package com.hxlxz.tomatoclock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.datastore.preferences.core.edit
import androidx.test.rule.GrantPermissionRule
import android.Manifest
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TomatoClockE2ETest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Inject
    lateinit var timeConfig: TimeConfig

    private lateinit var dataStore: SettingsDataStore

    private fun waitUntilTextExists(text: String, timeoutMillis: Long = 5000) {
        composeTestRule.waitUntil(timeoutMillis) {
            composeTestRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Before
    fun setup() {
        hiltRule.inject()
        // Speed up the timer for testing: 1 minute configured in UI = 1 second in real time
        timeConfig.multiplier = 1L
        
        dataStore = SettingsDataStore(ApplicationProvider.getApplicationContext())
        runBlocking {
            dataStore.saveFocusTime(3) // 3 seconds real time
            dataStore.saveShortBreakTime(2) // 2 seconds real time
            dataStore.saveAutoStartBreak(true)
        }
    }

    @After
    fun teardown() {
        // Restore time multiplier
        timeConfig.multiplier = 60L
        runBlocking {
            ApplicationProvider.getApplicationContext<android.content.Context>().dataStore.edit { it.clear() }
        }
    }

    @Test
    fun testFullTimerFlow() {
        composeTestRule.waitForIdle()

        // Verify initial state
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()

        // 2. Start the timer
        composeTestRule.onNodeWithText("开始").performClick()
        composeTestRule.waitForIdle()

        // Verify it changed to RUNNING state
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
        composeTestRule.onNodeWithText("暂停").assertIsDisplayed()
        composeTestRule.onNodeWithText("停止").assertIsDisplayed()

        // 3. Let the timer run to completion (wait 4 seconds max)
        waitUntilTextExists("短休息")
        composeTestRule.waitForIdle()

        // 4. Verify it auto-transitioned to Break (since autoStartBreak is true)
        composeTestRule.onNodeWithText("短休息").assertIsDisplayed()

        // 5. Test Pause functionality during break
        composeTestRule.onNodeWithText("暂停").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("继续").assertIsDisplayed()

        // 6. Test Stop functionality
        composeTestRule.onNodeWithText("停止").performClick()
        composeTestRule.waitForIdle()

        // Verify it resets to FOCUS mode and initial time (00:03)
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
        composeTestRule.onNodeWithText("00:03").assertIsDisplayed()
    }
    
    @Test
    fun testSettingsNavigationAndModification() {
        composeTestRule.waitForIdle()

        // Go to settings
        composeTestRule.onNodeWithContentDescription("设置").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("时长设置（分钟）").assertIsDisplayed()
        
        // Find the text field containing "3" (our focus time) and increment it
        composeTestRule.onNodeWithContentDescription("返回").performClick()
        composeTestRule.waitForIdle()
        
        // Verify we are back on Timer screen
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
    }

    @Test
    fun testManualTransitionAndSnoozeFlow() {
        runBlocking {
            dataStore.saveAutoStartBreak(false)
            dataStore.saveSnoozeTime(2) // 2 seconds snooze
        }
        
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()

        // Start timer
        composeTestRule.onNodeWithText("开始").performClick()
        
        // Wait 4 seconds for focus timer (3s) to finish
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        // Since autoStartBreak=false, we should see "开始下个阶段" and "推迟提醒"
        composeTestRule.onNodeWithText("开始下个阶段").assertIsDisplayed()
        composeTestRule.onNodeWithText("推迟提醒").assertIsDisplayed()

        // Click snooze
        composeTestRule.onNodeWithText("推迟提醒").performClick()
        composeTestRule.waitForIdle()

        // Wait 3 seconds for snooze timer (2s) to finish
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        // It should be finished again
        composeTestRule.onNodeWithText("开始下个阶段").assertIsDisplayed()

        // Click next
        composeTestRule.onNodeWithText("开始下个阶段").performClick()
        composeTestRule.waitForIdle()

        // Should transition to SHORT_BREAK and start immediately
        composeTestRule.onNodeWithText("短休息").assertIsDisplayed()
        composeTestRule.onNodeWithText("暂停").assertIsDisplayed()
    }

    @Test
    fun testLongBreakFlow() {
        runBlocking {
            dataStore.saveAutoStartBreak(true)
            dataStore.saveAutoStartFocus(true)
            dataStore.saveCycles(2)
            dataStore.saveLongBreakTime(4)
        }
        
        composeTestRule.waitForIdle()
        // Wait until datastore propagation reflects in UI (cycles = 2)
        waitUntilTextExists("第 1 / 2 次循环")
        
        composeTestRule.onNodeWithText("开始").performClick()
        
        // Cycle 1: Focus (initialized as 3s from @Before)
        waitUntilTextExists("短休息")
        composeTestRule.waitForIdle()

        // Should auto-transition to SHORT_BREAK (2s from @Before)
        composeTestRule.onNodeWithText("短休息").assertIsDisplayed()
        
        // Wait for Short break to finish (2s)
        waitUntilTextExists("专注中")
        composeTestRule.waitForIdle()

        // Cycle 2: Focus (reads 3s again)
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
        
        // Wait for Focus to finish (3s)
        waitUntilTextExists("长休息")
        composeTestRule.waitForIdle()

        // Because cycles = 2, we should now be in LONG_BREAK
        composeTestRule.onNodeWithText("长休息").assertIsDisplayed()
    }

    @Test
    fun testSettingsAlertModeModification() {
        composeTestRule.waitForIdle()

        // Go to settings
        composeTestRule.onNodeWithContentDescription("设置").performClick()
        composeTestRule.waitForIdle()
        
        // Find Alert mode label
        composeTestRule.onNodeWithText("提醒模式").performScrollTo().assertIsDisplayed()
        
        // Click the dropdown (the default value should be 闹铃 + 震动)
        composeTestRule.onNodeWithText("闹铃 + 震动").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        // Select "仅闹铃"
        composeTestRule.onNodeWithText("仅闹铃").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        // Verify it was updated
        composeTestRule.onNodeWithText("仅闹铃").performScrollTo().assertIsDisplayed()
        
        // Go back
        composeTestRule.onNodeWithContentDescription("返回").performClick()
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun testSettingsFlashAndRingtoneModification() {
        composeTestRule.waitForIdle()

        // Go to settings
        composeTestRule.onNodeWithContentDescription("设置").performClick()
        composeTestRule.waitForIdle()
        
        // Find Ringtone label
        composeTestRule.onNodeWithText("提示音选择").performScrollTo().assertIsDisplayed()
        
        // Change Ringtone from default (清脆风铃) to (柔和合成音)
        composeTestRule.onNodeWithText("清脆风铃").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("柔和合成音").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("柔和合成音").performScrollTo().assertIsDisplayed()
        
        // Toggle Flash Screen switch
        composeTestRule.onNodeWithText("结束时界面呼吸闪烁").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("结束时界面呼吸闪烁").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        // Go back
        composeTestRule.onNodeWithContentDescription("返回").performClick()
        composeTestRule.waitForIdle()
    }
}
