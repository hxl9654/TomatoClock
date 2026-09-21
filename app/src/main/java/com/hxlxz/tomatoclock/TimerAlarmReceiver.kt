package com.hxlxz.tomatoclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import androidx.core.content.ContextCompat
import kotlin.time.Duration.Companion.seconds

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
                // 主动拉起 TimerService，确保在进程被杀的情况下，服务能响应状态变更并播放闹钟
                val serviceIntent = Intent(context, TimerService::class.java)
                ContextCompat.startForegroundService(context, serviceIntent)

                // [A-2修复] goAsync() 系统超时约 10 秒（API 34+），
                // 此处设 8 秒 timeout 确保在系统强制 ANR 之前完成清理工作。
                withTimeout(8.seconds) {
                    handleAlarm()
                }
            } catch (e: TimeoutCancellationException) {
                Log.e("TimerAlarmReceiver", "forceFinishTimer timed out after 8s, ANR risk avoided.", e)
            } catch (e: Exception) {
                Log.e("TimerAlarmReceiver", "Unexpected error in alarm receiver.", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    /**
     * 闹钟触发后的核心业务逻辑，提取为独立 suspend 方法以便单元测试。
     *
     * [BUG-03修复] 测试可直接调用此方法验证 forceFinishTimer 的调用，
     * 绕开 @AndroidEntryPoint 的 Hilt 注入和 goAsync() 的复杂异步环境。
     */
    @androidx.annotation.VisibleForTesting
    internal suspend fun handleAlarm() {
        repository.forceFinishTimer(fromAlarm = true)
    }
}
