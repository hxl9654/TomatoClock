package com.example.tomatoclock

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val dispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + dispatcher)

    private val _timerMode = MutableStateFlow(TimerMode.FOCUS)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _timeRemaining = MutableStateFlow(0L) // In seconds
    val timeRemaining: StateFlow<Long> = _timeRemaining.asStateFlow()

    private val _totalTimeInSeconds = MutableStateFlow(1L)
    val totalTimeInSeconds: StateFlow<Long> = _totalTimeInSeconds.asStateFlow()

    private val _currentCycle = MutableStateFlow(1)
    val currentCycle: StateFlow<Int> = _currentCycle.asStateFlow()

    private var timerJob: Job? = null

    init {
        scope.launch {
            val focusTimeMin = settingsDataStore.focusTimeFlow.first()
            val seconds = focusTimeMin * 60L
            _timeRemaining.value = seconds
            _totalTimeInSeconds.value = seconds
        }
    }

    fun startTimer() {
        if (_timerState.value == TimerState.RUNNING) return
        
        if (_timerState.value == TimerState.IDLE) {
             scope.launch {
                 val initialTime = getInitialTimeForMode(_timerMode.value)
                 _timeRemaining.value = initialTime
                 _totalTimeInSeconds.value = initialTime
                 startCountdown()
             }
        } else if (_timerState.value == TimerState.PAUSED) {
            startCountdown()
        } else if (_timerState.value == TimerState.FINISHED) {
             scope.launch {
                 val initialTime = getInitialTimeForMode(_timerMode.value)
                 _timeRemaining.value = initialTime
                 _totalTimeInSeconds.value = initialTime
                 startCountdown()
             }
        }
    }

    private fun startCountdown() {
        _timerState.value = TimerState.RUNNING
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_timeRemaining.value > 0) {
                delay(1000)
                _timeRemaining.value -= 1
            }
            onTimerFinished()
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.PAUSED
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.IDLE
        scope.launch {
            val initialTime = getInitialTimeForMode(_timerMode.value)
            _timeRemaining.value = initialTime
            _totalTimeInSeconds.value = initialTime
        }
    }

    private suspend fun onTimerFinished() {
        _timerState.value = TimerState.FINISHED
        // Trigger sound/vibration/notification here via Service or Event
        
        val autoStartBreak = settingsDataStore.autoStartBreakFlow.first()
        val autoStartFocus = settingsDataStore.autoStartFocusFlow.first()
        
        if (_timerMode.value == TimerMode.FOCUS && autoStartBreak) {
            nextPhase()
            startTimer()
        } else if (_timerMode.value != TimerMode.FOCUS && autoStartFocus) {
            nextPhase()
            startTimer()
        }
    }

    fun nextPhase() {
        scope.launch {
            val totalCycles = settingsDataStore.cyclesFlow.first()
            
            when (_timerMode.value) {
                TimerMode.FOCUS -> {
                    if (_currentCycle.value >= totalCycles) {
                        _timerMode.value = TimerMode.LONG_BREAK
                    } else {
                        _timerMode.value = TimerMode.SHORT_BREAK
                    }
                }
                TimerMode.SHORT_BREAK -> {
                    _timerMode.value = TimerMode.FOCUS
                    _currentCycle.value += 1
                }
                TimerMode.LONG_BREAK -> {
                    _timerMode.value = TimerMode.FOCUS
                    _currentCycle.value = 1
                }
            }
            _timerState.value = TimerState.IDLE
            val initialTime = getInitialTimeForMode(_timerMode.value)
            _timeRemaining.value = initialTime
            _totalTimeInSeconds.value = initialTime
        }
    }

    fun snooze() {
        scope.launch {
            val snoozeMinutes = settingsDataStore.snoozeTimeFlow.first()
            val snoozeSeconds = snoozeMinutes * 60L
            _timeRemaining.value = snoozeSeconds
            _totalTimeInSeconds.value = snoozeSeconds
            startCountdown()
        }
    }

    private suspend fun getInitialTimeForMode(mode: TimerMode): Long {
        val minutes = when (mode) {
            TimerMode.FOCUS -> settingsDataStore.focusTimeFlow.first()
            TimerMode.SHORT_BREAK -> settingsDataStore.shortBreakTimeFlow.first()
            TimerMode.LONG_BREAK -> settingsDataStore.longBreakTimeFlow.first()
        }
        return minutes * 60L
    }
}
