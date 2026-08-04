package com.sentinelshield.antitheft.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sentinelshield.antitheft.SecurityPreferences
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer

import androidx.compose.material.icons.filled.BugReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntruderSettingsScreen(
    onNavigateToDebugLogs: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var captureMode by remember { mutableStateOf(SecurityPreferences.getIntruderCaptureMode(context)) }
    var videoDuration by remember { mutableIntStateOf(SecurityPreferences.getIntruderVideoDuration(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Intruder Selfie Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onNavigateToDebugLogs != null) {
                        IconButton(onClick = onNavigateToDebugLogs) {
                            Icon(Icons.Default.BugReport, contentDescription = "Diagnostic Logs")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RoundedCardContainer {
                SettingsSwitchItem(
                    title = "Capture Mode",
                    subtitle = if (captureMode == "VIDEO") "Record a short video" else "Take a silent photo",
                    icon = if (captureMode == "VIDEO") Icons.Default.Videocam else Icons.Default.CameraAlt,
                    isChecked = captureMode == "VIDEO",
                    onCheckedChange = { isVideo ->
                        val newMode = if (isVideo) "VIDEO" else "PHOTO"
                        SecurityPreferences.setIntruderCaptureMode(context, newMode)
                        captureMode = newMode
                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "IntruderSettings", "Intruder capture mode changed to: $newMode", force = true)
                    }
                )

                if (captureMode == "VIDEO") {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Video Duration: ${videoDuration}s",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = videoDuration.toFloat(),
                            onValueChange = { 
                                videoDuration = it.toInt()
                                SecurityPreferences.setIntruderVideoDuration(context, videoDuration)
                                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "IntruderSettings", "Intruder video duration set to: ${videoDuration}s", force = true)
                            },
                            valueRange = 3f..10f,
                            steps = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Intruder Selfie Diagnostics Section
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Intruder Selfie Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Verify front camera access, failed password attempts receiver status, and background capture policies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val hasCameraPerm = context.checkSelfPermission(android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val devicePolicyManager = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
                                val adminComponent = android.content.ComponentName(context, com.sentinelshield.antitheft.LockScreenAdminReceiver::class.java)
                                val hasAdmin = devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent)

                                com.sentinelshield.antitheft.utils.DebugLogger.log(
                                    context,
                                    "IntruderDiagnostics",
                                    "Camera Permission: $hasCameraPerm | DeviceAdmin Active: $hasAdmin | Mode: $captureMode (${videoDuration}s)"
                                )
                                android.widget.Toast.makeText(context, "Intruder diagnostic logged! (Camera: $hasCameraPerm, Admin: $hasAdmin)", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Camera", style = MaterialTheme.typography.bodySmall)
                        }

                        if (onNavigateToDebugLogs != null) {
                            Button(
                                onClick = onNavigateToDebugLogs,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("View Logs", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
