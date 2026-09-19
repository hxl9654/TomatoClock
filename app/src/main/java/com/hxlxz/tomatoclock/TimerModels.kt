package com.hxlxz.tomatoclock

enum class TimerMode {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

data class SavedTimerState(
    val state: TimerState,
    val mode: TimerMode,
    val targetEndTimeWallClock: Long,
    val pausedTimeRemaining: Long,
    val currentCycle: Int,
    val lastSavedTimestamp: Long,
    /** 【H-4修复】保存用户当前设定的总时长（含 addTime 延长后），为 0 表示旧数据兼容。 */
    val totalTimeInSeconds: Long = 0L
)
