package com.hxlxz.tomatoclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
    
    private var partialWakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "tomatoclock_channel"
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
            repository.timerMode
        ) { state, time, mode ->
            updateNotification(state, time, mode)
        }.launchIn(serviceScope)

        // Handle State Side Effects (WakeLocks, Alarms)
        repository.timerState.onEach { state ->
            handleStateSideEffects(state)
        }.launchIn(serviceScope)
    }

    private suspend fun handleStateSideEffects(state: TimerState) {
        when (state) {
            TimerState.RUNNING -> {
                acquireWakeLock()
                alarmPlayer.stop()
            }
            TimerState.FINISHED -> {
                // Read latest settings
                val alertMode = AlertMode.fromInt(settingsDataStore.alertModeFlow.first())
                val ringtone = Ringtone.fromInt(settingsDataStore.ringtoneFlow.first())
                val wakeScreen = settingsDataStore.wakeScreenFlow.first()
                
                if (wakeScreen) {
                    wakeUpScreen()
                }
                
                alarmPlayer.play(alertMode, ringtone)
                
                // Release the persistent partial wake lock since we are no longer running,
                // but wakeUpScreen will handle keeping it bright for a moment.
                releaseWakeLock()
            }
            TimerState.PAUSED, TimerState.IDLE -> {
                releaseWakeLock()
                alarmPlayer.stop()
            }
        }
    }
    
    private fun acquireWakeLock() {
        if (partialWakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            partialWakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "TomatoClock::TimerWakeLock"
            ).apply {
                acquire(2 * 60 * 60 * 1000L /* 2 hours max */) 
            }
        }
    }
    
    private fun releaseWakeLock() {
        partialWakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        partialWakeLock = null
    }

    private fun wakeUpScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TomatoClock::WakeScreen"
        )
        wakeLock.acquire(5000) // 亮屏 5 秒
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_START -> repository.startTimer()
                ACTION_PAUSE -> repository.pauseTimer()
                ACTION_STOP -> repository.stopTimer()
                ACTION_NEXT -> repository.nextPhase()
                ACTION_SNOOZE -> repository.snooze()
            }
        }
        
        if (!isForeground) {
            val initialNotification = buildNotification(
                repository.timerState.value,
                repository.timeRemaining.value,
                repository.timerMode.value
            )
            startForeground(NOTIFICATION_ID, initialNotification)
            isForeground = true
        }
        
        return START_STICKY
    }

    private fun updateNotification(state: TimerState, time: Long, mode: TimerMode) {
        if (!isForeground) return
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(state, time, mode))
    }

    private fun buildNotification(state: TimerState, time: Long, mode: TimerMode): Notification {
        val title = when (mode) {
            TimerMode.FOCUS -> getString(R.string.state_focus)
            TimerMode.SHORT_BREAK -> getString(R.string.state_short_break)
            TimerMode.LONG_BREAK -> getString(R.string.state_long_break)
        }

        val minutes = time / 60
        val seconds = time % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)

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
        releaseWakeLock()
        alarmPlayer.stop()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
