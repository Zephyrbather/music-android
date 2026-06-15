package com.musicplayer.app.performance

import android.os.Process
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuPerformanceManager @Inject constructor() {
    fun setBackgroundPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    }
    
    fun setDefaultPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
    }
}
