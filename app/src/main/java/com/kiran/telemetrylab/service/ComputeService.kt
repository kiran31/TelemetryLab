package com.kiran.telemetrylab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.kiran.telemetrylab.R
import com.kiran.telemetrylab.data.ConvolutionProcessor
import com.kiran.telemetrylab.data.TelemetryDataBus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class ComputeService : Service() {

    @Inject
    lateinit var convolutionProcessor: ConvolutionProcessor

    @Inject
    lateinit var dataBus: TelemetryDataBus

    private val job = SupervisorJob()
    private lateinit var scope: CoroutineScope

    private var computeLoad = 1
    private var isPowerSaveMode = false

    private val powerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                isPowerSaveMode = powerManager.isPowerSaveMode
                // Restart the job to apply new settings
                if (scope.isActive) {
                    restartComputation()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        registerReceiver(powerModeReceiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        isPowerSaveMode = powerManager.isPowerSaveMode
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                computeLoad = intent.getIntExtra(EXTRA_COMPUTE_LOAD, 1)
                startForegroundService()
                startComputation()
            }
            ACTION_STOP -> {
                stopComputation()
            }
            ACTION_UPDATE_LOAD -> {
                computeLoad = intent.getIntExtra(EXTRA_COMPUTE_LOAD, 1)
                // The loop will pick up the new load in the next iteration
            }
        }
        return START_STICKY
    }

    private fun startComputation() {
        if (::scope.isInitialized && scope.isActive) {
            return // Already running
        }
        scope = CoroutineScope(Dispatchers.Default + job)
        dataBus.setServiceRunning(true)
        scope.launch {
            val targetDelay = if (isPowerSaveMode) 100L else 50L // 10Hz in power save, 20Hz otherwise
            val currentLoad = if (isPowerSaveMode) (computeLoad - 1).coerceAtLeast(1) else computeLoad

            while (isActive) {
                val startTime = System.nanoTime()
                convolutionProcessor.performConvolution(currentLoad)
                val elapsedTime = (System.nanoTime() - startTime) / 1_000_000
                delay(targetDelay - elapsedTime.coerceAtMost(targetDelay))
            }
        }
    }

    private fun restartComputation() {
        scope.coroutineContext.cancelChildren()
        startComputation()
    }


    private fun stopComputation() {
        if (::scope.isInitialized) {
            scope.cancel()
        }
        dataBus.setServiceRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundService() {
        val channelId = "compute_service_channel"
        val channelName = "Compute Service Channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Telemetry Lab")
            .setContentText("Performing edge compute simulation...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with a real icon
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not a bound service
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        unregisterReceiver(powerModeReceiver)
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_UPDATE_LOAD = "ACTION_UPDATE_LOAD"
        const val EXTRA_COMPUTE_LOAD = "EXTRA_COMPUTE_LOAD"
        private const val NOTIFICATION_ID = 1
    }
}