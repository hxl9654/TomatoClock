package com.example.tomatoclock

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class TimerViewModelTest {
    private val repository: TimerRepository = mockk(relaxed = true)
    private val viewModel = TimerViewModel(repository)

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
}
