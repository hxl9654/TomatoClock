package com.hxlxz.tomatoclock

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerServiceTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mockRepository: TimerRepository
    private lateinit var mockAlarmPlayer: AlarmPlayer
    private lateinit var mockDataStore: SettingsDataStore
    
    private lateinit var timerService: TimerService

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk(relaxed = true)
        mockAlarmPlayer = mockk(relaxed = true)
        mockDataStore = mockk(relaxed = true)
        
        timerService = TimerService().apply {
            this.repository = mockRepository
            this.alarmPlayer = mockAlarmPlayer
            this.settingsDataStore = mockDataStore
            this.soundModeFlow = MutableStateFlow(0) // CONTINUOUS
            this.vibrationModeFlow = MutableStateFlow(0) // CONTINUOUS
            this.ringtoneFlow = MutableStateFlow(1) // CHIME
        }

        // 默认模拟 DataStore 的返回值，避免挂起死锁（虽然不再依赖 first()，但保留无妨）
        coEvery { mockDataStore.soundModeFlow } returns flowOf(0) 
        coEvery { mockDataStore.vibrationModeFlow } returns flowOf(0) 
        coEvery { mockDataStore.ringtoneFlow } returns flowOf(1) 
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FINISHED state with suppressAlarm false plays alarm`() = runTest(testDispatcher) {
        val suppressFlow = MutableStateFlow(false)
        every { mockRepository.suppressAlarm } returns suppressFlow

        timerService.handleStateSideEffects(TimerState.FINISHED)

        coVerify(exactly = 1) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `FINISHED state with suppressAlarm true does NOT play alarm`() = runTest(testDispatcher) {
        val suppressFlow = MutableStateFlow(true)
        every { mockRepository.suppressAlarm } returns suppressFlow

        timerService.handleStateSideEffects(TimerState.FINISHED)

        coVerify(exactly = 0) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `FINISHED state plays correct sound and vibration mode from DataStore`() = runTest(testDispatcher) {
        // [B-1新增] 验证传入 play() 的参数是从 DataStore (缓存的 StateFlow) 正确读取的对应属性
        timerService.soundModeFlow = MutableStateFlow(1) // SoundMode.SINGLE
        timerService.vibrationModeFlow = MutableStateFlow(2) // VibrationMode.OFF
        timerService.ringtoneFlow = MutableStateFlow(3) // Ringtone.ZEN_BOWL
        val suppressFlow = MutableStateFlow(false)
        every { mockRepository.suppressAlarm } returns suppressFlow

        timerService.handleStateSideEffects(TimerState.FINISHED)

        coVerify(exactly = 1) {
            mockAlarmPlayer.play(SoundMode.SINGLE, VibrationMode.OFF, Ringtone.ZEN_BOWL)
        }
    }

    // ── [B-1修复] 补全缺失的 RUNNING/PAUSED/IDLE 状态覆盖测试 ──────────────

    @Test
    fun `RUNNING state calls alarmPlayer stop to clear any previous alarm sound`() = runTest(testDispatcher) {
        // RUNNING 状态进入时，应立即停止上一次的报警声
        timerService.handleStateSideEffects(TimerState.RUNNING)

        verify(exactly = 1) { mockAlarmPlayer.stop() }
        coVerify(exactly = 0) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `PAUSED state calls alarmPlayer stop`() = runTest(testDispatcher) {
        // 暂停时应停止任何正在播放的报警声
        timerService.handleStateSideEffects(TimerState.PAUSED)

        verify(exactly = 1) { mockAlarmPlayer.stop() }
        coVerify(exactly = 0) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `IDLE state calls alarmPlayer stop`() = runTest(testDispatcher) {
        // 回到 IDLE（停止）时应停止任何正在播放的报警声
        timerService.handleStateSideEffects(TimerState.IDLE)

        verify(exactly = 1) { mockAlarmPlayer.stop() }
        coVerify(exactly = 0) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    /**
     * [P1-4修复] 直接调用 @VisibleForTesting internal 的 updateNotification 方法，
     * 替换原先通过反射 invoke + InvocationTargetException 掩盖结果的脆弱测试。
     *
     * 原测试缺陷：catch(InvocationTargetException) { if "not mocked" skip } 逻辑导致
     * SecurityException 是否被捕获根本未被验证，测试永远通过。
     *
     * 修复：updateNotification 已改为 @VisibleForTesting internal，可直接调用，
     * 通过 verify(Log.e) 确认 SecurityException 被记录而非传播。
     */
    @Test
    fun `updateNotification catches SecurityException and logs error without rethrowing`() = runTest(testDispatcher) {
        io.mockk.mockkStatic(android.util.Log::class)
        every { android.util.Log.e(any(), any(), any()) } returns 0

        val spyService = io.mockk.spyk(timerService)
        val mockNotificationManager = mockk<android.app.NotificationManager>(relaxed = true)
        every { spyService.getSystemService(android.app.NotificationManager::class.java) } returns mockNotificationManager
        every { mockNotificationManager.notify(any(), any()) } throws SecurityException("Permission revoked")

        // 设置 isForeground = true，绕过 updateNotification 开头的 early return
        val isForegroundField = TimerService::class.java.getDeclaredField("isForeground")
        isForegroundField.isAccessible = true
        isForegroundField.set(spyService, true)

        // 直接调用（不再依赖反射 invoke），不应抛出异常
        spyService.updateNotification(TimerState.RUNNING, 1000L, TimerMode.FOCUS, false)

        // 核心断言：SecurityException 被捕获后必须记录日志（而非重新抛出）
        verify(atLeast = 1) { android.util.Log.e(any(), any(), any()) }

        io.mockk.unmockkStatic(android.util.Log::class)
    }

    /**
     * [BUG-01修复] 改用 spyk(timerService) 在真实服务实例上验证 startForegroundSafe 的行为。
     *
     * 原测试使用 mockk<TimerService>(relaxed=true) + callOriginal()，测试的是 mockk 框架
     * 的代理行为，而非 Android Service 真实调用栈，属于假绿测试。
     *
     * 修复方案：spyk 包装真实 TimerService 实例，仅 stub startForeground() 抛出异常，
     * 验证 stopSelf() 被调用，同时通过反射确认 isForeground 没有被置为 true。
     */
    @Test
    fun `startForegroundSafe stops service gracefully when startForeground throws SecurityException`() =
        runTest(testDispatcher) {
            io.mockk.mockkStatic(android.util.Log::class)
            every { android.util.Log.e(any(), any(), any()) } returns 0

            // 使用 spyk 包装真实实例，仅 override 会调用 Android Framework 的方法
            val spyService = io.mockk.spyk(timerService)
            every { spyService.startForeground(any<Int>(), any()) } throws SecurityException("Permission revoked")
            every { spyService.stopSelf() } returns Unit

            spyService.startForegroundSafe(mockk(relaxed = true))

            // 验证：异常发生时 stopSelf() 必须被调用一次
            verify(exactly = 1) { spyService.stopSelf() }

            // 验证：startForeground 抛异常后，isForeground 不应被置为 true
            val isForegroundField = TimerService::class.java.getDeclaredField("isForeground")
            isForegroundField.isAccessible = true
            val isForegroundValue = isForegroundField.getBoolean(spyService)
            assert(!isForegroundValue) { "isForeground should remain false when startForeground throws" }

            io.mockk.unmockkStatic(android.util.Log::class)
        }

    @Test
    fun `startForegroundSafe sets isForeground to true when startForeground succeeds`() =
        runTest(testDispatcher) {
            // 使用 spyk 包装真实实例，stub startForeground 让其成功（不抛异常）
            val spyService = io.mockk.spyk(timerService)
            every { spyService.startForeground(any<Int>(), any()) } returns Unit

            spyService.startForegroundSafe(mockk(relaxed = true))

            // 验证：成功时 isForeground 应被置为 true
            val isForegroundField = TimerService::class.java.getDeclaredField("isForeground")
            isForegroundField.isAccessible = true
            val isForegroundValue = isForegroundField.getBoolean(spyService)
            assert(isForegroundValue) { "isForeground should be true when startForeground succeeds" }

            // 验证：成功时不应调用 stopSelf()
            verify(exactly = 0) { spyService.stopSelf() }
        }
}
