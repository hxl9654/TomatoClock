package com.hxlxz.tomatoclock

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.hxlxz.tomatoclock.ui.TimerScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import androidx.activity.ComponentActivity

@RunWith(AndroidJUnit4::class)
class TimerScreenInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `when IDLE start button is visible and clickable`() {
        val viewModel = mockk<TimerViewModel>(relaxed = true)
        every { viewModel.timerMode } returns MutableStateFlow(TimerMode.FOCUS)
        every { viewModel.timerState } returns MutableStateFlow(TimerState.IDLE)
        every { viewModel.timeRemaining } returns MutableStateFlow(25 * 60L)
        every { viewModel.totalTimeInSeconds } returns MutableStateFlow(25 * 60L)
        every { viewModel.currentCycle } returns MutableStateFlow(1)
        every { viewModel.totalCycles } returns MutableStateFlow(4)
        every { viewModel.flashScreen } returns MutableStateFlow(false)

        composeTestRule.setContent {
            TimerScreen(onNavigateToSettings = {}, viewModel = viewModel)
        }

        // Action Start should exist (string resource action_start might be "Start" or "开始" in Chinese)
        // Since we are running in tests without setting locale, we might need to rely on what it resolves to.
        // Assuming default English strings for test environment, or we can check both.
        // But the requirements said Chinese UI only. Let's just find by text or use semantics.
        // To be safe, we'll check if the button with text for Start is clicked.
        // Let's assume Chinese strings.xml has "开始"
        // Since we don't have strings.xml hardcoded in the test, we'll just check if startTimer is called when we click the only button if there is only one.
        // Actually, we can use `onNodeWithText` for "开始".
        
        composeTestRule.onNodeWithText("开始").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.startTimer() }
    }

    @Test
    fun `when RUNNING pause and stop buttons are visible`() {
        val viewModel = mockk<TimerViewModel>(relaxed = true)
        every { viewModel.timerMode } returns MutableStateFlow(TimerMode.FOCUS)
        every { viewModel.timerState } returns MutableStateFlow(TimerState.RUNNING)
        every { viewModel.timeRemaining } returns MutableStateFlow(25 * 60L)
        every { viewModel.totalTimeInSeconds } returns MutableStateFlow(25 * 60L)
        every { viewModel.currentCycle } returns MutableStateFlow(1)
        every { viewModel.totalCycles } returns MutableStateFlow(4)
        every { viewModel.flashScreen } returns MutableStateFlow(false)

        composeTestRule.setContent {
            TimerScreen(onNavigateToSettings = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("暂停").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.pauseTimer() }

        composeTestRule.onNodeWithText("停止").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.stopTimer() }
    }

    @Test
    fun `when FINISHED next and snooze buttons are visible`() {
        val viewModel = mockk<TimerViewModel>(relaxed = true)
        every { viewModel.timerMode } returns MutableStateFlow(TimerMode.FOCUS)
        every { viewModel.timerState } returns MutableStateFlow(TimerState.FINISHED)
        every { viewModel.timeRemaining } returns MutableStateFlow(0L)
        every { viewModel.totalTimeInSeconds } returns MutableStateFlow(25 * 60L)
        every { viewModel.currentCycle } returns MutableStateFlow(1)
        every { viewModel.totalCycles } returns MutableStateFlow(4)
        every { viewModel.flashScreen } returns MutableStateFlow(false)

        composeTestRule.setContent {
            TimerScreen(onNavigateToSettings = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("开始下个阶段").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.nextPhase() }

        composeTestRule.onNodeWithText("推迟提醒").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.snooze() }
    }

    @Test
    fun timerControls_addTimeAndSkip_visibleOnlyWhenRunning() {
        val viewModel = mockk<TimerViewModel>(relaxed = true)
        every { viewModel.timerMode } returns MutableStateFlow(TimerMode.FOCUS)
        val stateFlow = MutableStateFlow(TimerState.IDLE)
        every { viewModel.timerState } returns stateFlow
        every { viewModel.timeRemaining } returns MutableStateFlow(25 * 60L)
        every { viewModel.totalTimeInSeconds } returns MutableStateFlow(25 * 60L)
        every { viewModel.currentCycle } returns MutableStateFlow(1)
        every { viewModel.totalCycles } returns MutableStateFlow(4)
        every { viewModel.flashScreen } returns MutableStateFlow(false)

        composeTestRule.setContent {
            TimerScreen(onNavigateToSettings = {}, viewModel = viewModel)
        }

        // When IDLE, buttons should not exist
        composeTestRule.onNodeWithContentDescription("加5分钟").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("跳过当前周期").assertDoesNotExist()

        // Switch to PAUSED
        stateFlow.value = TimerState.PAUSED
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("加5分钟").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("跳过当前周期").assertDoesNotExist()

        // Switch to RUNNING
        stateFlow.value = TimerState.RUNNING
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("加5分钟").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("跳过当前周期").assertIsDisplayed()
    }

    @Test
    fun `click add time and skip phase delegate to viewModel`() {
        val viewModel = mockk<TimerViewModel>(relaxed = true)
        every { viewModel.timerMode } returns MutableStateFlow(TimerMode.FOCUS)
        every { viewModel.timerState } returns MutableStateFlow(TimerState.RUNNING)
        every { viewModel.timeRemaining } returns MutableStateFlow(25 * 60L)
        every { viewModel.totalTimeInSeconds } returns MutableStateFlow(25 * 60L)
        every { viewModel.currentCycle } returns MutableStateFlow(1)
        every { viewModel.totalCycles } returns MutableStateFlow(4)
        every { viewModel.flashScreen } returns MutableStateFlow(false)

        composeTestRule.setContent {
            TimerScreen(onNavigateToSettings = {}, viewModel = viewModel)
        }

        composeTestRule.onNodeWithContentDescription("加5分钟").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.addFiveMinutes() }

        composeTestRule.onNodeWithContentDescription("跳过当前周期").performClick()
        composeTestRule.waitForIdle()
        verify { viewModel.skipCurrentPhase() }
    }
}
