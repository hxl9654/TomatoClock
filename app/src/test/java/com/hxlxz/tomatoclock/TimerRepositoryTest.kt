package com.hxlxz.tomatoclock

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import android.os.SystemClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerRepositoryTest {

    private lateinit var repository: TimerRepository
    private lateinit var mockDataStore: SettingsDataStore
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
        io.mockk.every { SystemClock.elapsedRealtime() } answers { testDispatcher.scheduler.currentTime }

        Dispatchers.setMain(testDispatcher)
        mockDataStore = mockk(relaxed = true)

        // Mock default flows
        coEvery { mockDataStore.focusTimeFlow } returns flowOf(25)
        coEvery { mockDataStore.shortBreakTimeFlow } returns flowOf(5)
        coEvery { mockDataStore.longBreakTimeFlow } returns flowOf(15)
        coEvery { mockDataStore.cyclesFlow } returns flowOf(4)
        coEvery { mockDataStore.snoozeTimeFlow } returns flowOf(5)
        coEvery { mockDataStore.autoStartBreakFlow } returns flowOf(false)
        coEvery { mockDataStore.autoStartFocusFlow } returns flowOf(false)

        val timeConfig = TimeConfig()
        val testScope = CoroutineScope(SupervisorJob() + testDispatcher)
        repository = TimerRepository(mockDataStore, timeConfig, testScope)
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
            advanceTimeBy(1001)
            val afterOneSec = awaitItem()
            assertEquals(initial - 1, afterOneSec)
        }
    }

    @Test
    fun `pauseTimer stops countdown and updates state`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        repository.startTimer()
        testScheduler.runCurrent()
        advanceTimeBy(2000)

        repository.pauseTimer()
        testScheduler.runCurrent()

        repository.timerState.test {
            assertEquals(TimerState.PAUSED, awaitItem())
        }

        repository.timeRemaining.test {
            val pausedTime = awaitItem()
            advanceTimeBy(3000)
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
        advanceTimeBy(25 * 60 * 1000L + 1000)
        
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
    fun `onTimerFinished auto-starts next phase when enabled`() = runTest(testDispatcher) {
        coEvery { mockDataStore.autoStartBreakFlow } returns flowOf(true)
        val repoWithAutoStart = TimerRepository(mockDataStore, TimeConfig(), CoroutineScope(SupervisorJob() + testDispatcher))
        testScheduler.advanceUntilIdle()
        
        repoWithAutoStart.startTimer()
        advanceTimeBy(25 * 60 * 1000L + 1000)
        
        repoWithAutoStart.timerMode.test {
            assertEquals(TimerMode.SHORT_BREAK, awaitItem())
        }
        repoWithAutoStart.timerState.test {
            assertEquals(TimerState.RUNNING, awaitItem())
        }
    }

    @Test
    fun `onTimerFinished stays finished when auto-start is disabled`() = runTest(testDispatcher) {
        testScheduler.advanceUntilIdle()
        
        repository.startTimer()
        advanceTimeBy(25 * 60 * 1000L + 1000)
        
        repository.timerState.test {
            assertEquals(TimerState.FINISHED, awaitItem())
        }
        repository.timerMode.test {
            assertEquals(TimerMode.FOCUS, awaitItem())
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

        repository.stopTimer()
        testScheduler.advanceUntilIdle()

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
        val repo = TimerRepository(mockDataStore, TimeConfig(), testScope)
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
        val repo = TimerRepository(mockDataStore, TimeConfig(), testScope)
        testScheduler.advanceUntilIdle()

        // 启动计时
        repo.startTimer()
        testScheduler.runCurrent()
        assertEquals(TimerState.RUNNING, repo.timerState.value)

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
        val repo = TimerRepository(mockDataStore, TimeConfig(), testScope)
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
}
