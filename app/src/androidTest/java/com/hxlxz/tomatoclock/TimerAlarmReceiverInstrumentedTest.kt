package com.hxlxz.tomatoclock

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import dagger.hilt.android.testing.BindValue

/**
 * TimerAlarmReceiver 的集成测试（Hilt 注入环境）。
 *
 * 由于 TimerAlarmReceiver 使用 @AndroidEntryPoint，其字段注入由 Hilt 管理，
 * 无法在纯 JVM 单元测试中手动注入 mock。
 * 此测试通过调用 forceFinishTimer() 后观察 Repository 状态来间接验证。
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TimerAlarmReceiverInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val mockRepo: TimerRepository = io.mockk.mockk(relaxed = true)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `forceFinishTimer when RUNNING is called by receiver`() {
        // 将测试场景改为 RUNNING 状态
        val stateFlow = kotlinx.coroutines.flow.MutableStateFlow(TimerState.RUNNING)
        io.mockk.every { mockRepo.timerState } returns stateFlow

        // 触发 receiver
        val intent = Intent(ApplicationProvider.getApplicationContext(), TimerAlarmReceiver::class.java)
        val receiver = TimerAlarmReceiver()
        receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)

        // Receiver 应调用 forceFinishTimer(fromAlarm=true)
        io.mockk.verify(exactly = 1) { mockRepo.forceFinishTimer(fromAlarm = true) }
    }

    @Test
    fun `onReceive calls forceFinishTimer with fromAlarm true`() = runTest {
        // Trigger the receiver
        val intent = Intent(ApplicationProvider.getApplicationContext(), TimerAlarmReceiver::class.java)
        val receiver = TimerAlarmReceiver()
        
        // When onReceive is called, Hilt will inject the @BindValue mockRepo into the receiver
        receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
        
        // Verify the mock was called
        io.mockk.verify(exactly = 1) { mockRepo.forceFinishTimer(fromAlarm = true) }
    }
}
