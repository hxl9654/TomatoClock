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
                    // [SMELL-06修复] 使用枚举 .value 属性替代魔法数字，
                    // 编译器将在枚举定义变更时报错，杜绝静默失效风险。
                    soundMode = SoundMode.CONTINUOUS.value,
                    vibrationMode = VibrationMode.CONTINUOUS.value,
                    ringtone = Ringtone.CHIME.value,
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
                    soundMode = SoundMode.CONTINUOUS.value,
                    vibrationMode = VibrationMode.CONTINUOUS.value,
                    ringtone = Ringtone.CHIME.value,
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
