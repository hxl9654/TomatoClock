package com.hxlxz.tomatoclock

import android.Manifest
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MainActivityTest — 验证主 Activity 的核心导航流程。
 *
 * 注意事项：
 *  - order=0: HiltAndroidRule 必须最先运行，保证 Hilt 组件图在 Activity 启动前就绪。
 *  - order=1: ComposeTestRule 启动 Activity 并挂载 Compose 树。
 *  - order=2: GrantPermissionRule 预授 POST_NOTIFICATIONS，防止权限弹窗阻塞 UI 树挂载。
 *  - @Before setup(): 显式调用 hiltRule.inject()，确保字段注入在任何测试方法前完成。
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testNavigationToSettingsAndBack() {
        val settingsTitle = composeTestRule.activity.getString(R.string.settings_title)
        val navigateBackStr = composeTestRule.activity.getString(R.string.navigate_back)
        val appName = composeTestRule.activity.getString(R.string.app_name)
        val settingsDurations = composeTestRule.activity.getString(R.string.settings_durations)

        // 1. 等待 UI 稳定，确认在 TimerScreen（TopAppBar 显示 app_name）
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(appName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(appName).assertExists()

        // 2. 点击设置图标，导航到 SettingsScreen
        composeTestRule.onNodeWithContentDescription(settingsTitle).performClick()
        composeTestRule.waitForIdle()

        // 3. 确认已进入设置页（含"时长设置（分钟）"文字）
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(settingsDurations).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(settingsDurations).assertExists()

        // 4. 点击返回，回到 TimerScreen
        composeTestRule.onNodeWithContentDescription(navigateBackStr).performClick()
        composeTestRule.waitForIdle()

        // 5. 确认回到主界面
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText(appName).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText(appName).assertExists()
    }
}
