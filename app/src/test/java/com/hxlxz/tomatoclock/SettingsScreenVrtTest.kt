package com.hxlxz.tomatoclock

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.hxlxz.tomatoclock.ui.SettingsScreenContent

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN-" + RobolectricDeviceQualifiers.Pixel5)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsScreenVrtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testSettingsScreenSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                SettingsScreenContent(
                    focusTime = 25,
                    shortBreakTime = 5,
                    longBreakTime = 15,
                    cycles = 4,
                    snoozeTime = 5,
                    wakeScreen = true,
                    flashScreen = true,
                    soundMode = 0,
                    vibrationMode = 0,
                    ringtone = 1,
                    onNavigateBack = {},
                    onFocusTimeChange = {},
                    onShortBreakTimeChange = {},
                    onLongBreakTimeChange = {},
                    onCyclesChange = {},
                    onSnoozeTimeChange = {},
                    onWakeScreenChange = {},
                    onFlashScreenChange = {},
                    onSoundModeChange = {},
                    onVibrationModeChange = {},
                    onRingtoneChange = {},
                    onRingtonePreview = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "+night")
    fun testSettingsScreenDarkModeSnapshot() {
        composeTestRule.setContent {
            com.hxlxz.tomatoclock.ui.theme.TomatoClockTheme {
                SettingsScreenContent(
                    focusTime = 25,
                    shortBreakTime = 5,
                    longBreakTime = 15,
                    cycles = 4,
                    snoozeTime = 5,
                    wakeScreen = true,
                    flashScreen = true,
                    soundMode = 0,
                    vibrationMode = 0,
                    ringtone = 1,
                    onNavigateBack = {},
                    onFocusTimeChange = {},
                    onShortBreakTimeChange = {},
                    onLongBreakTimeChange = {},
                    onCyclesChange = {},
                    onSnoozeTimeChange = {},
                    onWakeScreenChange = {},
                    onFlashScreenChange = {},
                    onSoundModeChange = {},
                    onVibrationModeChange = {},
                    onRingtoneChange = {},
                    onRingtonePreview = {}
                )
            }
        }
        
        composeTestRule.onRoot().captureRoboImage()
    }
}
