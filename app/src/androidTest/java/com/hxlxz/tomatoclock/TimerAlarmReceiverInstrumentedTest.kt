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
    fun `onReceive calls forceFinishTimer with fromAlarm true`() = runTest {
        // [Fix-TG-1] 验证 Receiver 触发后 forceFinishTimer(fromAlarm=true) 被调用。
        val intent = Intent(ApplicationProvider.getApplicationContext(), TimerAlarmReceiver::class.java)
        val receiver = TimerAlarmReceiver()

        // When onReceive is called, Hilt will inject the @BindValue mockRepo into the receiver
        receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)

        // Verify the mock was called with fromAlarm=true
        io.mockk.coVerify(exactly = 1, timeout = 3000) { mockRepo.forceFinishTimer(fromAlarm = true) }
    }

    @Test
    fun `onReceive called multiple times only triggers once per call`() = runTest {
        // [Fix-TG-1] 取代原先重复的「RUNNING 状态」用例，改为验证调用幂等性：
        // 每次 onReceive 应当恰好触发一次 forceFinishTimer，不多不少。
        val intent = Intent(ApplicationProvider.getApplicationContext(), TimerAlarmReceiver::class.java)
        val receiver = TimerAlarmReceiver()

        receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)
        receiver.onReceive(ApplicationProvider.getApplicationContext(), intent)

        // 每次 onReceive 都应调用一次，共调用两次
        io.mockk.coVerify(exactly = 2, timeout = 3000) { mockRepo.forceFinishTimer(fromAlarm = true) }
    }
}
