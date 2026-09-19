package com.hxlxz.tomatoclock

enum class SoundMode(val value: Int) {
    CONTINUOUS(0),
    SINGLE(1),
    OFF(2);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: CONTINUOUS
    }
}

enum class VibrationMode(val value: Int) {
    CONTINUOUS(0),
    SINGLE(1),
    OFF(2);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: CONTINUOUS
    }
}

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
