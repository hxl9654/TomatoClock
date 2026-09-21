package com.hxlxz.tomatoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: TimerRepository

    @Inject
    lateinit var applicationScope: kotlinx.coroutines.CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val pendingResult = goAsync()
        handleBootCompleted(pendingResult)
    }

    /**
     * 设备重启后的初始化逻辑，提取为独立方法以便单元测试。
     *
     * [SMELL-03修复] 显式调用 repository.ensureInitialized()，
     * 并且配合 goAsync() 避免在恢复 DataStore 时进程被杀。
     */
    @androidx.annotation.VisibleForTesting
    internal fun handleBootCompleted(pendingResult: PendingResult?) {
        Log.d("BootCompletedReceiver", "Device rebooted. Calling ensureInitialized() to restore alarms.")
        applicationScope.launch {
            try {
                repository.ensureInitialized()
            } catch (e: RuntimeException) {
                Log.e("BootCompletedReceiver", "Error during boot initialization.", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
