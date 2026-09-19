package com.hxlxz.tomatoclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: TimerRepository,
    settingsDataStore: SettingsDataStore
) : ViewModel() {

    val timerMode = repository.timerMode
    val timerState = repository.timerState
    val timeRemaining = repository.timeRemaining
    val totalTimeInSeconds = repository.totalTimeInSeconds
    val currentCycle = repository.currentCycle
    val totalCycles = repository.totalCycles

    // 转换为 StateFlow，共享订阅，避免多个 collector 重复触发 DataStore 读取
    val flashScreen = settingsDataStore.flashScreenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun startTimer() {
        viewModelScope.launch { repository.startTimer() }
    }

    fun pauseTimer() {
        viewModelScope.launch { repository.pauseTimer() }
    }

    fun stopTimer() {
        viewModelScope.launch { repository.stopTimer() }
    }

    fun nextPhase() {
        viewModelScope.launch { repository.nextPhase() }
    }

    fun snooze() {
        viewModelScope.launch { repository.snooze() }
    }

    fun addFiveMinutes() {
        viewModelScope.launch { repository.addTime(300) }
    }

    fun skipCurrentPhase() {
        viewModelScope.launch { repository.forceFinishTimer(isSkipped = true) }
    }
}
