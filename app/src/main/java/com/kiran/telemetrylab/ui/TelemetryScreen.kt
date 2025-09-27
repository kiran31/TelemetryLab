package com.kiran.telemetrylab.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelemetryScreen(viewModel: TelemetryViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Telemetry Lab", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ControlPanel(
                    isProcessing = uiState.isProcessing,
                    computeLoad = uiState.computeLoad,
                    isPowerSaveMode = uiState.isPowerSaveMode,
                    onProcessingToggled = viewModel::onProcessingToggled,
                    onComputeLoadChanged = viewModel::onComputeLoadChanged
                )
                Spacer(modifier = Modifier.height(24.dp))
                PerformanceDashboard(metrics = uiState.performanceMetrics)
                Spacer(modifier = Modifier.height(24.dp))
                ScrollingList(isProcessing = uiState.isProcessing)
            }
        }
    )
}

@Composable
fun ControlPanel(
    isProcessing: Boolean,
    computeLoad: Float,
    isPowerSaveMode: Boolean,
    onProcessingToggled: (Boolean) -> Unit,
    onComputeLoadChanged: (Float) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(visible = isPowerSaveMode, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Power-save mode is ON",
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isPowerSaveMode) Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Edge Compute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = isProcessing,
                    onCheckedChange = onProcessingToggled,
                    thumbContent = {
                        Icon(
                            imageVector = if (isProcessing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Processing"
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Compute Load: ${computeLoad.roundToInt()}",
                style = MaterialTheme.typography.bodyLarge
            )
            Slider(
                value = computeLoad,
                onValueChange = onComputeLoadChanged,
                valueRange = 1f..5f,
                steps = 3 // 4 steps for 5 values (1, 2, 3, 4, 5)
            )
        }
    }
}

@Composable
fun PerformanceDashboard(metrics: com.kiran.telemetrylab.data.PerformanceMetrics) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            label = "Jank",
            value = "${"%.1f".format(metrics.jankPercentage)}%",
            modifier = Modifier.weight(1f),
            valueColor = if (metrics.jankPercentage > 5.0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        MetricCard(
            label = "Jank Frames",
            value = metrics.jankFrameCount.toString(),
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "Avg Frame Time",
            value = "${"%.2f".format(metrics.frameTimeMillis)} ms",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.primary) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun ScrollingList(isProcessing: Boolean, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            while (true) {
                coroutineScope.launch {
                    listState.animateScrollToItem(
                        index = listState.firstVisibleItemIndex + 1,
                        scrollOffset = 10
                    )
                }
                kotlinx.coroutines.delay(16) // Roughly 60fps
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(500) { index ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = index.toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("List Item #$index")
                }
            }
        }
    }
}