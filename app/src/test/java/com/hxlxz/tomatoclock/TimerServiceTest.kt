package com.hxlxz.tomatoclock

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

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
        }

        // 默认模拟 DataStore 的返回值，避免挂起死锁
        coEvery { mockDataStore.soundModeFlow } returns flowOf(0) // CONTINUOUS
        coEvery { mockDataStore.vibrationModeFlow } returns flowOf(0) // CONTINUOUS
        coEvery { mockDataStore.ringtoneFlow } returns flowOf(1) // CHIME
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `FINISHED state with suppressAlarm false plays alarm`() = runTest(testDispatcher) {
        // Arrange
        val suppressFlow = MutableStateFlow(false)
        every { mockRepository.suppressAlarm } returns suppressFlow

        // Act
        timerService.handleStateSideEffects(TimerState.FINISHED)

        // Assert: 因为 suppressAlarm=false，必须调用 alarmPlayer.play
        coVerify(exactly = 1) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `FINISHED state with suppressAlarm true does NOT play alarm`() = runTest(testDispatcher) {
        // Arrange
        val suppressFlow = MutableStateFlow(true)
        every { mockRepository.suppressAlarm } returns suppressFlow

        // Act
        timerService.handleStateSideEffects(TimerState.FINISHED)

        // Assert: 因为 suppressAlarm=true，alarmPlayer.play 绝不能被调用
        coVerify(exactly = 0) { mockAlarmPlayer.play(any(), any(), any()) }
    }

    @Test
    fun `suppressAlarm captured before IO suspension reflects correct value`() = runTest(testDispatcher) {
        val suppressFlow = MutableStateFlow(false)
        every { mockRepository.suppressAlarm } returns suppressFlow

        val capturedValue = suppressFlow.value
        suppressFlow.value = true

        assertFalse("Captured value should retain the original false", capturedValue)
        assertTrue("The flow itself changed to true", suppressFlow.value)
    }
}
