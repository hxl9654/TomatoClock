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
    val lastSavedTimestamp: Long
)
