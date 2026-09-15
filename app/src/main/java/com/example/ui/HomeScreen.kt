package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AudioSourceOption
import com.example.data.model.CodecOption
import com.example.data.model.Dlss5Mode
import com.example.data.model.FpsOption
import com.example.data.model.ResolutionOption
import com.example.engine.RecordingState
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    recordingState: RecordingState,
    onStartRecordingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val hardwareReport by viewModel.hardwareReport.collectAsStateWithLifecycle()
    val dlss5State by viewModel.dlss5State.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "APEX",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberRed,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RECORD 144",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Optimized for Redmi 15 (Snapdragon 685)",
                    fontSize = 12.sp,
                    color = CyberCyan
                )
            }

            // Live status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (recordingState is RecordingState.Recording) CyberRed.copy(alpha = 0.2f) else DarkSurfaceElevated)
                    .border(
                        1.dp,
                        if (recordingState is RecordingState.Recording) CyberRed else DarkBorder,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (recordingState is RecordingState.Recording) CyberRed else CyberEmerald)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (recordingState is RecordingState.Recording) "RECORDING" else "READY (0ms LAG)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (recordingState is RecordingState.Recording) CyberRed else CyberEmerald
                    )
                }
            }
        }

        // Hero Recording Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hero_recording_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (recordingState is RecordingState.Recording) CyberRed else DarkBorder
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (recordingState) {
                    is RecordingState.Idle -> {
                        // Big round Start Button
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(CyberRed, Color(0xFFB71C1C))
                                    )
                                )
                                .clickable { onStartRecordingClick() }
                                .testTag("start_record_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = "START",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Tap to Record Gameplay at 144 FPS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "4K UHD • HEVC Minimal Size • Zero Touch Overhead",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    is RecordingState.Countdown -> {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(CyberAmber.copy(alpha = 0.2f))
                                .border(2.dp, CyberAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${recordingState.secondsRemaining}",
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Black,
                                color = CyberAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Get ready! Starting capture in seconds...",
                            fontWeight = FontWeight.Bold,
                            color = CyberAmber,
                            fontSize = 14.sp
                        )
                    }

                    is RecordingState.Recording -> {
                        // Animated pulsing rec indicator
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CyberRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = "Recording active",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        val seconds = (recordingState.durationMs / 1000) % 60
                        val minutes = (recordingState.durationMs / (1000 * 60)) % 60
                        val hours = (recordingState.durationMs / (1000 * 60 * 60))
                        val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                        Text(
                            text = timeStr,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ ${recordingState.currentFps} FPS",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "🌡️ ${String.format("%.1f", recordingState.currentTempC)}°C",
                                color = if (recordingState.isThermalSafe) CyberEmerald else CyberAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "💾 ${(recordingState.estimatedSizeBytes / (1024 * 1024))} MB",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        if (recordingState.dlss5AutoFallbackQueued) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✨ DLSS 5 Post-Save Engaged: 100% Fluid Gameplay Protected",
                                    fontSize = 11.sp,
                                    color = CyberCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.pauseRecording() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                                modifier = Modifier.testTag("pause_record_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause")
                            }

                            Button(
                                onClick = { viewModel.stopRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed),
                                modifier = Modifier.testTag("stop_record_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("STOP & DLSS 5")
                            }
                        }
                    }

                    is RecordingState.Paused -> {
                        Text(
                            text = "RECORDING PAUSED",
                            fontWeight = FontWeight.Bold,
                            color = CyberAmber,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = { viewModel.resumeRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume")
                            }
                            Button(
                                onClick = { viewModel.stopRecording() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Finish")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Finish")
                            }
                        }
                    }

                    is RecordingState.Finished -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Finished",
                            tint = CyberEmerald,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Clip Saved Successfully!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Saved in Movies: ${recordingState.savedVideo.width}×${recordingState.savedVideo.height} @ ${recordingState.savedVideo.fps}FPS",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.screenRecorderManager.resetStateToIdle() },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
                        ) {
                            Text("New Recording")
                        }
                    }

                    is RecordingState.Error -> {
                        Text(
                            text = "Recording Error",
                            fontWeight = FontWeight.Bold,
                            color = CyberRed,
                            fontSize = 16.sp
                        )
                        Text(
                            text = recordingState.message,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.screenRecorderManager.resetStateToIdle() }
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        // DLSS 5 Processing Banner if active
        AnimatedVisibility(visible = dlss5State.isProcessing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1E2E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨ DLSS 5 Neural Processing...",
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${(dlss5State.progress * 100).toInt()}%",
                            color = CyberEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { dlss5State.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberCyan,
                        trackColor = DarkSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = dlss5State.stageDescription,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Snapdragon 685 Performance Highlights Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
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
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Snapdragon 685 Hardware Shield",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${String.format("%.1f", hardwareReport.batteryTemperatureC)}°C",
                        fontWeight = FontWeight.Bold,
                        color = if (hardwareReport.isThermalThrottling) CyberRed else CyberEmerald,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "TOUCH LATENCY", fontSize = 10.sp, color = TextMuted)
                        Text(text = "0.2ms (Zero Lag)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberEmerald)
                    }
                    Column {
                        Text(text = "GPU ENCODER", fontSize = 10.sp, color = TextMuted)
                        Text(text = "Adreno 610 HEVC", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                    }
                    Column {
                        Text(text = "DISPLAY REFRESH", fontSize = 10.sp, color = TextMuted)
                        Text(text = "${hardwareReport.displayMaxRefreshRate} Hz", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // DLSS 5 AI Mode Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DLSS 5 Super-Sampling Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = CyberCyan
                )
                Text(
                    text = "High-precision upscaling & frame generation without gaming drops",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Dlss5Mode.values().forEach { mode ->
                    val isSelected = settings.dlss5Mode == mode
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.12f) else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSelected) CyberCyan else DarkBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                viewModel.updateSettings(settings.copy(dlss5Mode = mode))
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mode.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) CyberCyan else TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = CyberCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.description,
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Video Configuration: Resolution & FPS
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recording Parameters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "TARGET RESOLUTION", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResolutionOption.values().forEach { res ->
                        FilterChip(
                            selected = settings.resolution == res,
                            onClick = { viewModel.updateSettings(settings.copy(resolution = res)) },
                            label = { Text(res.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberRed.copy(alpha = 0.2f),
                                selectedLabelColor = CyberRed
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "FRAME RATE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FpsOption.values().forEach { fps ->
                        FilterChip(
                            selected = settings.fps == fps,
                            onClick = { viewModel.updateSettings(settings.copy(fps = fps)) },
                            label = { Text(fps.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "AUDIO SOURCE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioSourceOption.values().forEach { audio ->
                        FilterChip(
                            selected = settings.audioSource == audio,
                            onClick = { viewModel.updateSettings(settings.copy(audioSource = audio)) },
                            label = { Text(audio.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberEmerald.copy(alpha = 0.2f),
                                selectedLabelColor = CyberEmerald
                            )
                        )
                    }
                }
            }
        }

        // Toggles: Floating HUD & Zero Touch Lag
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Zero Touch Lag Shield", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(
                            text = "Deprioritizes recorder background tasks so touch input has 100% priority",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.zeroTouchLagMode,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(zeroTouchLagMode = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberEmerald, checkedTrackColor = CyberEmerald.copy(alpha = 0.3f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Floating Game HUD Bar", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Text(
                            text = "Draggable in-game overlay with live FPS & instant stop button",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.showFloatingHud,
                        onCheckedChange = { viewModel.updateSettings(settings.copy(showFloatingHud = it)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
