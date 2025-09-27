package com.kiran.telemetrylab

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.metrics.performance.JankStats
import com.kiran.telemetrylab.ui.TelemetryScreen
import com.kiran.telemetrylab.ui.TelemetryViewModel
import com.kiran.telemetrylab.ui.theme.TelemetryLabTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var jankStats: JankStats? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Handle permission grant/denial if needed
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val viewModel: TelemetryViewModel = hiltViewModel()

            TelemetryLabTheme {
                window.statusBarColor = MaterialTheme.colorScheme.background.toArgb()
                window.navigationBarColor = MaterialTheme.colorScheme.background.toArgb()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TelemetryScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        jankStats = JankStats.createAndTrack(
            window,
        ) { frameData ->
            val viewModel: TelemetryViewModel by viewModels()
            viewModel.processFrameData(frameData)
        }
    }

    override fun onPause() {
        super.onPause()
        jankStats?.let {
            it.isTrackingEnabled = false
        }
        jankStats = null
    }
}