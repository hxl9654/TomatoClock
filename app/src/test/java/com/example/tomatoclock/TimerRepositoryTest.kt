package com.example.tomatoclock

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        repository = TimerRepository(mockDataStore, testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        testScheduler.advanceUntilIdle()

        repository.timerMode.test {
            assertEquals(TimerMode.SHORT_BREAK, awaitItem())
        }
        repository.timeRemaining.test {
            assertEquals(5 * 60L, awaitItem())
        }
    }
}
