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
import kotlinx.coroutines.flow.first
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

    private val _suppressAlarm = MutableStateFlow(false)
    val suppressAlarm: StateFlow<Boolean> = _suppressAlarm.asStateFlow()

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

    private var timerJob: Job? = null
    
    // Time Anchoring: stores the target absolute time
    private var targetEndTimeMs: Long = 0L
    private var pausedTimeRemainingSeconds: Long = 0L

    init {
        scope.launch {
            // 1. 恢复状态 (Restore state)
            val savedState = settingsDataStore.savedTimerStateFlow.first()
            if (savedState != null) {
                val now = System.currentTimeMillis()
                // 如果超过1小时 (3,600,000 毫秒)，则丢弃状态
                if (now - savedState.lastSavedTimestamp > 3600_000L) {
                    settingsDataStore.clearTimerState()
                } else {
                    _timerMode.value = savedState.mode
                    _currentCycle.value = savedState.currentCycle
                    _totalTimeInSeconds.value = getInitialTimeForMode(savedState.mode)

                    when (savedState.state) {
                        TimerState.RUNNING -> {
                            val elapsed = now - savedState.targetEndTimeWallClock
                            if (elapsed >= 0) {
                                // 已经到期
                                _suppressAlarm.value = elapsed > 10 * 60 * 1000L // 超过10分钟静音
                                _timeRemaining.value = 0
                                _timerState.value = TimerState.FINISHED
                            } else {
                                // 还在运行
                                _suppressAlarm.value = false
                                val remainingSeconds = (-elapsed + 999L) / 1000L
                                startCountdown(remainingSeconds)
                            }
                        }
                        TimerState.PAUSED -> {
                            _suppressAlarm.value = false
                            pausedTimeRemainingSeconds = savedState.pausedTimeRemaining
                            _timeRemaining.value = pausedTimeRemainingSeconds
                            _timerState.value = TimerState.PAUSED
                        }
                        TimerState.FINISHED -> {
                            _suppressAlarm.value = true // 之前就完成的，恢复时不再响铃
                            _timeRemaining.value = 0
                            _timerState.value = TimerState.FINISHED
                        }
                        TimerState.IDLE -> {
                            _suppressAlarm.value = false
                            val initialTime = getInitialTimeForMode(savedState.mode)
                            _timeRemaining.value = initialTime
                        }
                    }
                }
            }

            // 2. 持续监听专注时长设置：计时器空闲时立即更新首页显示
            launch {
                focusTimeMin.collect { focusTime ->
                    if (_timerState.value == TimerState.IDLE) {
                        val seconds = focusTime * timeConfig.multiplier
                        _timeRemaining.value = seconds
                        _totalTimeInSeconds.value = seconds
                    }
                }
            }

            // 3. 持续监听循环次数设置：始终保持与设置同步
            launch {
                settingsDataStore.cyclesFlow.collect { cycles ->
                    _totalCycles.value = cycles
                }
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

    private fun onTimerFinished(isSkipped: Boolean = false) {
        alarmScheduler.cancelAlarm()
        _suppressAlarm.value = isSkipped // 正常完成允许响铃，手动跳过则静音
        _timerState.value = TimerState.FINISHED
        // The AlarmPlayer logic will be handled by TimerService reacting to state change
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

    fun forceFinishTimer(fromAlarm: Boolean = false, isSkipped: Boolean = false) {
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
            onTimerFinished(isSkipped)
        }
    }
    
    suspend fun saveCurrentState() {
        val state = _timerState.value
        val mode = _timerMode.value
        val targetEndTimeWallClock = if (state == TimerState.RUNNING) {
            System.currentTimeMillis() + (targetEndTimeMs - SystemClock.elapsedRealtime())
        } else 0L
        
        val savedState = SavedTimerState(
            state = state,
            mode = mode,
            targetEndTimeWallClock = targetEndTimeWallClock,
            pausedTimeRemaining = pausedTimeRemainingSeconds,
            currentCycle = _currentCycle.value,
            lastSavedTimestamp = System.currentTimeMillis()
        )
        settingsDataStore.saveTimerState(savedState)
    }
}
