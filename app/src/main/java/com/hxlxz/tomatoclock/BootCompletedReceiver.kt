package com.hxlxz.tomatoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TimerRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device rebooted. Initializing TimerRepository to restore alarms.")
            // Injecting TimerRepository forces it to instantiate.
            // Its init block will automatically read the saved state from DataStore
            // and reschedule the Exact Alarm if the timer was RUNNING.
            // No further action is required here since the init block uses the application scope.
        }
    }
}
