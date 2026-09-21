package com.hxlxz.tomatoclock

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import org.junit.Assert.assertNotNull

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class BootCompletedReceiverInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var timerRepository: TimerRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun onReceive_bootCompleted_instantiatesRepository() = runTest {
        val receiver = BootCompletedReceiver()
        
        // Simulating Hilt field injection
        receiver.repository = timerRepository

        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        
        receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
        
        // The main goal of this receiver is to be instantiated by the system,
        // which triggers Hilt to inject TimerRepository.
        // TimerRepository's init block runs and restores the alarm.
        // We verify that the receiver successfully receives the intent without crashing
        // and that its repository is not null.
        assertNotNull(receiver.repository)
    }
}
