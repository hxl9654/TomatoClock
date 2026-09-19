package com.hxlxz.tomatoclock

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
                    onSnooze = {}
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
                    onSnooze = {}
                )
            }
        }
        
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
                    onSnooze = {}
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
                    onSnooze = {}
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
                    onSnooze = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }
}
