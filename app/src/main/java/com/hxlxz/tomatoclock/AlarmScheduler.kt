package com.hxlxz.tomatoclock

interface AlarmScheduler {
    fun scheduleAlarm(triggerAtMillis: Long)
    fun cancelAlarm()
}
