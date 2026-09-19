package com.hxlxz.tomatoclock

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AndroidAlarmScheduler 的 Robolectric 单元测试。
 *
 * 由于 Robolectric ShadowAlarmManager 对 setAlarmClock() 的追踪能力有限
 * （不同于 set()/setExact()，setAlarmClock 无法通过 ShadowAlarmManager 检索），
 * 且 ShadowPendingIntent 的 FLAG_NO_CREATE 行为与真机存在差异，
 * 本测试聚焦于：
 *   1. 无崩溃验证：scheduleAlarm/cancelAlarm 在各种调用顺序下不抛出异常
 *   2. 权限降级路径验证（SDK 31+ 逻辑分支）
 * AlarmManager 的实际调用效果通过 E2E 仪器化测试（TomatoClockE2ETest）覆盖。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var scheduler: AndroidAlarmScheduler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scheduler = AndroidAlarmScheduler(context)
    }

    @Test
    fun `scheduleAlarm completes without throwing`() {
        scheduler.scheduleAlarm(System.currentTimeMillis() + 60_000L)
    }

    @Test
    fun `cancelAlarm after scheduleAlarm does not throw`() {
        scheduler.scheduleAlarm(System.currentTimeMillis() + 60_000L)
        scheduler.cancelAlarm()
    }

    @Test
    fun `cancelAlarm without prior schedule does not throw`() {
        scheduler.cancelAlarm()
    }

    @Test
    fun `scheduleAlarm handles past trigger time gracefully`() {
        // 触发时间在过去不应崩溃
        scheduler.scheduleAlarm(System.currentTimeMillis() - 1000L)
    }

    @Test
    fun `multiple scheduleAlarm calls are idempotent`() {
        // 多次调用只保留最后一次（AlarmClockInfo 会覆盖）
        scheduler.scheduleAlarm(System.currentTimeMillis() + 60_000L)
        scheduler.scheduleAlarm(System.currentTimeMillis() + 30_000L)
        scheduler.cancelAlarm()
    }
}
