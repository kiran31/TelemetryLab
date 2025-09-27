package com.kiran.telemetrylab.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A data class holding all the performance metrics to be displayed on the UI.
 */
data class PerformanceMetrics(
    val frameTimeMillis: Double = 0.0,
    val jankFrameCount: Int = 0,
    val totalFrameCount: Int = 0,
) {
    val jankPercentage: Float
        get() = if (totalFrameCount > 0) {
            (jankFrameCount.toFloat() / totalFrameCount) * 100
        } else {
            0f
        }
}

@Singleton
class TelemetryDataBus @Inject constructor() {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics = _metrics.asStateFlow()

    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
    }

    fun postMetrics(newMetrics: PerformanceMetrics) {
        _metrics.value = newMetrics
    }
}