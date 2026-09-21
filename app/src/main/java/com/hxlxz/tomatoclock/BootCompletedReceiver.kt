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
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        handleBootCompleted()
    }

    /**
     * 设备重启后的初始化逻辑，提取为独立方法以便单元测试。
     *
     * [SMELL-03修复] 显式调用 repository.ensureInitialized()，
     * 而非依赖 Hilt 注入的隐式副作用。
     */
    @androidx.annotation.VisibleForTesting
    internal fun handleBootCompleted() {
        Log.d("BootCompletedReceiver", "Device rebooted. Calling ensureInitialized() to restore alarms.")
        repository.ensureInitialized()
    }
}
