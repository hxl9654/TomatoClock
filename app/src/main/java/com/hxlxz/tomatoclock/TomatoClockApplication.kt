package com.hxlxz.tomatoclock

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TomatoClockApplication : Application() {

    @Inject
    lateinit var timerRepository: TimerRepository

    @Inject
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // [Fix-P-5] Write-Through 策略已在 TimerRepository 各操作方法末尾持久化状态。
                // 此处作为安全网，兜底「计时器运行时心跳窗口内（最多60秒）」的最后一次保存。
                applicationScope.launch {
                    timerRepository.saveCurrentState()
                }
            }
        })
    }
}
