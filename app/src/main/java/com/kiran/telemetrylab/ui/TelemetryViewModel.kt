package com.kiran.telemetrylab.ui

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.metrics.performance.FrameData
import com.kiran.telemetrylab.data.PerformanceMetrics
import com.kiran.telemetrylab.data.TelemetryDataBus
import com.kiran.telemetrylab.service.ComputeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class TelemetryUiState(
    val isProcessing: Boolean = false,
    val computeLoad: Float = 1f,
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val isPowerSaveMode: Boolean = false,
)

@HiltViewModel
class TelemetryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataBus: TelemetryDataBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState = _uiState.asStateFlow()

    private val frameDataWindow = ArrayDeque<FrameData>(1800) // 30s at 60fps
    private val windowDurationNanos = 30 * 1_000_000_000L

    init {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _uiState.update { it.copy(isPowerSaveMode = powerManager.isPowerSaveMode) }

        dataBus.isServiceRunning
            .onEach { isRunning ->
                _uiState.update { it.copy(isProcessing = isRunning) }
            }
            .launchIn(viewModelScope)

        dataBus.metrics
            .onEach { metrics ->
                _uiState.update { it.copy(performanceMetrics = metrics) }
            }
            .launchIn(viewModelScope)
    }

    fun onProcessingToggled(isProcessing: Boolean) {
        if (isProcessing) {
            startService()
        } else {
            stopService()
        }
    }

    fun onComputeLoadChanged(newLoad: Float) {
        _uiState.update { it.copy(computeLoad = newLoad) }
        updateServiceLoad(newLoad.toInt())
    }

    fun processFrameData(frameData: FrameData) {
        // Add new frame data and remove old ones
        frameDataWindow.add(frameData)
        while (frameData.frameStartNanos - frameDataWindow.first().frameStartNanos > windowDurationNanos) {
            frameDataWindow.removeFirst()
        }

        // Calculate metrics from the window
        val jankCount = frameDataWindow.count { it.isJank }
        val avgFrameTime = frameDataWindow.map { it.frameDurationUiNanos }.average()

        val newMetrics = PerformanceMetrics(
            frameTimeMillis = avgFrameTime / 1_000_000.0,
            jankFrameCount = jankCount,
            totalFrameCount = frameDataWindow.size
        )

        dataBus.postMetrics(newMetrics)
    }

    private fun startService() {
        Intent(context, ComputeService::class.java).also {
            it.action = ComputeService.ACTION_START
            it.putExtra(ComputeService.EXTRA_COMPUTE_LOAD, _uiState.value.computeLoad.toInt())
            context.startService(it)
        }
    }

    private fun stopService() {
        Intent(context, ComputeService::class.java).also {
            it.action = ComputeService.ACTION_STOP
            context.startService(it)
        }
    }

    private fun updateServiceLoad(load: Int) {
        if (_uiState.value.isProcessing) {
            Intent(context, ComputeService::class.java).also {
                it.action = ComputeService.ACTION_UPDATE_LOAD
                it.putExtra(ComputeService.EXTRA_COMPUTE_LOAD, load)
                context.startService(it)
            }
        }
    }
}