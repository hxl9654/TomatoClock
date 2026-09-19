package com.hxlxz.tomatoclock

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeConfig @Inject constructor() {
    // [Fix-CS-1] internal var：限制为 module-internal，防止生产代码意外修改。
    // 测试中由 E2E 测试直接修改（两者均在同一 module 内）。
    internal var multiplier: Long = 60L
}
