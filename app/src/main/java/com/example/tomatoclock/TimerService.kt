package com.example.tomatoclock

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var repository: TimerRepository

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var isForeground = false

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

        combine(
            repository.timerState,
            repository.timeRemaining,
            repository.timerMode
        ) { state, time, mode ->
            updateNotification(state, time, mode)
        }.launchIn(serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.action?.let { action ->
            when (action) {
                ACTION_START -> repository.startTimer()
                ACTION_PAUSE -> repository.pauseTimer()
                ACTION_STOP -> repository.stopTimer()
                ACTION_NEXT -> {
                    repository.nextPhase()
                    repository.startTimer()
                }
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
            TimerState.IDLE -> getString(R.string.state_idle) + " " + title
            TimerState.RUNNING -> getString(R.string.state_running, timeString)
            TimerState.PAUSED -> getString(R.string.state_paused) + " - " + timeString
            TimerState.FINISHED -> title + " " + getString(R.string.state_finished_focus)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            // Use a default Android icon since we haven't created one yet
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(state == TimerState.RUNNING || state == TimerState.PAUSED)
            .setPriority(
                if (state == TimerState.FINISHED) NotificationCompat.PRIORITY_HIGH 
                else NotificationCompat.PRIORITY_LOW
            )
            .setOnlyAlertOnce(state != TimerState.FINISHED)

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
                builder.addAction(0, getString(R.string.action_next), getServicePendingIntent(ACTION_NEXT))
                builder.addAction(0, getString(R.string.action_snooze), getServicePendingIntent(ACTION_SNOOZE))
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
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows the ongoing timer and alerts when finished"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
