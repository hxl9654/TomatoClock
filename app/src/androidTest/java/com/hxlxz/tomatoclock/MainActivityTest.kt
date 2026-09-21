package com.hxlxz.tomatoclock

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testNavigationToSettingsAndBack() {
        // App starts on TimerScreen.
        // Find the settings button (the action bar icon has contentDescription = "设置")
        // We use the application's string resource for Settings
        val settingsTitle = composeTestRule.activity.getString(R.string.settings_title)
        val navigateBackStr = composeTestRule.activity.getString(R.string.navigate_back)

        // Verify we are on Timer Screen (has app_name title)
        val appName = composeTestRule.activity.getString(R.string.app_name)
        composeTestRule.onNodeWithText(appName).assertExists()

        // Click on Settings icon
        composeTestRule.onNodeWithContentDescription(settingsTitle).performClick()

        // Verify we navigated to SettingsScreen (has "番茄钟设置" or "持续时间" text)
        val settingsDurations = composeTestRule.activity.getString(R.string.settings_durations)
        composeTestRule.onNodeWithText(settingsDurations).assertExists()

        // Click Back
        composeTestRule.onNodeWithContentDescription(navigateBackStr).performClick()

        // Verify we are back to Timer Screen
        composeTestRule.onNodeWithText(appName).assertExists()
    }
}
