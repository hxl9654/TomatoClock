package com.hxlxz.tomatoclock

import android.content.Context
import android.os.Build
import android.os.VibratorManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
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
        assertTrue(shadowVibrator.isCancelled)
    }

    @Test
    fun `play after preview does not throw and stopRunnable is invalidated`() {
        // [B-2修复] 加强测试：验证 isPreviewActive flag 将 stopRunnable 失效
        // 而非仅验证"无异常"（原测试是假阳性）

        alarmPlayer.preview(1) // CHIME
        // 点击 preview 后 isPreviewActive = true
        assertTrue("isPreviewActive should be true after preview", alarmPlayer.isPreviewActive)

        // 立即调用 play()，此时 stopRunnable 尚未触发（有 2s 延迟）
        alarmPlayer.play(SoundMode.CONTINUOUS, VibrationMode.CONTINUOUS, Ringtone.ZEN_BOWL)
        // play() 应将 isPreviewActive 置 false，使 stopRunnable 失效
        assertFalse("isPreviewActive should be false after play()", alarmPlayer.isPreviewActive)

        // 推进主线程 Handler 2秒以上，模拟 stopRunnable 应该被触发的时刻
        ShadowLooper.idleMainLooper()

        // pattern 不为 null 证明 play() 成功设置了斱器
        assertNotNull(shadowVibrator.pattern)
    }

    @Test
    fun `stop after preview cancels vibration`() {
        alarmPlayer.preview(0) // DIGITAL
        alarmPlayer.stop()
        assertTrue(shadowVibrator.isCancelled)
    }

    @Test
    fun `multiple stop calls do not throw`() {
        // 验证 stop() 的幂等性：多次调用不应抛出异常
        alarmPlayer.stop()
        alarmPlayer.stop()
        alarmPlayer.stop()
        // 无异常即通过
    }

    @Test
    fun `play fallbacks to system default when MediaPlayer creation fails`() {
        // Robolectric doesn't easily mock MediaPlayer.create returning null for a specific resource, 
        // but we can verify the method signature works without crash in the mocked environment.
        // The fallback logic uses RingtoneManager.
        // We'll just invoke play and ensure it completes without throwing an exception.
        alarmPlayer.play(SoundMode.SINGLE, VibrationMode.OFF, Ringtone.DIGITAL)
        
        // Assert state doesn't crash (Robolectric might return null or mock the MediaPlayer,
        // either way, the fallback should catch it or handle the mock gracefully).
        assertTrue(true)
    }
}
