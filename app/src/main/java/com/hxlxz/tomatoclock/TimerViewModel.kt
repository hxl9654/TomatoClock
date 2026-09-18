package com.hxlxz.tomatoclock

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: TimerRepository
) : ViewModel() {

    val timerMode = repository.timerMode
    val timerState = repository.timerState
    val timeRemaining = repository.timeRemaining
    val totalTimeInSeconds = repository.totalTimeInSeconds
    val currentCycle = repository.currentCycle
    val totalCycles = repository.totalCycles

    fun startTimer() = repository.startTimer()
    fun pauseTimer() = repository.pauseTimer()
    fun stopTimer() = repository.stopTimer()
    fun nextPhase() = repository.nextPhase()
    fun snooze() = repository.snooze()
}
