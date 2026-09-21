@file:OptIn(ExperimentalCoroutinesApi::class)

package com.hxlxz.tomatoclock

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.content.BroadcastReceiver
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    fun `handleBootCompleted calls ensureInitialized on repository`() = runTest {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val mockPendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        
        val receiver = BootCompletedReceiver().also { 
            it.repository = mockRepository
            it.applicationScope = testScope 
        }

        receiver.handleBootCompleted(mockPendingResult)

        // 核心断言：确认 ensureInitialized() 被调用了恰好一次，且最后 finish 被调用
        coVerify(exactly = 1) { mockRepository.ensureInitialized() }
        verify(exactly = 1) { mockPendingResult.finish() }
    }

    /**
     * 验证 ACTION_BOOT_COMPLETED 到达时触发初始化逻辑（通过 spyk 代理实现）。
     *
     * [P2-6] 覆盖局限性说明：
     * BootCompletedReceiver 标注了 @AndroidEntryPoint，在真机环境 Hilt 会在字节码层拦截
     * onReceive 调用，执行依赖注入逻辑。
     * 此处的 Robolectric 测试中使用了真实对象实例副本（spyk），并绕过了 Hilt，
     * 因此测试调用的是未被修改的原始 Kotlin 字节码路径，仅验证纯业务分发逻辑。
     *
     * 核心初始化逻辑（ensureInitialized）实际由 handleBootCompleted 的独立测试覆盖。
     */
    @Test
    fun `onReceive with ACTION_BOOT_COMPLETED calls handleBootCompleted`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val testScope = TestScope(UnconfinedTestDispatcher())
        val receiver = BootCompletedReceiver().also { 
            it.repository = mockRepository
            it.applicationScope = testScope 
        }
        val spyReceiver = io.mockk.spyk(receiver)
        val mockPendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
        io.mockk.every { spyReceiver.goAsync() } returns mockPendingResult
        io.mockk.every { spyReceiver.handleBootCompleted(any()) } returns Unit

        spyReceiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        verify(exactly = 1) { spyReceiver.handleBootCompleted(mockPendingResult) }
    }

    /**
     * 验证 ACTION_LOCKED_BOOT_COMPLETED 到达时触发初始化逻辑。
     */
    @Test
    fun `onReceive with ACTION_LOCKED_BOOT_COMPLETED calls handleBootCompleted`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val testScope = TestScope(UnconfinedTestDispatcher())
        val receiver = BootCompletedReceiver().also { 
            it.repository = mockRepository
            it.applicationScope = testScope
        }
        val spyReceiver = io.mockk.spyk(receiver)
        val mockPendingResult = mockk<BroadcastReceiver.PendingResult>(relaxed = true)
        io.mockk.every { spyReceiver.goAsync() } returns mockPendingResult
        io.mockk.every { spyReceiver.handleBootCompleted(any()) } returns Unit

        spyReceiver.onReceive(context, Intent(Intent.ACTION_LOCKED_BOOT_COMPLETED))

        verify(exactly = 1) { spyReceiver.handleBootCompleted(mockPendingResult) }
    }

    /**
     * 反向测试：收到无关广播时，不触发 handleBootCompleted()，不调用 ensureInitialized()。
     */
    @Test
    fun `onReceive with unrelated action does NOT call handleBootCompleted`() {
        val mockRepository = mockk<TimerRepository>(relaxed = true)
        val testScope = TestScope(UnconfinedTestDispatcher())
        val receiver = BootCompletedReceiver().also { 
            it.repository = mockRepository
            it.applicationScope = testScope
        }
        val spyReceiver = io.mockk.spyk(receiver)
        io.mockk.every { spyReceiver.handleBootCompleted(any()) } returns Unit

        spyReceiver.onReceive(context, Intent(Intent.ACTION_POWER_CONNECTED))

        // 核心断言：无关 Action 时不应触发初始化逻辑
        verify(exactly = 0) { spyReceiver.handleBootCompleted(any()) }
    }
}
