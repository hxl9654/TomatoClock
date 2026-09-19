package com.hxlxz.tomatoclock

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
    @SuppressLint("ObsoleteSdkInt")
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 【H-5修复】标记是否处于预览播放状态。
     * 当 [play] 被调用时立即置 false，使 [stopRunnable] 失效，
     * 避免先调用 [preview] 后再调用 [play]，
     * 残留的 2 秒延迟误停止正在响铃的报警声。
     */
    private var isPreviewActive = false

    private val stopRunnable = Runnable {
        // 【H-5修复】只有仍处于预览状态时才停止
        if (isPreviewActive) {
            isPreviewActive = false
            stop()
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    fun play(soundMode: SoundMode, vibrationMode: VibrationMode, ringtoneIndex: Ringtone) {
        // 【H-5修复】调用 play() 时立即标记预览状态为 false，
        // 使任何残留的 stopRunnable 失效。
        isPreviewActive = false
        stop() // Ensure previous is stopped

        // Setup Vibrator
        if (vibrationMode != VibrationMode.OFF) {
            val pattern = if (vibrationMode == VibrationMode.CONTINUOUS) {
                longArrayOf(0, 500, 500)
            } else {
                longArrayOf(0, 1000)
            }
            val repeatIndex = if (vibrationMode == VibrationMode.CONTINUOUS) 0 else -1

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeatIndex))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeatIndex)
            }
        }

        // Setup MediaPlayer
        if (soundMode != SoundMode.OFF) {
            val audioRes = getAudioRes(ringtoneIndex.value)

            try {
                mediaPlayer = MediaPlayer.create(context, audioRes)
                if (mediaPlayer == null) {
                    Log.w("AlarmPlayer", "MediaPlayer.create() returned null for resource $audioRes. Falling back to default alarm ringtone.")
                    val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                    mediaPlayer = MediaPlayer.create(context, defaultUri)
                }
                
                mediaPlayer?.apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    if (soundMode == SoundMode.SINGLE) {
                        isLooping = false
                        setOnCompletionListener { 
                            try {
                                it.release()
                                if (mediaPlayer == it) {
                                    mediaPlayer = null
                                }
                            } catch (e: Exception) {
                                Log.e("AlarmPlayer", "Error releasing mediaPlayer: ${e.message}", e)
                            }
                        }
                    } else {
                        isLooping = true
                    }
                    start()
                }
            } catch (e: Exception) {
                Log.e("AlarmPlayer", "Error: ${e.message}", e)
            }
        }
    }

    fun preview(ringtoneIndex: Int) {
        stop()
        isPreviewActive = true // 【H-5修复】标记预览状态
        val audioRes = getAudioRes(ringtoneIndex)
        try {
            mediaPlayer = MediaPlayer.create(context, audioRes)
            if (mediaPlayer == null) {
                Log.w("AlarmPlayer", "MediaPlayer.create() returned null for resource $audioRes. Preview will use default alarm.")
                val defaultUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer.create(context, defaultUri)
            }
            
            mediaPlayer?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                // 【L-2修复】预览时循环播放，并在 2 秒后由 stopRunnable 停止。
                // 这是有意设计，确保短于 2 秒的音效也能被听到。
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
            Log.e("AlarmPlayer", "Error releasing MediaPlayer on stop: ${e.message}", e)
        }

        try {
            vibrator.cancel()
        } catch (e: Exception) {
            Log.e("AlarmPlayer", "Error cancelling vibrator on stop: ${e.message}", e)
        }
    }
}
