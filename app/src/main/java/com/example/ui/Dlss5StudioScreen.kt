package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
fun Dlss5StudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val dlss5State by viewModel.dlss5State.collectAsStateWithLifecycle()
    val videos by viewModel.allVideos.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    var splitSliderPosition by remember { mutableFloatStateOf(0.5f) }

    val rawFrame = remember { viewModel.dlss5Engine.createComparisonFrame(false) }
    val dlssFrame = remember { viewModel.dlss5Engine.createComparisonFrame(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "DLSS 5 AI STUDIO",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberCyan,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Deep Learning Super Sampling 5 • 4K 144 FPS Engine",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Interactive Before vs After Comparison Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dlss5_comparison_card"),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Interactive Comparison",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Drag slider below",
                        fontSize = 11.sp,
                        color = CyberCyan
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated gaming split view
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                ) {
                    // Show either raw or dlss frame based on slider or layered
                    val currentBitmap = if (splitSliderPosition > 0.5f) dlssFrame else rawFrame
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Comparison View",
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay labels
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (splitSliderPosition <= 0.5f) "RAW NATIVE (1080p 60FPS)" else "DLSS 5 (4K 144FPS AI)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (splitSliderPosition <= 0.5f) CyberAmber else CyberEmerald
                        )
                    }

                    // Bottom info tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceElevated.copy(alpha = 0.85f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (splitSliderPosition <= 0.5f) "Size: 450 MB • Standard" else "Size: 189 MB • HEVC CRF Minimal",
                            fontSize = 10.sp,
                            color = if (splitSliderPosition <= 0.5f) TextMuted else CyberCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = splitSliderPosition,
                    onValueChange = { splitSliderPosition = it },
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = DarkBorder
                    ),
                    modifier = Modifier.testTag("dlss_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "👈 1080p Raw Native", fontSize = 11.sp, color = CyberAmber)
                    Text(text = "DLSS 5 4K 144FPS 👉", fontSize = 11.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Processing Status if running
        if (dlss5State.isProcessing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ Enhancing Clip with DLSS 5...",
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${(dlss5State.progress * 100).toInt()}%",
                            fontWeight = FontWeight.Black,
                            color = CyberEmerald,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { dlss5State.progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = CyberCyan,
                        trackColor = DarkSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dlss5State.stageDescription,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        // DLSS 5 Configuration Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "DLSS 5 Neural Parameters",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )

                // AI Sharpness Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "AI Edge Sharpness", fontSize = 13.sp, color = TextPrimary)
                        Text(
                            text = "${(settings.dlss5SharpnessLevel * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                    }
                    Slider(
                        value = settings.dlss5SharpnessLevel,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(dlss5SharpnessLevel = it))
                        },
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                    )
                    Text(
                        text = "Removes blurring and highlights in-game UI details and crosshairs",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }

                // Frame Generation 144 FPS Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Temporal Frame Generation (144 FPS)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Synthesizes intermediate frames for ultra-high refresh smoothness",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = settings.dlss5FrameGenEnabled,
                        onCheckedChange = {
                            viewModel.updateSettings(settings.copy(dlss5FrameGenEnabled = it))
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberEmerald, checkedTrackColor = CyberEmerald.copy(alpha = 0.3f))
                    )
                }

                // Minimal Size HEVC Optimization Info
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Minimal File Size Overhead Mode Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberEmerald
                            )
                            Text(
                                text = "HEVC CRF + AI variable rate compression saves up to 58% storage space compared to raw MP4.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Apply DLSS 5 to Existing Clips
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Apply DLSS 5 to Saved Clips",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Upscale your recordings to 4K 144FPS with zero in-game lag penalty",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (videos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "No saved videos yet. Record your first clip!", color = TextMuted, fontSize = 12.sp)
                    }
                } else {
                    for (video in videos.take(3)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceElevated)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${video.width}×${video.height} • ${video.fps} FPS • ${(video.fileSizeBytes / (1024 * 1024))} MB",
                                    fontSize = 11.sp,
                                    color = if (video.hasDlss5Applied) CyberEmerald else TextSecondary
                                )
                            }

                            if (video.hasDlss5Applied) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyberEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("4K DLSS 5", fontSize = 11.sp, color = CyberEmerald, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.applyDlss5ToVideo(video) },
                                    enabled = !dlss5State.isProcessing,
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Apply DLSS 5", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
