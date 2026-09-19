package com.hxlxz.tomatoclock

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var repository: TimerRepository

    @Inject
    lateinit var alarmPlayer: AlarmPlayer
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isForeground = false

    companion object {
        const val CHANNEL_ID = "tomato-clock_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Handle Notification updates
        combine(
            repository.timerState,
            repository.timeRemaining,
            repository.timerMode,
            settingsDataStore.wakeScreenFlow
        ) { state, time, mode, wakeScreen ->
            updateNotification(state, time, mode, wakeScreen)
        }.launchIn(serviceScope)

        // Handle State Side Effects (Alarms)
        repository.timerState.onEach { state ->
            handleStateSideEffects(state)
        }.launchIn(serviceScope)
    }

    private suspend fun handleStateSideEffects(state: TimerState) {
        when (state) {
            TimerState.RUNNING -> {
                alarmPlayer.stop()
            }
            TimerState.FINISHED -> {
                // [Fix-CS-2] 在进入 withContext(IO) 之前捕获 suppressAlarm 值到局部变量，
                // 避免挂起期间值被修改导致的竞态窗口（虽然概率极低，但属防御性编程规范）。
                val suppress = repository.suppressAlarm.value
                if (!suppress) {
                    // 【C-3修复】DataStore 的 Flow.first() 是 IO 操作，
                    // 不能在 Main dispatcher 下直接调用，否则可能引发 ANR。
                    val (soundMode, vibrationMode, ringtone) = withContext(Dispatchers.IO) {
                        Triple(
                            SoundMode.fromInt(settingsDataStore.soundModeFlow.first()),
                            VibrationMode.fromInt(settingsDataStore.vibrationModeFlow.first()),
                            Ringtone.fromInt(settingsDataStore.ringtoneFlow.first())
                        )
                    }
                    alarmPlayer.play(soundMode, vibrationMode, ringtone)
                }
            }
            TimerState.PAUSED, TimerState.IDLE -> {
                alarmPlayer.stop()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            serviceScope.launch {
                when (action) {
                    ACTION_START -> repository.startTimer()
                    ACTION_PAUSE -> repository.pauseTimer()
                    ACTION_STOP -> repository.stopTimer()
                    ACTION_NEXT -> repository.nextPhase()
                    ACTION_SNOOZE -> repository.snooze()
                }
            }
        }
        
        if (!isForeground) {
            val initialNotification = buildNotification(
                repository.timerState.value,
                repository.timeRemaining.value,
                repository.timerMode.value,
                false
            )
            startForeground(NOTIFICATION_ID, initialNotification)
            isForeground = true
        }
        
        return START_STICKY
    }

    private fun updateNotification(state: TimerState, time: Long, mode: TimerMode, wakeScreen: Boolean) {
        if (!isForeground) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state, time, mode, wakeScreen))
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun buildNotification(state: TimerState, time: Long, mode: TimerMode, wakeScreen: Boolean): Notification {
        val title = when (mode) {
            TimerMode.FOCUS -> getString(R.string.state_focus)
            TimerMode.SHORT_BREAK -> getString(R.string.state_short_break)
            TimerMode.LONG_BREAK -> getString(R.string.state_long_break)
        }

        val minutes = time / 60
        val seconds = time % 60
        val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val contentText = when (state) {
            TimerState.IDLE -> getString(R.string.notification_idle)
            TimerState.RUNNING -> getString(R.string.notification_running, timeString)
            TimerState.PAUSED -> getString(R.string.notification_paused, timeString)
            TimerState.FINISHED -> when (mode) {
                TimerMode.FOCUS -> getString(R.string.notification_finished_focus)
                else -> getString(R.string.notification_finished_break)
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(state == TimerState.RUNNING || state == TimerState.PAUSED)
            .setPriority(
                if (state == TimerState.FINISHED) NotificationCompat.PRIORITY_HIGH 
                else NotificationCompat.PRIORITY_LOW
            )
            // Only sound a basic notification if we don't have our own sound system active.
            // But since we DO have our own AlarmPlayer, we can set silent here for FINISHED,
            // or let the system default play as a fallback. 
            // We setOnlyAlertOnce to avoid spamming.
            .setOnlyAlertOnce(true) 

        // Actions
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val pendingMainActivity = PendingIntent.getActivity(
            this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.setContentIntent(pendingMainActivity)

        if (state == TimerState.FINISHED && wakeScreen) {
            builder.setFullScreenIntent(pendingMainActivity, true)
        }

        when (state) {
            TimerState.RUNNING -> {
                builder.addAction(0, getString(R.string.action_pause), getServicePendingIntent(ACTION_PAUSE))
                builder.addAction(0, getString(R.string.action_stop), getServicePendingIntent(ACTION_STOP))
            }
            TimerState.PAUSED -> {
                builder.addAction(0, getString(R.string.action_resume), getServicePendingIntent(ACTION_START))
                builder.addAction(0, getString(R.string.action_stop), getServicePendingIntent(ACTION_STOP))
            }
            TimerState.FINISHED -> {
                builder.addAction(0, getString(R.string.action_next_short), getServicePendingIntent(ACTION_NEXT))
                builder.addAction(0, getString(R.string.action_snooze), getServicePendingIntent(ACTION_SNOOZE))
                // STOP also stops alarm
                builder.addAction(0, getString(R.string.action_stop), getServicePendingIntent(ACTION_STOP))
            }
            TimerState.IDLE -> {
                builder.addAction(0, getString(R.string.action_start), getServicePendingIntent(ACTION_START))
            }
        }

        return builder.build()
    }

    private fun getServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TimerService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setSound(null, null) // We handle sound via AlarmPlayer!
                enableVibration(false) // We handle vibration via AlarmPlayer!
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        alarmPlayer.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
