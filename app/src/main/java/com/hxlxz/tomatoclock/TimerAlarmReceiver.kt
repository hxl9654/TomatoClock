package com.hxlxz.tomatoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TimerRepository

    @Inject
    lateinit var applicationScope: kotlinx.coroutines.CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        Log.d("TimerAlarmReceiver", "Exact alarm fired! Forcing timer finish.")
        applicationScope.launch {
            try {
                repository.forceFinishTimer(fromAlarm = true)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
