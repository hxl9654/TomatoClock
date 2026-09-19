package com.hxlxz.tomatoclock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

private const val TAG = "AndroidAlarmScheduler"

class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private fun getAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun scheduleAlarm(triggerAtMillis: Long) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Android 12+ 要求检查运行时精确闹钟权限（用户可能在系统设置中撤销）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted by user. " +
                    "Timer will still run via coroutine, but screen wake-up alarm won't fire. " +
                    "User should grant permission in System Settings > Special App Access > Alarms & Reminders.")
                return
            }

            // triggerAtMillis is based on SystemClock.elapsedRealtime().
            // AlarmManager.setAlarmClock uses RTC (Real Time Clock), i.e. System.currentTimeMillis().
            // We must convert the elapsed time base to RTC base.
            val delayMs = triggerAtMillis - android.os.SystemClock.elapsedRealtime()
            val triggerAtRTC = System.currentTimeMillis() + delayMs

            val pendingIntent = getAlarmPendingIntent()
            val info = AlarmManager.AlarmClockInfo(triggerAtRTC, pendingIntent)
            alarmManager.setAlarmClock(info, pendingIntent)
            Log.d(TAG, "Exact alarm scheduled for ${triggerAtRTC}ms (RTC), delay=${delayMs}ms")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Cannot schedule exact alarm. Permission may have been revoked.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while scheduling alarm.", e)
        }
    }

    override fun cancelAlarm() {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(getAlarmPendingIntent())
            Log.d(TAG, "Exact alarm cancelled.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while cancelling alarm.", e)
        }
    }
}
