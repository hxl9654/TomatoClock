package com.hxlxz.tomatoclock

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeConfig @Inject constructor() {
    var multiplier: Long = 60L
}
