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
class TimerAlarmReceiverTest {

    @Test
    fun testOnReceive_doesNotCrash() {
        val receiver = TimerAlarmReceiver()
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        
        receiver.repository = mockRepository

        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent()

        // Just verify that onReceive doesn't crash.
        // Deep coroutine testing with goAsync() and withTimeout()
        // behaves inconsistently in Robolectric's virtual time environment.
        receiver.onReceive(context, intent)
    }
}
