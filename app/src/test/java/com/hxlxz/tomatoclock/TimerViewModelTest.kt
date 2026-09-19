package com.hxlxz.tomatoclock

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val repository: TimerRepository = mockk(relaxed = true)
    private val dataStore: SettingsDataStore = mockk(relaxed = true)

    private lateinit var viewModel: TimerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // 为 flashScreen stateIn 提供 Flow
        every { dataStore.flashScreenFlow } returns flowOf(false)
        // 为 repository flows 提供默认值（TimerViewModel 直接暴露 repository 的 StateFlow）
        every { repository.timerMode } returns MutableStateFlow(TimerMode.FOCUS)
        every { repository.timerState } returns MutableStateFlow(TimerState.IDLE)
        every { repository.timeRemaining } returns MutableStateFlow(0L)
        every { repository.totalTimeInSeconds } returns MutableStateFlow(1500L)
        every { repository.currentCycle } returns MutableStateFlow(1)
        every { repository.totalCycles } returns MutableStateFlow(4)

        viewModel = TimerViewModel(repository, dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test startTimer delegates to repository`() {
        viewModel.startTimer()
        verify { repository.startTimer() }
    }

    @Test
    fun `test pauseTimer delegates to repository`() {
        viewModel.pauseTimer()
        verify { repository.pauseTimer() }
    }

    @Test
    fun `test stopTimer delegates to repository`() {
        viewModel.stopTimer()
        verify { repository.stopTimer() }
    }

    @Test
    fun `test nextPhase delegates to repository`() {
        viewModel.nextPhase()
        verify { repository.nextPhase() }
    }

    @Test
    fun `test snooze delegates to repository`() {
        viewModel.snooze()
        verify { repository.snooze() }
    }

    @Test
    fun `test totalCycles is exposed from repository`() {
        // 确保 ViewModel 直接代理 repository 的 StateFlow（同一引用）
        assertSame(repository.totalCycles, viewModel.totalCycles)
    }

    @Test
    fun `test timerMode is exposed from repository`() {
        assertSame(repository.timerMode, viewModel.timerMode)
    }

    @Test
    fun `test timerState is exposed from repository`() {
        assertSame(repository.timerState, viewModel.timerState)
    }

    @Test
    fun `test timeRemaining is exposed from repository`() {
        assertSame(repository.timeRemaining, viewModel.timeRemaining)
    }

    @Test
    fun `test flashScreen is a StateFlow derived from dataStore`() = runTest(testDispatcher) {
        // flashScreen 应是 StateFlow（热流）
        // 使用 WhileSubscribed(5000)，在无收集者时上游 flow 不激活
        // 初始值为 stateIn 的 initialValue = true
        val state = viewModel.flashScreen
        // 验证类型：StateFlow 有 value 属性（区别于普通冷流）
        assertNotNull("flashScreen should be a StateFlow with a value", state.value)
        // 初始值应等于 stateIn 的 initialValue
        assertEquals(true, state.value)
    }
}
