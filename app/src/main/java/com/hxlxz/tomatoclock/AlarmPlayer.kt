package com.hxlxz.tomatoclock

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
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
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stop() }

    // 0: Sound+Vib, 1: Sound, 2: Vib, 3: SingleSound, 4: Silent
    fun play(alertMode: AlertMode, ringtoneIndex: Ringtone) {
        stop() // Ensure previous is stopped

        if (alertMode == AlertMode.SILENT) return

        // Setup Vibrator
        if (alertMode == AlertMode.SOUND_AND_VIBRATE || alertMode == AlertMode.VIBRATE_ONLY) {
            val pattern = longArrayOf(0, 500, 500) // wait 0, vibrate 500, sleep 500
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat at index 0
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }

        // Setup MediaPlayer
        if (alertMode == AlertMode.SOUND_AND_VIBRATE || alertMode == AlertMode.SOUND_ONLY || alertMode == AlertMode.SINGLE_SOUND) {
            val audioRes = getAudioRes(ringtoneIndex.value)

            try {
                mediaPlayer = MediaPlayer.create(context, audioRes)?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true // Let it loop for all modes, we'll stop it manually
                    start()
                }
                if (alertMode == AlertMode.SINGLE_SOUND) {
                    handler.postDelayed(stopRunnable, 2000L)
                }
            } catch (e: Exception) {
                Log.e("AlarmPlayer", "Error: ${e.message}", e)
            }
        }
    }

    fun preview(ringtoneIndex: Int) {
        stop()
        val audioRes = getAudioRes(ringtoneIndex)
        try {
            mediaPlayer = MediaPlayer.create(context, audioRes)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                start()
            }
            handler.postDelayed(stopRunnable, 2000L)
        } catch (e: Exception) {
                Log.e("AlarmPlayer", "Error: ${e.message}", e)
            }
    }

    private fun getAudioRes(ringtoneIndex: Int): Int {
        return when (ringtoneIndex) {
            0 -> R.raw.digital_beep
            1 -> R.raw.chime
            2 -> R.raw.soft_synth
            3 -> R.raw.zen_bowl
            4 -> R.raw.nature_wood
            else -> R.raw.chime
        }
    }

    fun stop() {
        handler.removeCallbacks(stopRunnable)
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
                Log.e("AlarmPlayer", "Error: ${e.message}", e)
            }

        try {
            vibrator.cancel()
        } catch (e: Exception) {
                Log.e("AlarmPlayer", "Error: ${e.message}", e)
            }
    }
}
