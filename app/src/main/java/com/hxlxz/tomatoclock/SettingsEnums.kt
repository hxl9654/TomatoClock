package com.hxlxz.tomatoclock

import androidx.annotation.Keep

// 【SEC-1修复】枚举的整数值存储在 DataStore，fromInt() 在读取时通过 entries.find{} 匹配。
// 加 @Keep 确保枚举名称和 value 字段在 R8 混淆后不被重命名。

@Keep
enum class SoundMode(val value: Int) {
    CONTINUOUS(0),
    SINGLE(1),
    OFF(2);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: CONTINUOUS
    }
}

@Keep
enum class VibrationMode(val value: Int) {
    CONTINUOUS(0),
    SINGLE(1),
    OFF(2);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: CONTINUOUS
    }
}

@Keep
enum class Ringtone(val value: Int) {
    DIGITAL(0),
    CHIME(1),
    SOFT_SYNTH(2),
    ZEN_BOWL(3),
    NATURE_WOOD(4);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: CHIME
    }
}
