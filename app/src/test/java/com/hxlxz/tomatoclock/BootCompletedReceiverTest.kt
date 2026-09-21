package com.hxlxz.tomatoclock

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BootCompletedReceiverTest {

    @Test
    fun testOnReceive_withBootCompletedAction() {
        val receiver = BootCompletedReceiver()
        // Mock repository to avoid real implementation dependencies
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        receiver.repository = mockRepository

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        receiver.onReceive(context, intent)

        // The receiver just logs and initializes repository which fetches state on init.
        // We ensure no exception is thrown.
    }

    @Test
    fun testOnReceive_withLockedBootCompletedAction() {
        val receiver = BootCompletedReceiver()
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        receiver.repository = mockRepository

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED)

        receiver.onReceive(context, intent)
    }
}
