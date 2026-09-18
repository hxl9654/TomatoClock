package com.hxlxz.tomatoclock

import kotlinx.coroutines.CoroutineScope
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
    private val timeConfig: TimeConfig,
    private val scope: CoroutineScope
) {

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

    private val _totalCycles = MutableStateFlow(4)
    val totalCycles: StateFlow<Int> = _totalCycles.asStateFlow()

    private var timerJob: Job? = null

    init {
        // 持续监听专注时长设置：计时器空闲时立即更新首页显示
        scope.launch {
            settingsDataStore.focusTimeFlow.collect { focusTimeMin ->
                if (_timerState.value == TimerState.IDLE) {
                    val seconds = focusTimeMin * timeConfig.multiplier
                    _timeRemaining.value = seconds
                    _totalTimeInSeconds.value = seconds
                }
            }
        }

        // 持续监听循环次数设置：始终保持与设置同步
        scope.launch {
            settingsDataStore.cyclesFlow.collect { cycles ->
                _totalCycles.value = cycles
            }
        }
    }

    fun startTimer() {
        if (_timerState.value == TimerState.RUNNING) return

        if (_timerState.value == TimerState.PAUSED) {
            // Resume from where we left off, no need to reset time
            startCountdown()
        } else {
            // IDLE or FINISHED: reset time to full duration for current mode
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
            _timerMode.value = TimerMode.FOCUS
            _currentCycle.value = 1
            val initialTime = getInitialTimeForMode(TimerMode.FOCUS)
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
        } else if (_timerMode.value != TimerMode.FOCUS && autoStartFocus) {
            nextPhase()
        }
    }

    fun nextPhase() {
        scope.launch {
            val cycles = settingsDataStore.cyclesFlow.first()
            _totalCycles.value = cycles

            when (_timerMode.value) {
                TimerMode.FOCUS -> {
                    if (_currentCycle.value >= cycles) {
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
            val initialTime = getInitialTimeForMode(_timerMode.value)
            _timeRemaining.value = initialTime
            _totalTimeInSeconds.value = initialTime
            startCountdown()
        }
    }

    fun snooze() {
        scope.launch {
            val snoozeMinutes = settingsDataStore.snoozeTimeFlow.first()
            val snoozeSeconds = snoozeMinutes * timeConfig.multiplier
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
        return minutes * timeConfig.multiplier
    }
}
