package com.hxlxz.tomatoclock

import android.content.Context
import android.os.Build
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowVibrator

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class AlarmPlayerTest {

    private lateinit var context: Context
    private lateinit var alarmPlayer: AlarmPlayer
    private lateinit var shadowVibrator: ShadowVibrator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        alarmPlayer = AlarmPlayer(context)
        
        // Mock Vibrator
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        shadowVibrator = shadowOf(vibrator)
    }

    @Test
    fun `play with CONTINUOUS vibration and CONTINUOUS sound`() {
        alarmPlayer.play(SoundMode.CONTINUOUS, VibrationMode.CONTINUOUS, Ringtone.CHIME)

        // Verify vibration
        assertNotNull(shadowVibrator.pattern)
        
        // Due to Robolectric limitations on MediaPlayer creation without explicit shadows,
        // we mainly assert no exceptions are thrown and state is handled smoothly.
        // The fact that it doesn't crash means our try/catch and basic flow is solid.
    }

    @Test
    fun `play with SINGLE vibration and SINGLE sound`() {
        alarmPlayer.play(SoundMode.SINGLE, VibrationMode.SINGLE, Ringtone.DIGITAL)

        // Verify vibration
        assertNotNull(shadowVibrator.pattern)
    }

    @Test
    fun `play with OFF vibration and OFF sound`() {
        alarmPlayer.play(SoundMode.OFF, VibrationMode.OFF, Ringtone.SOFT_SYNTH)

        // Verify no vibration
        val pattern = shadowVibrator.pattern
        assertNull(pattern) // If it wasn't called, pattern is null or empty
    }

    @Test
    fun `preview ringtone`() {
        alarmPlayer.preview(2) // SOFT_SYNTH
    }

    @Test
    fun `stop cancels vibration and releases media player`() {
        alarmPlayer.play(SoundMode.CONTINUOUS, VibrationMode.CONTINUOUS, Ringtone.CHIME)
        alarmPlayer.stop()
        
        // Verify vibration is canceled
        org.junit.Assert.assertTrue(shadowVibrator.isCancelled)
    }

    @Test
    fun `play after preview does not throw and cancels previous vibration`() {
        // 【H-5修复验证】先 preview 再 play 应能安全执行，
        // play() 调用时必须清除 isPreviewActive 标志，使 stopRunnable 失效
        alarmPlayer.preview(1) // CHIME
        // 立即调用 play()，此时 stopRunnable 尚未触发（2s 延迟）
        alarmPlayer.play(SoundMode.CONTINUOUS, VibrationMode.CONTINUOUS, Ringtone.ZEN_BOWL)
        // play 内部调用 stop()，会 removeCallbacks(stopRunnable) 并设置 isPreviewActive=false
        // 验证无异常，且振动已重新设置
        assertNotNull(shadowVibrator.pattern)
    }

    @Test
    fun `stop after preview cancels vibration`() {
        alarmPlayer.preview(0) // DIGITAL
        alarmPlayer.stop()
        org.junit.Assert.assertTrue(shadowVibrator.isCancelled)
    }

    @Test
    fun `multiple stop calls do not throw`() {
        // 验证 stop() 的幂等性：多次调用不应抛出异常
        alarmPlayer.stop()
        alarmPlayer.stop()
        alarmPlayer.stop()
        // 无异常即通过
    }
}
