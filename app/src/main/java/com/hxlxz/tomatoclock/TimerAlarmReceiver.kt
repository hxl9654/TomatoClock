package com.hxlxz.tomatoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TimerRepository

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerAlarmReceiver", "Exact alarm fired! Forcing timer finish.")
        repository.forceFinishTimer()
    }
}
