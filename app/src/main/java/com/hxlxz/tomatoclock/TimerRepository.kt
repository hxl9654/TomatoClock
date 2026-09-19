package com.hxlxz.tomatoclock

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val timeConfig: TimeConfig,
    private val scope: CoroutineScope,
    private val alarmScheduler: AlarmScheduler
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

    // 缓存所有设置配置，避免在使用时产生挂起 (blocking IO equivalent for DataStore)
    private val focusTimeMin = settingsDataStore.focusTimeFlow.stateIn(scope, SharingStarted.Eagerly, 25)
    private val shortBreakTimeMin = settingsDataStore.shortBreakTimeFlow.stateIn(scope, SharingStarted.Eagerly, 5)
    private val longBreakTimeMin = settingsDataStore.longBreakTimeFlow.stateIn(scope, SharingStarted.Eagerly, 15)
    private val snoozeTimeMin = settingsDataStore.snoozeTimeFlow.stateIn(scope, SharingStarted.Eagerly, 5)
    private val autoStartBreak = settingsDataStore.autoStartBreakFlow.stateIn(scope, SharingStarted.Eagerly, false)
    private val autoStartFocus = settingsDataStore.autoStartFocusFlow.stateIn(scope, SharingStarted.Eagerly, false)

    private var timerJob: Job? = null
    
    // Time Anchoring: stores the target absolute time
    private var targetEndTimeMs: Long = 0L
    private var pausedTimeRemainingSeconds: Long = 0L

    init {
        // 持续监听专注时长设置：计时器空闲时立即更新首页显示
        scope.launch {
            focusTimeMin.collect { focusTime ->
                if (_timerState.value == TimerState.IDLE) {
                    val seconds = focusTime * timeConfig.multiplier
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
            // Resume from where we left off
            startCountdown(pausedTimeRemainingSeconds)
        } else {
            // IDLE or FINISHED: reset time to full duration for current mode
            val initialTime = getInitialTimeForMode(_timerMode.value)
            _totalTimeInSeconds.value = initialTime
            startCountdown(initialTime)
        }
    }

    private fun startCountdown(durationSeconds: Long) {
        _timerState.value = TimerState.RUNNING
        targetEndTimeMs = SystemClock.elapsedRealtime() + (durationSeconds * 1000L)
        _timeRemaining.value = durationSeconds
        
        alarmScheduler.scheduleAlarm(targetEndTimeMs)
        
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val remainingMs = targetEndTimeMs - now
                if (remainingMs <= 0) {
                    _timeRemaining.value = 0
                    alarmScheduler.cancelAlarm()
                    onTimerFinished()
                    break
                }
                // 使用 ceiling 确保不到最后一秒不显示 0
                _timeRemaining.value = (remainingMs + 999L) / 1000L 
                delay(200) // update UI frequently enough, no drift because of absolute time anchor
            }
        }
    }

    fun pauseTimer() {
        timerJob?.cancel()
        alarmScheduler.cancelAlarm()
        pausedTimeRemainingSeconds = _timeRemaining.value
        _timerState.value = TimerState.PAUSED
    }

    fun stopTimer() {
        timerJob?.cancel()
        alarmScheduler.cancelAlarm()
        _timerState.value = TimerState.IDLE
        _timerMode.value = TimerMode.FOCUS
        _currentCycle.value = 1
        
        val initialTime = getInitialTimeForMode(TimerMode.FOCUS)
        _timeRemaining.value = initialTime
        _totalTimeInSeconds.value = initialTime
    }

    private fun onTimerFinished() {
        alarmScheduler.cancelAlarm()
        _timerState.value = TimerState.FINISHED
        // The AlarmPlayer logic will be handled by TimerService reacting to state change
        
        val autoBreak = autoStartBreak.value
        val autoFocus = autoStartFocus.value
        
        if (_timerMode.value == TimerMode.FOCUS && autoBreak) {
            nextPhase()
        } else if (_timerMode.value != TimerMode.FOCUS && autoFocus) {
            nextPhase()
        }
    }

    fun nextPhase() {
        when (_timerMode.value) {
            TimerMode.FOCUS -> {
                if (_currentCycle.value >= _totalCycles.value) {
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
        _totalTimeInSeconds.value = initialTime
        startCountdown(initialTime)
    }

    fun snooze() {
        val snoozeSeconds = snoozeTimeMin.value * timeConfig.multiplier
        _totalTimeInSeconds.value = snoozeSeconds
        startCountdown(snoozeSeconds)
    }

    private fun getInitialTimeForMode(mode: TimerMode): Long {
        val minutes = when (mode) {
            TimerMode.FOCUS -> focusTimeMin.value
            TimerMode.SHORT_BREAK -> shortBreakTimeMin.value
            TimerMode.LONG_BREAK -> longBreakTimeMin.value
        }
        return minutes * timeConfig.multiplier
    }

    fun addTime(seconds: Long) {
        if (_timerState.value == TimerState.RUNNING) {
            targetEndTimeMs += (seconds * 1000L)
            _totalTimeInSeconds.value += seconds
            alarmScheduler.scheduleAlarm(targetEndTimeMs)
            
            // Immediately update remaining time for UI responsiveness
            val now = SystemClock.elapsedRealtime()
            val remainingMs = targetEndTimeMs - now
            if (remainingMs > 0) {
                _timeRemaining.value = (remainingMs + 999L) / 1000L
            }
        } else if (_timerState.value == TimerState.PAUSED) {
            pausedTimeRemainingSeconds += seconds
            _totalTimeInSeconds.value += seconds
            _timeRemaining.value = pausedTimeRemainingSeconds
        }
    }

    fun forceFinishTimer(fromAlarm: Boolean = false) {
        if (_timerState.value == TimerState.RUNNING) {
            if (fromAlarm) {
                // If this is triggered by the alarm, ensure it's not a stale alarm
                // caused by a recent addTime operation pushing the target end time further.
                val now = SystemClock.elapsedRealtime()
                if (now < targetEndTimeMs - 2000L) {
                    return
                }
            }
            timerJob?.cancel()
            alarmScheduler.cancelAlarm()
            _timeRemaining.value = 0
            onTimerFinished()
        }
    }
}
