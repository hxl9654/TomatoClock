package com.hxlxz.tomatoclock

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TimerViewModelTest {
    private val repository: TimerRepository = mockk(relaxed = true)
    private val dataStore: SettingsDataStore = mockk(relaxed = true)
    private val viewModel = TimerViewModel(repository, dataStore)

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
        // Ensures ViewModel properly delegates the totalCycles StateFlow
        assert(viewModel.totalCycles === repository.totalCycles)
    }
}
