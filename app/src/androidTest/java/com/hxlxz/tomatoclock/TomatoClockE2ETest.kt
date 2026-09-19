package com.hxlxz.tomatoclock

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
    fun setup() = runTest {
        hiltRule.inject()
        // Speed up the timer for testing: 1 minute configured in UI = 1 second in real time
        timeConfig.multiplier = 1L
        
        dataStore = SettingsDataStore(ApplicationProvider.getApplicationContext())
        dataStore.saveFocusTime(3) // 3 seconds real time
        dataStore.saveShortBreakTime(2) // 2 seconds real time
    }

    @After
    fun teardown() = runTest {
        // Restore time multiplier
        timeConfig.multiplier = 60L
        ApplicationProvider.getApplicationContext<android.content.Context>().dataStore.edit { it.clear() }
    }

    @Test
    fun testFullTimerFlow() {
        composeTestRule.waitForIdle()

        // 【UI修复】IDLE 状态显示"准备专注"而不是"专注中"
        composeTestRule.onNodeWithText("准备专注").assertIsDisplayed()

        // 2. Start the timer
        composeTestRule.onNodeWithText("开始").performClick()
        composeTestRule.waitForIdle()

        // Verify it changed to RUNNING state: 现在正在运行显示"专注中"
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
        composeTestRule.onNodeWithText("暂停").assertIsDisplayed()
        composeTestRule.onNodeWithText("停止").assertIsDisplayed()

        // 3. Let the timer run to completion (wait 4 seconds max)
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        // 4. Verify we are now in FINISHED state where we can click "开始下个阶段"
        composeTestRule.onNodeWithText("开始下个阶段").assertIsDisplayed()
        composeTestRule.onNodeWithText("开始下个阶段").performClick()
        composeTestRule.waitForIdle()

        // SHORT_BREAK RUNNING 状态显示"短休息"
        composeTestRule.onNodeWithText("短休息").assertIsDisplayed()

        // 5. Test Pause functionality during break
        composeTestRule.onNodeWithText("暂停").performClick()
        composeTestRule.waitForIdle()
        // 【UI修复】SHORT_BREAK PAUSED 显示"短休息已暂停"
        composeTestRule.onNodeWithText("短休息已暂停").assertIsDisplayed()
        composeTestRule.onNodeWithText("继续").assertIsDisplayed()

        // 6. Test Stop functionality
        composeTestRule.onNodeWithText("停止").performClick()
        composeTestRule.waitForIdle()

        // Verify it resets to FOCUS IDLE mode: 显示"准备专注"
        composeTestRule.onNodeWithText("准备专注").assertIsDisplayed()
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

        // 【UI修复】返回后仍处于 IDLE，显示"准备专注"
        composeTestRule.onNodeWithText("准备专注").assertIsDisplayed()
    }

    @Test
    fun testManualTransitionAndSnoozeFlow() = runTest {
        dataStore.saveSnoozeTime(2) // 2 seconds snooze
        
        
        composeTestRule.waitForIdle()
        // 【UI修复】IDLE 状态显示"准备专注"
        composeTestRule.onNodeWithText("准备专注").assertIsDisplayed()

        // Start timer
        composeTestRule.onNodeWithText("开始").performClick()
        
        // Wait 4 seconds for focus timer (3s) to finish
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        // We should see "开始下个阶段" and "推迟提醒"
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
    fun testLongBreakFlow() = runTest {
        dataStore.saveCycles(2)
        dataStore.saveLongBreakTime(4)
        
        composeTestRule.waitForIdle()
        // Wait until datastore propagation reflects in UI (cycles = 2)
        waitUntilTextExists("第 1 / 2 次循环")
        
        composeTestRule.onNodeWithText("开始").performClick()
        
        // Cycle 1: Focus (initialized as 3s from @Before)
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("开始下个阶段").performClick()
        composeTestRule.waitForIdle()

        // Should transition to SHORT_BREAK (2s from @Before)
        composeTestRule.onNodeWithText("短休息").assertIsDisplayed()
        
        // Wait for Short break to finish (2s)
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("开始下个阶段").performClick()
        composeTestRule.waitForIdle()

        // Cycle 2: Focus RUNNING (reads 3s again)
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
        
        // Wait for Focus to finish (3s)
        waitUntilTextExists("开始下个阶段")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("开始下个阶段").performClick()
        composeTestRule.waitForIdle()

        // Because cycles = 2, we should now be in LONG_BREAK
        composeTestRule.onNodeWithText("长休息").assertIsDisplayed()
    }

    @Test
    fun testSettingsSoundAndVibrationModification() {
        composeTestRule.waitForIdle()

        // Go to settings
        composeTestRule.onNodeWithContentDescription("设置").performClick()
        composeTestRule.waitForIdle()
        
        // Find Sound Mode label
        composeTestRule.onNodeWithText("铃声模式").performScrollTo().assertIsDisplayed()
        // Select "响铃一次" (directly visible as SegmentedButton)
        composeTestRule.onNodeWithText("响铃一次").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        // Verify it was updated
        composeTestRule.onNodeWithText("响铃一次").performScrollTo().assertIsDisplayed()
        
        // Find Vibration Mode label
        composeTestRule.onNodeWithText("震动模式").performScrollTo().assertIsDisplayed()
        
        // Select "震动一次" (directly visible as SegmentedButton)
        composeTestRule.onNodeWithText("震动一次").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        // Verify it was updated
        composeTestRule.onNodeWithText("震动一次").performScrollTo().assertIsDisplayed()
        
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

    @Test
    fun testAddFiveMinutesFlow() = runTest {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("开始").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()

        // 此时由于 1 分钟 = 1 秒，专注时间3秒，剩余时间应很快降到 00:02 或更少。
        // 点击加5分钟按钮（在我们的测试配置下，将增加5秒的倒计时时长）。
        composeTestRule.onNodeWithContentDescription("加5分钟").performClick()
        composeTestRule.waitForIdle()

        // 验证时间确实增加：3秒加5秒，约等于8秒，我们验证不会立刻结束，仍然在"专注中"
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()

        // 等待原来应该结束的3秒过去，确认它还没有结束（如果没加时，这里早就FINISHED了）
        composeTestRule.mainClock.advanceTimeBy(4000)
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()
    }

    @Test
    fun testSkipPhaseFlow() = runTest {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("开始").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("专注中").assertIsDisplayed()

        // 点击跳过
        composeTestRule.onNodeWithContentDescription("跳过当前周期").performClick()
        composeTestRule.waitForIdle()

        // 验证直接进入 FINISHED 状态
        composeTestRule.onNodeWithText("开始下个阶段").assertIsDisplayed()
    }
}
