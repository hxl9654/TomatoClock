package com.hxlxz.tomatoclock

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class TimerRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val timeConfig: TimeConfig,
    private val scope: CoroutineScope,
    private val alarmScheduler: AlarmScheduler
) {

    private val _timerMode = MutableStateFlow(TimerMode.FOCUS)
    val timerMode: StateFlow<TimerMode> = _timerMode.asStateFlow()

    /**
     * 是否抑制计时结束提醒（铃声/振动）。
     *
     * 设置为 true 的场景：
     * 1. 用户手动跳过当前阶段（[forceFinishTimer] 传入 isSkipped=true）
     * 2. 从后台恢复时，计时结束超过 10 分钟（用户早已知晓，无需补发提醒）
     * 3. 从后台恢复时，当前状态已是 FINISHED（之前已触发过提醒）
     *
     * 由 [TimerService.handleStateSideEffects] 消费。
     */
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
    @Volatile
    private var targetEndTimeMs = 0L
    @Volatile
    private var pausedTimeRemainingSeconds = 0L
    private val stateMutex = Mutex()
    private val isInitialized = CompletableDeferred<Unit>()

    init {
        scope.launch {
            // ── Step 1: 恢复持久化状态 ────────────────────────────────────────────
            // 【C-2修复】必须先完成状态恢复，再启动设置监听协程，避免竞态条件：
            // focusTimeMin 使用 SharingStarted.Eagerly 会立即发射默认值，
            // 若监听器先于恢复逻辑运行，可能将恢复的时间覆盖回默认专注时间。
            val savedState = settingsDataStore.savedTimerStateFlow.first()
            stateMutex.withLock {
                if (savedState != null) {
                    val now = System.currentTimeMillis()
                    // 如果超过1小时 (3,600,000 毫秒)，则丢弃状态
                    if (now - savedState.lastSavedTimestamp > 3600_000L) {
                        settingsDataStore.clearTimerState()
                    } else {
                        _timerMode.value = savedState.mode
                        _currentCycle.value = savedState.currentCycle
                        // 【H-4修复】优先使用保存的 totalTimeInSeconds，
                        // 以正确恢复用户通过 addTime() 延长后的进度圆弧。
                        _totalTimeInSeconds.value = if (savedState.totalTimeInSeconds > 0L) {
                            savedState.totalTimeInSeconds
                        } else {
                            getInitialTimeForMode(savedState.mode) // 旧数据兼容 fallback
                        }

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
            }
            isInitialized.complete(Unit)

            // ── Step 2: 状态恢复完成后，再启动响应式设置监听 ────────────────────
            // 保证监听器不会干扰上方的恢复逻辑。

            // 持续监听专注时长设置：计时器空闲时立即更新首页显示
            launch {
                focusTimeMin.collect { focusTime ->
                    if (_timerState.value == TimerState.IDLE) {
                        val seconds = focusTime * timeConfig.multiplier
                        _timeRemaining.value = seconds
                        _totalTimeInSeconds.value = seconds
                    }
                }
            }

            // 持续监听循环次数设置：始终保持与设置同步
            launch {
                settingsDataStore.cyclesFlow.collect { cycles ->
                    _totalCycles.value = cycles
                }
            }
        }
    }

    suspend fun startTimer() {
        isInitialized.await()
        stateMutex.withLock {
            if (_timerState.value == TimerState.RUNNING) return

            if (_timerState.value == TimerState.PAUSED) {
                // Resume from where we left off
                startCountdown(pausedTimeRemainingSeconds)
            } else {
                // IDLE or FINISHED: reset time to full duration for current mode
                // [Fix-CS-3] 消除隐式依赖：显式重置 pausedTimeRemainingSeconds，
                // 避免在 IDLE/FINISHED 启动后残留旧的暂停时间。
                pausedTimeRemainingSeconds = 0L
                val initialTime = getInitialTimeForMode(_timerMode.value)
                _totalTimeInSeconds.value = initialTime
                startCountdown(initialTime)
            }
            persistStateAsync()
        }
    }

    private fun startCountdown(durationSeconds: Long) {
        _timerState.value = TimerState.RUNNING
        targetEndTimeMs = SystemClock.elapsedRealtime() + (durationSeconds * 1000L)
        _timeRemaining.value = durationSeconds

        alarmScheduler.scheduleAlarm(targetEndTimeMs)

        timerJob?.cancel()
        timerJob = scope.launch {
            // [Fix-P-5 心跳] 每 300 tick（约 60 秒）保存一次状态，
            // 防止应用崩溃时丢失 targetEndTime 的最新值。
            var ticksSinceLastSave = 0
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val remainingMs = targetEndTimeMs - now
                if (remainingMs <= 0) {
                    _timeRemaining.value = 0
                    // 【H-3修复】此处无需调用 cancelAlarm()，
                    // onTimerFinished() 内部已统一处理取消逻辑，避免双重取消。
                    onTimerFinished()
                    break
                }
                // 使用 ceiling 确保不到最后一秒不显示 0
                _timeRemaining.value = (remainingMs + 999L) / 1000L
                ticksSinceLastSave++
                if (ticksSinceLastSave >= 300) { // 300 × 200ms = 60 秒
                    ticksSinceLastSave = 0
                    persistStateAsync()
                }
                delay(200.milliseconds) // update UI frequently enough, no drift because of absolute time anchor
            }
        }
    }

    suspend fun pauseTimer() {
        isInitialized.await()
        stateMutex.withLock {
            timerJob?.cancel()
            alarmScheduler.cancelAlarm()
            pausedTimeRemainingSeconds = _timeRemaining.value
            _timerState.value = TimerState.PAUSED
            persistStateAsync()
        }
    }

    suspend fun stopTimer() {
        isInitialized.await()
        stateMutex.withLock {
            timerJob?.cancel()
            alarmScheduler.cancelAlarm()
            _timerState.value = TimerState.IDLE
            _timerMode.value = TimerMode.FOCUS
            _currentCycle.value = 1

            val initialTime = getInitialTimeForMode(TimerMode.FOCUS)
            _timeRemaining.value = initialTime
            _totalTimeInSeconds.value = initialTime
            // IDLE 状态无需恢复，直接清除持久化数据
            clearStateAsync()
        }
    }

    private fun onTimerFinished(isSkipped: Boolean = false) {
        alarmScheduler.cancelAlarm()
        _suppressAlarm.value = isSkipped // 正常完成允许响铃，手动跳过则静音
        _timerState.value = TimerState.FINISHED
        // The AlarmPlayer logic will be handled by TimerService reacting to state change
        // [Fix-P-5] 在此处立即持久化，覆盖「countdown loop 检测到时间到」的路径（路径A）。
        // 路径B（Alarm 触发 forceFinishTimer）亦会调用此函数，两者均得到正确保存。
        persistStateAsync()
    }

    /**
     * 进入下一个番茄阶段，并自动启动倒计时。
     *
     * 状态机转换规则：
     * - FOCUS (cycle < totalCycles)  -> SHORT_BREAK
     * - FOCUS (cycle >= totalCycles) -> LONG_BREAK，cycle 不变（由 LONG_BREAK->FOCUS 重置为 1）
     * - SHORT_BREAK -> FOCUS，cycle + 1（上限裁剪至 totalCycles）
     * - LONG_BREAK  -> FOCUS，cycle = 1
     *
     * [CR-1修复] SHORT_BREAK->FOCUS 时 currentCycle 自增须裁剪到 totalCycles，
     * 避免用户在 Settings 中缩小 totalCycles 后 currentCycle 持续越界，
     * 导致 FOCUS -> LONG_BREAK 的条件永远无法满足。
     */
    suspend fun nextPhase() {
        isInitialized.await()
        stateMutex.withLock {
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
                    // [CR-1修复] coerceAtMost 防止 totalCycles 被用户缩小时越界
                    _currentCycle.value = (_currentCycle.value + 1).coerceAtMost(_totalCycles.value)
                }

                TimerMode.LONG_BREAK -> {
                    _timerMode.value = TimerMode.FOCUS
                    _currentCycle.value = 1
                }
            }
            val initialTime = getInitialTimeForMode(_timerMode.value)
            _totalTimeInSeconds.value = initialTime
            startCountdown(initialTime)
            persistStateAsync()
        }
    }

    /**
     * 推迟提醒：在阶段结束（FINISHED）后，再计一段缓冲倒计时而不进入下一阶段。
     *
     * 注意：此函数可在任意状态调用，调用后无论当前状态如何都会启动新的倒计时。
     * 业务上通常仅在 FINISHED 状态由 UI 触发。
     */
    suspend fun snooze() {
        isInitialized.await()
        stateMutex.withLock {
            val snoozeSeconds = snoozeTimeMin.value * timeConfig.multiplier
            _totalTimeInSeconds.value = snoozeSeconds
            startCountdown(snoozeSeconds)
            persistStateAsync()
        }
    }

    /**
     * 根据当前模式返回初始倒计时秒数（含 TimeConfig.multiplier 缩放）。
     *
     * @param mode 目标计时模式
     * @return 初始倒计时秒数（= 分钟数 × multiplier）
     */
    private fun getInitialTimeForMode(mode: TimerMode): Long {
        val minutes = when (mode) {
            TimerMode.FOCUS -> focusTimeMin.value
            TimerMode.SHORT_BREAK -> shortBreakTimeMin.value
            TimerMode.LONG_BREAK -> longBreakTimeMin.value
        }
        return minutes * timeConfig.multiplier
    }

    /**
     * 在当前倒计时基础上追加时间（快捷"加5分钟"功能）。
     *
     * - RUNNING：延长目标结束时间，重新调度 Alarm，并立即刷新 UI 显示。
     * - PAUSED：延长 pausedTimeRemainingSeconds，恢复后的倒计时将包含追加时间。
     * - IDLE / FINISHED：静默忽略，不做任何操作。
     *
     * @param seconds 追加的秒数，应为正数。
     */
    suspend fun addTime(seconds: Long) {
        isInitialized.await()
        stateMutex.withLock {
            val maxAllowedSeconds = 24 * 3600L
            val validSeconds = if (_totalTimeInSeconds.value + seconds > maxAllowedSeconds) {
                maxAllowedSeconds - _totalTimeInSeconds.value
            } else {
                seconds
            }
            if (validSeconds <= 0) return@withLock

            if (_timerState.value == TimerState.RUNNING) {
                targetEndTimeMs += (validSeconds * 1000L)
                _totalTimeInSeconds.value += validSeconds
                alarmScheduler.scheduleAlarm(targetEndTimeMs)

                // Immediately update remaining time for UI responsiveness
                val now = SystemClock.elapsedRealtime()
                val remainingMs = targetEndTimeMs - now
                if (remainingMs > 0) {
                    _timeRemaining.value = (remainingMs + 999L) / 1000L
                }
                persistStateAsync()
            } else if (_timerState.value == TimerState.PAUSED) {
                pausedTimeRemainingSeconds += validSeconds
                _totalTimeInSeconds.value += validSeconds
                _timeRemaining.value = pausedTimeRemainingSeconds
                persistStateAsync()
            }
            // IDLE / FINISHED: silently ignored
        }
    }

    suspend fun forceFinishTimer(fromAlarm: Boolean = false, isSkipped: Boolean = false) {
        isInitialized.await()
        stateMutex.withLock {
            if (_timerState.value == TimerState.RUNNING) {
                if (fromAlarm) {
                    // If this is triggered by the alarm, ensure it's not a stale alarm
                    // caused by a recent addTime operation pushing the target end time further.
                    val now = SystemClock.elapsedRealtime()
                    if (now < targetEndTimeMs - 2000L) {
                        return@withLock
                    }
                }
                timerJob?.cancel()
                alarmScheduler.cancelAlarm()
                _timeRemaining.value = 0
                onTimerFinished(isSkipped)
                // [Fix-P-5] persistStateAsync() 已在 onTimerFinished() 内调用，无需重复。
            }
        }
    }

    /**
     * 将当前计时器状态持久化到 DataStore。
     *
     * - RUNNING：保存 targetEndTimeWallClock（wall-clock 绝对时间）以便恢复后重建倒计时。
     * - PAUSED：保存 pausedTimeRemaining 以便恢复后继续。
     * - IDLE / FINISHED：字段仍被记录，但恢复时会根据状态做适当处理。
     *
     * 注意：此函数是 `suspend` 的；非挂起调用场景请使用 [persistStateAsync]。
     */
    suspend fun saveCurrentState() {
        val savedState = stateMutex.withLock {
            val state = _timerState.value
            val mode = _timerMode.value
            val targetEndTimeWallClock = if (state == TimerState.RUNNING) {
                System.currentTimeMillis() + (targetEndTimeMs - SystemClock.elapsedRealtime())
            } else 0L

            SavedTimerState(
                state = state,
                mode = mode,
                targetEndTimeWallClock = targetEndTimeWallClock,
                pausedTimeRemaining = pausedTimeRemainingSeconds,
                currentCycle = _currentCycle.value,
                lastSavedTimestamp = System.currentTimeMillis(),
                totalTimeInSeconds = _totalTimeInSeconds.value // 【H-4修复】保存当前总时长
            )
        }
        settingsDataStore.saveTimerState(savedState)
    }

    /**
     * 非阻塞地将当前状态持久化（fire-and-forget）。
     *
     * 在各状态变更方法（[startTimer]、[pauseTimer] 等）末尾调用，
     * 实现 Write-Through 持久化策略，确保状态在操作完成后立即落盘。
     */
    private fun persistStateAsync() {
        scope.launch {
            // [A-3修复] 捕获 DataStore 可能抛出的 IOException（磁盘满/加密错误等）。
            // [P2-2修复] 使用 IOException 而非 Exception 基类，确保 CancellationException
            // 能正常传播，不破坏协程结构化并发的取消机制。
            try {
                saveCurrentState()
            } catch (e: java.io.IOException) {
                Log.e("TimerRepository", "Failed to persist timer state to DataStore", e)
            }
        }
    }

    /**
     * 非阻塞地清除持久化的计时器状态（fire-and-forget）。
     *
     * 在 [stopTimer] 后调用：IDLE 状态无需恢复，直接清除 DataStore 中的快照，
     * 避免下次启动时错误恢复到已停止的状态。
     */
    private fun clearStateAsync() {
        scope.launch {
            // [P2-3修复] 使用 IOException 而非 Exception 基类，确保 CancellationException
            // 能正常传播，不破坏协程结构化并发的取消机制。
            try {
                settingsDataStore.clearTimerState()
            } catch (e: java.io.IOException) {
                Log.e("TimerRepository", "Failed to clear timer state from DataStore", e)
            }
        }
    }

    /**
     * 等待 TimerRepository 完成 DataStore 状态恢复和闹钟重调度。
     *
     * 由 [BootCompletedReceiver] 在设备重启后显式调用，确保：
     * 1. 完成 DataStore 状态读取，
     * 2. 触发 [startCountdown] 以重建因重启丢失的精确闹钟（如重启前处于 RUNNING 状态）。
     *
     * 此方法为幂等操作：若 Repository 已初始化完成，将立即返回。
     */
    suspend fun ensureInitialized() {
        isInitialized.await()
        Log.d("TimerRepository", "ensureInitialized: DataStore state restored and alarms rescheduled.")
    }

    @androidx.annotation.VisibleForTesting
    fun destroyForTesting() {
        timerJob?.cancel()
        alarmScheduler.cancelAlarm()
        scope.coroutineContext[Job]?.cancel()
    }
}
