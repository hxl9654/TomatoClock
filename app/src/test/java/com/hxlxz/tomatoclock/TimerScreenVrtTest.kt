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
}
