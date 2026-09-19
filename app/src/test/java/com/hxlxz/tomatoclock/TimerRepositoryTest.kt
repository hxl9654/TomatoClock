package com.hxlxz.tomatoclock

import android.os.SystemClock
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class TimerRepositoryTest {

    private lateinit var repository: TimerRepository
    private lateinit var mockDataStore: SettingsDataStore
    private lateinit var mockAlarmScheduler: AlarmScheduler
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
        io.mockk.every { SystemClock.elapsedRealtime() } answers { testDispatcher.scheduler.currentTime }

        Dispatchers.setMain(testDispatcher)
        mockDataStore = mockk(relaxed = true)
        mockAlarmScheduler = mockk(relaxed = true)

        // Mock default flows
        coEvery { mockDataStore.focusTimeFlow } returns flowOf(25)
        coEvery { mockDataStore.shortBreakTimeFlow } returns flowOf(5)
        coEvery { mockDataStore.longBreakTimeFlow } returns flowOf(15)
        coEvery { mockDataStore.cyclesFlow } returns flowOf(4)
        coEvery { mockDataStore.snoozeTimeFlow } returns flowOf(5)
        coEvery { mockDataStore.savedTimerStateFlow } returns flowOf(null)

        val timeConfig = TimeConfig()
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        repository = TimerRepository(mockDataStore, timeConfig, testScope, mockAlarmScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `initial state is IDLE and time is set correctly`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.timerState.test {
            assertEquals(TimerState.IDLE, awaitItem())
        }
        repository.timeRemaining.test {
            assertEquals(25 * 60L, awaitItem())
        }
    }

    @Test
    fun `startTimer begins countdown`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()

        repository.timerState.test {
            assertEquals(TimerState.RUNNING, awaitItem())
        }

        repository.timeRemaining.test {
            val initial = awaitItem()
            advanceTimeBy(1001.milliseconds)
            val afterOneSec = awaitItem()
            assertEquals(initial - 1, afterOneSec)
        }
    }

    @Test
    fun `pauseTimer stops countdown and updates state`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        advanceTimeBy(2000.milliseconds)

        repository.pauseTimer()
        testScheduler.runCurrent()

        repository.timerState.test {
            assertEquals(TimerState.PAUSED, awaitItem())
        }

        repository.timeRemaining.test {
            awaitItem() // Consume current state
            advanceTimeBy(3000.milliseconds)
            // Time should not change when paused
            expectNoEvents()
        }
    }

    @Test
    fun `nextPhase transitions from focus to short break`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.nextPhase()
        testScheduler.runCurrent() // Use runCurrent instead of advanceUntilIdle so the timer doesn't finish

        repository.timerMode.test {
            assertEquals(TimerMode.SHORT_BREAK, awaitItem())
        }
        repository.timerState.test {
            assertEquals(TimerState.RUNNING, awaitItem())
        }
        repository.timeRemaining.test {
            assertEquals(5 * 60L, awaitItem())
        }
    }

    @Test
    fun `stopTimer resets timer to FOCUS mode and cycle 1`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        // Move to SHORT_BREAK first
        repository.nextPhase()
        testScheduler.runCurrent()

        repository.stopTimer()
        testScheduler.advanceUntilIdle()

        repository.timerState.test {
            assertEquals(TimerState.IDLE, awaitItem())
        }
        repository.timerMode.test {
            assertEquals(TimerMode.FOCUS, awaitItem())
        }
        repository.currentCycle.test {
            assertEquals(1, awaitItem())
        }
        repository.timeRemaining.test {
            assertEquals(25 * 60L, awaitItem())
        }
    }

    @Test
    fun `snooze adds time and resumes countdown`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        // Let it finish
        repository.startTimer()
        advanceTimeBy((25 * 60 * 1000L + 1000).milliseconds)

        repository.snooze()
        testScheduler.runCurrent()

        repository.timerState.test {
            assertEquals(TimerState.RUNNING, awaitItem())
        }
        repository.timeRemaining.test {
            assertEquals(5 * 60L, awaitItem())
        }
    }

    @Test
    fun `full pomodoro cycle transitions correctly`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()

        // Cycle 1: Focus
        assertEquals(TimerMode.FOCUS, repository.timerMode.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 1: Short Break
        assertEquals(TimerMode.SHORT_BREAK, repository.timerMode.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 2: Focus
        assertEquals(2, repository.currentCycle.value)
        assertEquals(TimerMode.FOCUS, repository.timerMode.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 2: Short Break
        assertEquals(TimerMode.SHORT_BREAK, repository.timerMode.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 3: Focus
        assertEquals(3, repository.currentCycle.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 3: Short Break
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 4: Focus
        assertEquals(4, repository.currentCycle.value)
        assertEquals(TimerMode.FOCUS, repository.timerMode.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 4: Long Break (because cycle reaches 4)
        assertEquals(TimerMode.LONG_BREAK, repository.timerMode.value)
        repository.nextPhase()
        testScheduler.runCurrent()

        // Cycle 1: Focus again (reset)
        assertEquals(1, repository.currentCycle.value)
        assertEquals(TimerMode.FOCUS, repository.timerMode.value)
    }

    @Test
    fun `nextPhase transitions from long break back to focus and resets cycle`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        // Simulate being in long break with cycle=4
        repeat(3) {
            repository.nextPhase() // Focus -> Short Break
            testScheduler.runCurrent()
            repository.nextPhase() // Short Break -> Focus
            testScheduler.runCurrent()
        }
        repository.nextPhase() // Final Focus -> Long Break
        testScheduler.runCurrent()
        assertEquals(TimerMode.LONG_BREAK, repository.timerMode.value)

        repository.nextPhase() // Long Break -> Focus (reset)
        testScheduler.runCurrent()
        assertEquals(TimerMode.FOCUS, repository.timerMode.value)
        assertEquals(1, repository.currentCycle.value)
    }

    @Test
    fun `stopTimer while RUNNING cancels countdown and resets state`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        assertEquals(TimerState.RUNNING, repository.timerState.value)
        io.mockk.verify { mockAlarmScheduler.scheduleAlarm(any()) }

        repository.stopTimer()
        testScheduler.advanceUntilIdle()
        io.mockk.verify { mockAlarmScheduler.cancelAlarm() }

        assertEquals(TimerState.IDLE, repository.timerState.value)
        assertEquals(TimerMode.FOCUS, repository.timerMode.value)
        assertEquals(1, repository.currentCycle.value)
        assertEquals(25 * 60L, repository.timeRemaining.value)
    }

    // ── 本次新增：响应式设置变更测试 ──────────────────────────────────────

    @Test
    fun `focusTimeFlow change updates timeRemaining when IDLE`() = runTest(testDispatcher) {
        // 模拟 focusTimeFlow 发射新值 30 分钟
        val focusFlow = kotlinx.coroutines.flow.MutableStateFlow(25)
        coEvery { mockDataStore.focusTimeFlow } returns focusFlow

        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val repo = TimerRepository(mockDataStore, TimeConfig(), testScope, mockAlarmScheduler)
        testScheduler.advanceUntilIdle()

        // 初始值 25 分钟
        assertEquals(25 * 60L, repo.timeRemaining.value)

        // 设置改为 30 分钟
        focusFlow.value = 30
        testScheduler.advanceUntilIdle()

        // IDLE 状态下首页时间应立即更新
        assertEquals(30 * 60L, repo.timeRemaining.value)
        assertEquals(30 * 60L, repo.totalTimeInSeconds.value)
    }

    @Test
    fun `focusTimeFlow change does NOT affect timeRemaining when RUNNING`() = runTest(testDispatcher) {
        val focusFlow = kotlinx.coroutines.flow.MutableStateFlow(25)
        coEvery { mockDataStore.focusTimeFlow } returns focusFlow

        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val repo = TimerRepository(mockDataStore, TimeConfig(), testScope, mockAlarmScheduler)
        testScheduler.advanceUntilIdle()

        // 启动计时
        repo.startTimer()
        testScheduler.runCurrent()
        assertEquals(TimerState.RUNNING, repo.timerState.value)

        io.mockk.verify { mockAlarmScheduler.scheduleAlarm(any()) }

        val timeBeforeChange = repo.timeRemaining.value

        // 运行中修改设置
        focusFlow.value = 30
        testScheduler.advanceUntilIdle()

        // RUNNING 状态下时间不应被 settings 变化覆盖
        // (timeRemaining 可能因倒计时减少，但不会被重置为 30*60)
        assert(repo.timeRemaining.value <= timeBeforeChange) {
            "Running timer should not be reset by settings change"
        }
        assert(repo.timeRemaining.value < 30 * 60L) {
            "Time should not jump to new setting value while running"
        }
    }

    @Test
    fun `cyclesFlow change always updates totalCycles regardless of timer state`() = runTest(testDispatcher) {
        val cyclesFlow = kotlinx.coroutines.flow.MutableStateFlow(4)
        coEvery { mockDataStore.cyclesFlow } returns cyclesFlow

        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        val repo = TimerRepository(mockDataStore, TimeConfig(), testScope, mockAlarmScheduler)
        testScheduler.advanceUntilIdle()

        assertEquals(4, repo.totalCycles.value)

        // IDLE 状态下修改
        cyclesFlow.value = 6
        testScheduler.advanceUntilIdle()
        assertEquals(6, repo.totalCycles.value)

        // RUNNING 状态下修改
        repo.startTimer()
        testScheduler.runCurrent()
        cyclesFlow.value = 3
        testScheduler.advanceUntilIdle()
        assertEquals(3, repo.totalCycles.value)
    }

    // ── forceFinishTimer 边界条件测试 ────────────────────────────────────────

    @Test
    fun `forceFinishTimer when RUNNING transitions to FINISHED and cancels alarm`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        assertEquals(TimerState.RUNNING, repository.timerState.value)

        repository.forceFinishTimer()
        testScheduler.advanceUntilIdle()

        assertEquals(TimerState.FINISHED, repository.timerState.value)
        assertEquals(0L, repository.timeRemaining.value)
        // 验证 AlarmScheduler.cancelAlarm() 至少被调用一次（startCountdown + forceFinish 各一次）
        io.mockk.verify(atLeast = 1) { mockAlarmScheduler.cancelAlarm() }
    }

    @Test
    fun `forceFinishTimer when PAUSED does nothing`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        repository.pauseTimer()
        testScheduler.runCurrent()
        assertEquals(TimerState.PAUSED, repository.timerState.value)

        // 记录当前 cancelAlarm 调用次数
        io.mockk.clearMocks(mockAlarmScheduler)

        repository.forceFinishTimer()
        testScheduler.advanceUntilIdle()

        // PAUSED 状态下 forceFinishTimer 应无效，状态不变
        assertEquals(TimerState.PAUSED, repository.timerState.value)
        io.mockk.verify(exactly = 0) { mockAlarmScheduler.scheduleAlarm(any()) }
    }

    @Test
    fun `forceFinishTimer when IDLE does nothing`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        assertEquals(TimerState.IDLE, repository.timerState.value)

        io.mockk.clearMocks(mockAlarmScheduler)

        repository.forceFinishTimer()
        testScheduler.advanceUntilIdle()

        // IDLE 状态下 forceFinishTimer 应无效
        assertEquals(TimerState.IDLE, repository.timerState.value)
        io.mockk.verify(exactly = 0) { mockAlarmScheduler.scheduleAlarm(any()) }
    }

    @Test
    fun `forceFinishTimer is idempotent when called twice on RUNNING`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()

        repository.forceFinishTimer()
        testScheduler.advanceUntilIdle()
        assertEquals(TimerState.FINISHED, repository.timerState.value)

        // 第二次调用：FINISHED 状态下，不应再次触发
        repository.forceFinishTimer()
        testScheduler.advanceUntilIdle()
        // 状态仍是 FINISHED，不会重复迁移
        assertEquals(TimerState.FINISHED, repository.timerState.value)
    }

    @Test
    fun `addTime when RUNNING increases timeRemaining and totalTime`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()

        val initialTotal = repository.totalTimeInSeconds.value
        val initialRemaining = repository.timeRemaining.value

        // Add 5 minutes (300 seconds)
        repository.addTime(300)
        testScheduler.runCurrent()

        assertEquals(initialTotal + 300, repository.totalTimeInSeconds.value)
        assertEquals(initialRemaining + 300, repository.timeRemaining.value)
        io.mockk.verify(atLeast = 1) { mockAlarmScheduler.scheduleAlarm(any()) }
    }

    @Test
    fun `addTime when PAUSED increases pausedTimeRemaining`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        advanceTimeBy(2000.milliseconds)

        repository.pauseTimer()
        testScheduler.runCurrent()

        val timeBeforeAdd = repository.timeRemaining.value
        val totalBeforeAdd = repository.totalTimeInSeconds.value

        repository.addTime(300)
        testScheduler.runCurrent()

        assertEquals(timeBeforeAdd + 300, repository.timeRemaining.value)
        assertEquals(totalBeforeAdd + 300, repository.totalTimeInSeconds.value)

        // Resume to ensure the added time is carried over
        repository.startTimer()
        testScheduler.runCurrent()
        assertEquals(timeBeforeAdd + 300, repository.timeRemaining.value)
    }

    @Test
    fun `forceFinishTimer from alarm ignores stale alarm after addTime`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()

        // Fast-forward 1 minute
        advanceTimeBy(60_000.milliseconds)

        // Add 5 minutes
        repository.addTime(300)
        testScheduler.runCurrent()

        // Simulate an alarm firing for the OLD time (which is now in the past compared to targetEndTimeMs)
        // Since we just added 5 mins (300_000ms), the current time is 300_000ms before targetEndTimeMs.
        // A stale alarm fires, meaning now < targetEndTimeMs - 2000L.
        repository.forceFinishTimer(fromAlarm = true)
        testScheduler.runCurrent()

        // Timer should STILL be RUNNING because the stale alarm was ignored
        assertEquals(TimerState.RUNNING, repository.timerState.value)
    }

    // ── 后台状态恢复机制测试 (Background State Restoration Tests) ────────────────

    @Test
    fun `restore state discards state if older than 1 hour`() = runTest(testDispatcher) {
        val oldTimestamp = System.currentTimeMillis() - 3600_000L - 1000L // 1 hour + 1 second ago
        val savedState = SavedTimerState(
            state = TimerState.RUNNING,
            mode = TimerMode.SHORT_BREAK,
            targetEndTimeWallClock = System.currentTimeMillis() + 5000L,
            pausedTimeRemaining = 0L,
            currentCycle = 2,
            lastSavedTimestamp = oldTimestamp
        )
        coEvery { mockDataStore.savedTimerStateFlow } returns kotlinx.coroutines.flow.flowOf(savedState)

        val repo = TimerRepository(
            mockDataStore,
            TimeConfig(),
            CoroutineScope(SupervisorJob() + testDispatcher),
            mockAlarmScheduler
        )
        testScheduler.advanceUntilIdle()

        // 超过一小时应该调用 clearTimerState 并且状态保持 IDLE 默认值
        io.mockk.coVerify { mockDataStore.clearTimerState() }
        assertEquals(TimerState.IDLE, repo.timerState.value)
        assertEquals(TimerMode.FOCUS, repo.timerMode.value) // 默认值
    }

    @Test
    fun `restore state resumes countdown if target time is in the future`() = runTest(testDispatcher) {
        val now = System.currentTimeMillis()
        val futureTime = now + 120_000L // 2 minutes from now
        val savedState = SavedTimerState(
            state = TimerState.RUNNING,
            mode = TimerMode.SHORT_BREAK,
            targetEndTimeWallClock = futureTime,
            pausedTimeRemaining = 0L,
            currentCycle = 3,
            lastSavedTimestamp = now - 5000L // Saved 5 seconds ago
        )
        coEvery { mockDataStore.savedTimerStateFlow } returns kotlinx.coroutines.flow.flowOf(savedState)

        val repo = TimerRepository(
            mockDataStore,
            TimeConfig(),
            CoroutineScope(SupervisorJob() + testDispatcher),
            mockAlarmScheduler
        )
        testScheduler.runCurrent()

        assertEquals(TimerState.RUNNING, repo.timerState.value)
        assertEquals(TimerMode.SHORT_BREAK, repo.timerMode.value)
        assertEquals(3, repo.currentCycle.value)
        assertEquals(false, repo.suppressAlarm.value)
        // Approx 120 seconds
        org.junit.Assert.assertTrue(
            "Expected time around 120, but was ${repo.timeRemaining.value}",
            repo.timeRemaining.value in 110L..125L
        )
    }

    @Test
    fun `restore state finishes and rings if target time is in the past by less than 10 mins`() =
        runTest(testDispatcher) {
            val now = System.currentTimeMillis()
            val pastTime = now - 5 * 60 * 1000L // 5 minutes ago
            val savedState = SavedTimerState(
                state = TimerState.RUNNING,
                mode = TimerMode.FOCUS,
                targetEndTimeWallClock = pastTime,
                pausedTimeRemaining = 0L,
                currentCycle = 1,
                lastSavedTimestamp = now - 30 * 60 * 1000L // Saved 30 mins ago
            )
            coEvery { mockDataStore.savedTimerStateFlow } returns kotlinx.coroutines.flow.flowOf(savedState)

            val repo = TimerRepository(
                mockDataStore,
                TimeConfig(),
                CoroutineScope(SupervisorJob() + testDispatcher),
                mockAlarmScheduler
            )
            testScheduler.advanceUntilIdle()

            assertEquals(TimerState.FINISHED, repo.timerState.value)
            assertEquals(0L, repo.timeRemaining.value)
            assertEquals(false, repo.suppressAlarm.value) // 不超过10分钟，正常补发提醒
        }

    @Test
    fun `restore state finishes silently if target time is in the past by more than 10 mins`() =
        runTest(testDispatcher) {
            val now = System.currentTimeMillis()
            val pastTime = now - 15 * 60 * 1000L // 15 minutes ago
            val savedState = SavedTimerState(
                state = TimerState.RUNNING,
                mode = TimerMode.FOCUS,
                targetEndTimeWallClock = pastTime,
                pausedTimeRemaining = 0L,
                currentCycle = 1,
                lastSavedTimestamp = now - 40 * 60 * 1000L // Saved 40 mins ago
            )
            coEvery { mockDataStore.savedTimerStateFlow } returns kotlinx.coroutines.flow.flowOf(savedState)

            val repo = TimerRepository(
                mockDataStore,
                TimeConfig(),
                CoroutineScope(SupervisorJob() + testDispatcher),
                mockAlarmScheduler
            )
            testScheduler.advanceUntilIdle()

            assertEquals(TimerState.FINISHED, repo.timerState.value)
            assertEquals(0L, repo.timeRemaining.value)
            assertEquals(true, repo.suppressAlarm.value) // 超过10分钟，强制静音
        }

    @Test
    fun `saveCurrentState persists correct values`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()

        repository.saveCurrentState()

        io.mockk.coVerify {
            mockDataStore.saveTimerState(match {
                it.state == TimerState.RUNNING &&
                        it.mode == TimerMode.FOCUS &&
                        it.currentCycle == 1 &&
                        it.targetEndTimeWallClock > System.currentTimeMillis()
            })
        }
    }

    // 【L-4修复】补全 saveCurrentState 的 PAUSED 场景测试
    @Test
    fun `saveCurrentState when PAUSED persists correct pausedTimeRemaining`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        advanceTimeBy(5000.milliseconds)

        repository.pauseTimer()
        testScheduler.runCurrent()

        repository.saveCurrentState()

        io.mockk.coVerify {
            mockDataStore.saveTimerState(match {
                it.state == TimerState.PAUSED &&
                        it.mode == TimerMode.FOCUS &&
                        it.pausedTimeRemaining > 0L &&
                        it.targetEndTimeWallClock == 0L // PAUSED 时不保存 targetEndTime
            })
        }
    }

    @Test
    fun `saveCurrentState when IDLE persists IDLE state`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        // 默认为 IDLE

        repository.saveCurrentState()

        io.mockk.coVerify {
            mockDataStore.saveTimerState(match {
                it.state == TimerState.IDLE &&
                        it.mode == TimerMode.FOCUS &&
                        it.totalTimeInSeconds == 25 * 60L
            })
        }
    }

    // 【H-4修复验证】addTime 后 saveCurrentState 应保存延长后的 totalTimeInSeconds
    @Test
    fun `saveCurrentState after addTime persists extended totalTimeInSeconds`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()

        // 加 5 分钟 (300 秒)
        repository.addTime(300L)
        testScheduler.runCurrent()

        repository.saveCurrentState()

        io.mockk.coVerify {
            mockDataStore.saveTimerState(match {
                it.state == TimerState.RUNNING &&
                        // totalTimeInSeconds 应包含加时后的实际总时长（25*60 + 300 = 1800）
                        it.totalTimeInSeconds == (25 * 60 + 300).toLong()
            })
        }
    }

    // 【C-2修复验证】恢复后 settings 监听不应在 PAUSED 状态覆盖已恢复的 timeRemaining
    @Test
    fun `restoring PAUSED state does not get overwritten by focusTimeFlow collector`() = runTest(testDispatcher) {
        val pausedRemaining = 720L // 12 分钟
        val savedState = SavedTimerState(
            state = TimerState.PAUSED,
            mode = TimerMode.FOCUS,
            targetEndTimeWallClock = 0L,
            pausedTimeRemaining = pausedRemaining,
            currentCycle = 2,
            lastSavedTimestamp = System.currentTimeMillis(),
            totalTimeInSeconds = 1500L
        )
        coEvery { mockDataStore.savedTimerStateFlow } returns flowOf(savedState)

        val restoredRepo = TimerRepository(
            mockDataStore,
            TimeConfig(),
            CoroutineScope(testDispatcher + SupervisorJob()),
            mockAlarmScheduler
        )
        testScheduler.advanceUntilIdle()

        // PAUSED 状态下，timeRemaining 应保持恢复值，不被 focusTimeFlow 覆盖
        assertEquals(pausedRemaining, restoredRepo.timeRemaining.value)
        assertEquals(TimerState.PAUSED, restoredRepo.timerState.value)
    }
}
