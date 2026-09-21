package com.hxlxz.tomatoclock

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [BUG-02修复] BootCompletedReceiver 的单元测试。
 *
 * ## 测试策略说明
 * `BootCompletedReceiver` 标注了 `@AndroidEntryPoint`，Kotlin 编译后为 final class，
 * 无法通过继承创建测试子类。Hilt 的代码生成器会在 `onReceive` 调用前通过字节码代理
 * 覆盖所有 @Inject 字段，导致直接给 `receiver.repository = mockk` 失效。
 *
 * 解决方案：提取 `handleBootCompleted()` 为 `@VisibleForTesting internal` 方法，
 * 测试直接调用该方法并手动设置 repository，绕开 Hilt 的字节码注入机制。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BootCompletedReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * 验证设备重启逻辑：handleBootCompleted() 必须调用 repository.ensureInitialized()。
     * 直接测试提取的内部方法，绕开 @AndroidEntryPoint 的 Hilt 注入限制。
     */
    @Test
    fun `handleBootCompleted calls ensureInitialized on repository`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val receiver = BootCompletedReceiver().also { it.repository = mockRepository }

        receiver.handleBootCompleted()

        // 核心断言：确认 ensureInitialized() 被调用了恰好一次
        verify(exactly = 1) { mockRepository.ensureInitialized() }
    }

    /**
     * 验证 ACTION_BOOT_COMPLETED 到达时触发初始化逻辑（通过 spyk 代理实现）。
     * 使用 io.mockk.spyk 包装真实实例以观察 handleBootCompleted() 的调用次数。
     */
    @Test
    fun `onReceive with ACTION_BOOT_COMPLETED calls handleBootCompleted`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val receiver = BootCompletedReceiver().also { it.repository = mockRepository }
        val spyReceiver = io.mockk.spyk(receiver)
        io.mockk.every { spyReceiver.handleBootCompleted() } returns Unit

        spyReceiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        verify(exactly = 1) { spyReceiver.handleBootCompleted() }
    }

    /**
     * 验证 ACTION_LOCKED_BOOT_COMPLETED 到达时触发初始化逻辑。
     */
    @Test
    fun `onReceive with ACTION_LOCKED_BOOT_COMPLETED calls handleBootCompleted`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val receiver = BootCompletedReceiver().also { it.repository = mockRepository }
        val spyReceiver = io.mockk.spyk(receiver)
        io.mockk.every { spyReceiver.handleBootCompleted() } returns Unit

        spyReceiver.onReceive(context, Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))

        verify(exactly = 1) { spyReceiver.handleBootCompleted() }
    }

    /**
     * 反向测试：收到无关广播时，不触发 handleBootCompleted()，不调用 ensureInitialized()。
     */
    @Test
    fun `onReceive with unrelated action does NOT call handleBootCompleted`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val receiver = BootCompletedReceiver().also { it.repository = mockRepository }
        val spyReceiver = io.mockk.spyk(receiver)
        io.mockk.every { spyReceiver.handleBootCompleted() } returns Unit

        spyReceiver.onReceive(context, Intent(Intent.ACTION_POWER_CONNECTED))

        // 核心断言：无关 Action 时不应触发初始化逻辑
        verify(exactly = 0) { spyReceiver.handleBootCompleted() }
    }
}
