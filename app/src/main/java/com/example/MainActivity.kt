package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.ClipsGalleryScreen
import com.example.ui.Dlss5StudioScreen
import com.example.ui.HomeScreen
import com.example.ui.MainViewModel
import com.example.ui.Sd685OptimizerScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager

        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val recordingState by viewModel.recordingState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // MediaProjection launcher
                val projectionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK && result.data != null) {
                        viewModel.startRecordingWithProjectionResult(result.resultCode, result.data!!)
                    } else {
                        Toast.makeText(this, "Screen capture permission required to record", Toast.LENGTH_SHORT).show()
                    }
                }

                // Standard permissions launcher
                val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                } else {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                        permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Permissions handled
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                }

                // Toast/Snackbar observer
                LaunchedEffect(Unit) {
                    viewModel.screenRecorderManager.toastEvents.collectLatest { msg ->
                        scope.launch {
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .border(1.dp, DarkBorder)
                                .testTag("main_bottom_nav"),
                            containerColor = DarkSurface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == AppTab.DASHBOARD,
                                onClick = { viewModel.selectTab(AppTab.DASHBOARD) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Recorder",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text("Recorder", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberRed,
                                    selectedTextColor = CyberRed,
                                    indicatorColor = CyberRed.copy(alpha = 0.2f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_recorder")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.DLSS_STUDIO,
                                onClick = { viewModel.selectTab(AppTab.DLSS_STUDIO) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "DLSS 5 AI",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text("DLSS 5 AI", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.2f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_dlss")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.SD685_OPTIMIZER,
                                onClick = { viewModel.selectTab(AppTab.SD685_OPTIMIZER) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = "SD 685 Guard",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text("SD685 Guard", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberEmerald,
                                    selectedTextColor = CyberEmerald,
                                    indicatorColor = CyberEmerald.copy(alpha = 0.2f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_sd685")
                            )

                            NavigationBarItem(
                                selected = currentTab == AppTab.GALLERY,
                                onClick = { viewModel.selectTab(AppTab.GALLERY) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = "Clips",
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text("Clips", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TextPrimary,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = DarkBorder,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_clips")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground)
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        when (currentTab) {
                            AppTab.DASHBOARD -> HomeScreen(
                                viewModel = viewModel,
                                recordingState = recordingState,
                                onStartRecordingClick = {
                                    // Check overlay permission if user has floating HUD enabled
                                    if (viewModel.settings.value.showFloatingHud && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
                                        try {
                                            val overlayIntent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName")
                                            )
                                            startActivity(overlayIntent)
                                            Toast.makeText(this@MainActivity, "Enable overlay for Floating In-Game HUD Bar", Toast.LENGTH_LONG).show()
                                        } catch (_: Exception) {}
                                    }

                                    try {
                                        val captureIntent = mediaProjectionManager?.createScreenCaptureIntent()
                                        if (captureIntent != null) {
                                            projectionLauncher.launch(captureIntent)
                                        } else {
                                            Toast.makeText(this@MainActivity, "Screen capture service not available", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(this@MainActivity, "Could not open screen capture dialog: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            AppTab.DLSS_STUDIO -> Dlss5StudioScreen(viewModel = viewModel)
                            AppTab.SD685_OPTIMIZER -> Sd685OptimizerScreen(viewModel = viewModel)
                            AppTab.GALLERY -> ClipsGalleryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
