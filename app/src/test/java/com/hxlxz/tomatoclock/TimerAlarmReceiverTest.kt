package com.hxlxz.tomatoclock

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [BUG-03修复] TimerAlarmReceiver 的单元测试。
 *
 * ## 测试策略说明
 * `TimerAlarmReceiver` 标注了 `@AndroidEntryPoint`，Kotlin 编译后为 final class，
 * 无法通过继承创建测试子类，且 Hilt 会在 `onReceive` 执行前覆盖 @Inject 字段。
 *
 * 解决方案：
 * 1. 提取 `handleAlarm()` 为 `@VisibleForTesting internal suspend` 方法，
 *    直接测试其对 `repository.forceFinishTimer()` 的调用。
 * 2. 手动设置 repository 字段后调用 handleAlarm()，绕开 Hilt 注入。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerAlarmReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * 验证核心路径：handleAlarm() 调用 repository.forceFinishTimer(fromAlarm = true)。
     * 直接测试提取的内部方法，绕开 @AndroidEntryPoint 的 Hilt 注入限制。
     */
    @Test
    fun `handleAlarm calls forceFinishTimer with fromAlarm=true`() = runTest {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val receiver = TimerAlarmReceiver().also { it.repository = mockRepository }

        receiver.handleAlarm()

        // 核心断言：验证 forceFinishTimer 以 fromAlarm=true 被调用
        coVerify(exactly = 1) { mockRepository.forceFinishTimer(fromAlarm = true) }
    }

    /**
     * 验证 onReceive 不会崩溃（smoke test）。
     *
     * [P1-2修复] 原测试使用 catch (_: UninitializedPropertyAccessException) 完全静默，
     * 导致任何异常都被吞没，测试永远通过（哑炮）。
     *
     * 修复：断言异常确实来自 applicationScope 未注入，而非其他未预期字段的初始化失败。
     * 这样若生产代码引入了新的未初始化字段，测试会正确变红。
     */
    @Test
    fun `onReceive does not crash when applicationScope is uninitialized`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val receiver = TimerAlarmReceiver().also { it.repository = mockRepository }

        // 仅验证 onReceive 调用本身不抛出同步异常
        // goAsync() 触发的协程在 Robolectric 中需要 applicationScope 注入才能跑完，
        // 此处验证异常确实来自 applicationScope 未注入（而非其他未预期的初始化失败）
        try {
            receiver.onReceive(context, Intent())
        } catch (e: UninitializedPropertyAccessException) {
            // 断言：异常应来自 applicationScope 字段，而非其他未预期的字段
            assertTrue(
                "UninitializedPropertyAccessException should be from 'applicationScope', but was: ${e.message}",
                e.message?.contains("applicationScope") == true
            )
        }
    }
}
