package com.hxlxz.tomatoclock

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // 0: Sound+Vib, 1: Sound, 2: Vib, 3: SingleSound, 4: Silent
    fun play(alertMode: Int, ringtoneIndex: Int) {
        stop() // Ensure previous is stopped

        if (alertMode == 4) return // Silent

        // Setup Vibrator
        if (alertMode == 0 || alertMode == 2) {
            val pattern = longArrayOf(0, 500, 500) // wait 0, vibrate 500, sleep 500
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat at index 0
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }

        // Setup MediaPlayer
        if (alertMode == 0 || alertMode == 1 || alertMode == 3) {
            val audioRes = when (ringtoneIndex) {
                0 -> R.raw.digital_beep
                1 -> R.raw.chime
                2 -> R.raw.soft_synth
                else -> R.raw.chime
            }

            try {
                mediaPlayer = MediaPlayer.create(context, audioRes)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = (alertMode != 3) // SingleSound does not loop
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
