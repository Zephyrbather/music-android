package com.musicplayer.app.performance

import android.os.Process
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CpuPerformanceManager @Inject constructor() {
    
    fun boostPlaybackThread() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
    }
    
    fun setBackgroundPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
    }
    
    fun setDefaultPriority() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
    }
}
