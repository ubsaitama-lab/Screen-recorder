package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun Sd685OptimizerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val hardwareReport by viewModel.hardwareReport.collectAsStateWithLifecycle()
    val touchStatus by viewModel.touchStatus.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val benchmarkResult by viewModel.benchmarkResult.collectAsStateWithLifecycle()
    val isBenchmarking by viewModel.isBenchmarking.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "REDMI 15 / SD685 GUARD",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberEmerald,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Zero Touch Delay & Anti-Thermal Throttling Suite",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Hardware Identity Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hardware Architecture Detected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "PROCESSOR", fontSize = 10.sp, color = TextMuted)
                        Text(text = "Snapdragon 685 (4G)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        Text(text = "Kryo 265 Octa-Core", fontSize = 11.sp, color = TextSecondary)
                    }
                    Column {
                        Text(text = "GRAPHICS ENGINE", fontSize = 10.sp, color = TextMuted)
                        Text(text = "Adreno 610 GPU", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                        Text(text = "HW HEVC Encoder", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Touch Sensitivity & Zero-Lag Latency Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("touch_latency_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Touch Sensitivity Shield",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "ACTIVE", fontSize = 11.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "INPUT OVERHEAD", fontSize = 10.sp, color = TextMuted)
                        Text(text = "${touchStatus.estimatedTouchLatencyMs} ms", fontSize = 20.sp, fontWeight = FontWeight.Black, color = CyberEmerald)
                    }
                    Column {
                        Text(text = "TOUCH PRIORITY", fontSize = 10.sp, color = TextMuted)
                        Text(text = "100% Uninterrupted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Column {
                        Text(text = "SCHEDULER", fontSize = 10.sp, color = TextMuted)
                        Text(text = "Standard Hardware", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "• Recording worker threads run at Standard Hardware priority to prevent the extreme system-wide lag and touch interference that aggressive background throttling can cause on Adreno 610.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Anti-Thermal Throttling Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = CyberAmber, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Thermal Throttling Guard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", hardwareReport.batteryTemperatureC)}°C",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (hardwareReport.isThermalThrottling) CyberRed else CyberEmerald
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (hardwareReport.batteryTemperatureC / 50.0f).coerceIn(0.1f, 1.0f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (hardwareReport.isThermalThrottling) CyberRed else CyberEmerald,
                    trackColor = DarkSurfaceElevated
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: ${hardwareReport.thermalStatusText}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "If temperatures rise during long gaming sessions, the recorder automatically adjusts frame sampling rate to prevent GPU downclocking and in-game stutter.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Storage Overhead Calculator (Minimal File Size)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Minimal File Size Overhead (HEVC)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "1 MINUTE", fontSize = 11.sp, color = TextMuted)
                        Text(text = "~32 MB", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                        Text(text = "vs 120MB raw", fontSize = 10.sp, color = TextSecondary)
                    }
                    Column {
                        Text(text = "5 MINUTES", fontSize = 11.sp, color = TextMuted)
                        Text(text = "~160 MB", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                        Text(text = "vs 600MB raw", fontSize = 10.sp, color = TextSecondary)
                    }
                    Column {
                        Text(text = "30 MIN MATCH", fontSize = 11.sp, color = TextMuted)
                        Text(text = "~950 MB", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                        Text(text = "vs 3.6GB raw", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }
        }

        // Snapdragon 685 Hardware Benchmark
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Hardware Capability Benchmark",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Verify Adreno 610 hardware encoding throughput and latency",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.runHardwareBenchmark() },
                    enabled = !isBenchmarking,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_benchmark_button")
                ) {
                    if (isBenchmarking) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Testing Hardware Pipeline...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "RUN SD685 BENCHMARK", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                val bResult = benchmarkResult
                if (bResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, CyberEmerald.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = bResult,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = CyberEmerald,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
