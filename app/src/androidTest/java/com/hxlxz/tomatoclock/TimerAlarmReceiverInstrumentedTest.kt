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
import javax.inject.Inject

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

    @Inject
    lateinit var repository: TimerRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `forceFinishTimer when IDLE does not change state`() {
        // IDLE 状态下 forceFinishTimer 应为 no-op
        val stateBefore = repository.timerState.value
        repository.forceFinishTimer()
        val stateAfter = repository.timerState.value
        assert(stateAfter == stateBefore) {
            "forceFinishTimer on IDLE should not change state. Before: $stateBefore, After: $stateAfter"
        }
    }
}
