package com.hxlxz.tomatoclock

import androidx.annotation.Keep

// 【SEC-1修复】所有在 DataStore 中以字符串序列化的枚举和数据类必须加 @Keep，
// 防止开启 R8/ProGuard 后字段名被混淆，导致 valueOf()/DataStore 解析崩溃。

@Keep
enum class TimerMode {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

@Keep
enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED
}

@Keep
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
