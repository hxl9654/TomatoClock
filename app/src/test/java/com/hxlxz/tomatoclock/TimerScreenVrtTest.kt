package com.hxlxz.tomatoclock

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.hxlxz.tomatoclock.ui.TimerScreenContent

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel5)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimerScreenVrtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTimerScreenIdleSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.IDLE,
                    timeRemaining = 1500L,
                    totalTime = 1500L,
                    currentCycle = 1,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenRunningSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.RUNNING,
                    timeRemaining = 1200L,
                    totalTime = 1500L,
                    currentCycle = 2,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onNodeWithContentDescription("加5分钟").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("跳过当前周期").assertIsDisplayed()
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenFinishedWithFlashSnapshot() {
        // 验证 FINISHED + flashScreen=true 时的呼吸动画起始帧外观
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.FINISHED,
                    timeRemaining = 0L,
                    totalTime = 1500L,
                    currentCycle = 1,
                    totalCycles = 4,
                    flashScreen = true,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenShortBreakSnapshot() {
        // 验证 SHORT_BREAK 模式下的 Secondary 颜色方案
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.SHORT_BREAK,
                    timerState = TimerState.RUNNING,
                    timeRemaining = 270L,
                    totalTime = 300L,
                    currentCycle = 1,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenLongBreakSnapshot() {
        // 验证 LONG_BREAK 模式下的 Tertiary 颜色方案
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.LONG_BREAK,
                    timerState = TimerState.PAUSED,
                    timeRemaining = 600L,
                    totalTime = 900L,
                    currentCycle = 4,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenFocusPausedSnapshot() {
        // 【L-7修复】补全 FOCUS + PAUSED 状态快照，
        // 验证新的"专注已暂停"提示语渲染正确
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.PAUSED,
                    timeRemaining = 900L,
                    totalTime = 1500L,
                    currentCycle = 2,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenIdleSnapshot_withNewLabel() {
        // 【UI修复】验证 IDLE 状态下显示"准备专注"而非"专注中"
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.IDLE,
                    timeRemaining = 1500L,
                    totalTime = 1500L,
                    currentCycle = 1,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("设置").assertIsDisplayed()
        composeTestRule.onRoot().captureRoboImage()
    }

    // ── 本次新增：TC-7 缺失的快照覆盖率 ─────────────────────────────────

    @Test
    fun testTimerScreenLongBreakFinishedSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.LONG_BREAK,
                    timerState = TimerState.FINISHED,
                    timeRemaining = 0L,
                    totalTime = 900L,
                    currentCycle = 4,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenShortBreakPausedSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.SHORT_BREAK,
                    timerState = TimerState.PAUSED,
                    timeRemaining = 200L,
                    totalTime = 300L,
                    currentCycle = 1,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenFinishedWithoutFlashSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.FINISHED,
                    timeRemaining = 0L,
                    totalTime = 1500L,
                    currentCycle = 1,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun testTimerScreenDarkModeSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.FOCUS,
                    timerState = TimerState.RUNNING,
                    timeRemaining = 1200L,
                    totalTime = 1500L,
                    currentCycle = 2,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun testTimerScreenLongBreakRunningSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                TimerScreenContent(
                    timerMode = TimerMode.LONG_BREAK,
                    timerState = TimerState.RUNNING,
                    timeRemaining = 600L,
                    totalTime = 1200L,
                    currentCycle = 4,
                    totalCycles = 4,
                    flashScreen = false,
                    onNavigateToSettings = {},
                    onStart = {},
                    onPause = {},
                    onStop = {},
                    onNextPhase = {},
                    onSnooze = {},
                    onAddFiveMinutes = {},
                    onSkipPhase = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }
}
