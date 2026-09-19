package com.hxlxz.tomatoclock

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import com.hxlxz.tomatoclock.ui.TimerScreen
import com.hxlxz.tomatoclock.TimerViewModel
import io.mockk.mockk

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TimerScreenVrtTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTimerScreenIdleSnapshot() {
        // TODO: Replace with Roborazzi once plugin is configured
        // composeTestRule.setContent {
        //     TimerScreen(onNavigateToSettings = {}, viewModel = mockk(relaxed = true))
        // }
        // composeTestRule.onRoot().captureToImage() // Verify rendering doesn't crash
    }
}
