package com.hxlxz.tomatoclock

enum class AlertMode(val value: Int) {
    SOUND_AND_VIBRATE(0),
    SOUND_ONLY(1),
    VIBRATE_ONLY(2),
    SINGLE_SOUND(3),
    SILENT(4);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: SOUND_AND_VIBRATE
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
