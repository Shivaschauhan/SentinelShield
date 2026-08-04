package com.sentinelshield.antitheft.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.sentinelshield.antitheft.SecurityPreferences

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PocketSettingsScreen(
    onNavigateToRingtone: () -> Unit,
    onNavigateToDebugLogs: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var armingDelay by remember { mutableIntStateOf(SecurityPreferences.getPocketArmingDelay(context)) }
    var gracePeriod by remember { mutableIntStateOf(SecurityPreferences.getPocketGracePeriod(context)) }
    var useStrobe by remember { mutableStateOf(SecurityPreferences.isPocketUseStrobe(context)) }
    var forceVolume by remember { mutableStateOf(SecurityPreferences.isPocketForceVolume(context)) }
    
    var showDialogFor by remember { mutableStateOf<String?>(null) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                useStrobe = true
                SecurityPreferences.setPocketUseStrobe(context, true)
            } else {
                useStrobe = false
                SecurityPreferences.setPocketUseStrobe(context, false)
            }
        }
    )

    if (showDialogFor != null) {
        val title = when (showDialogFor) {
            "arming" -> "Arming Delay"
            "grace" -> "Grace Period"
            "strobe" -> "Camera Strobe"
            "volume" -> "Force Volume"
            else -> ""
        }
        val text = when (showDialogFor) {
            "arming" -> "Wait time before turning on the alarm, giving you time to put the phone in your pocket safely."
            "grace" -> "Wait time before the alarm goes off after taking the phone out of your pocket. The phone vibrates silently to warn you to unlock it."
            "strobe" -> "Quickly blinks the camera flashlight when the alarm triggers to scare the thief. (Needs Camera permission)"
            "volume" -> "Forces the volume to maximum when the alarm triggers, even if your phone is on silent."
            else -> ""
        }
        AlertDialog(
            onDismissRequest = { showDialogFor = null },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = { showDialogFor = null }) {
                    Text("Got it")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pocket Snatch Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Hint text
            Text(
                text = "Tip: Long-press any setting to learn more about what it does.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            com.sentinelshield.antitheft.ui.components.RoundedCardContainer {
                SettingsRowItem(
                    title = "Alarm Sound",
                    subtitle = "Choose loud built-in or custom alarms",
                    icon = androidx.compose.material.icons.Icons.Default.VolumeUp,
                    onClick = onNavigateToRingtone
                )
            }

            // Arming Delay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showDialogFor = "arming" }
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Arming Delay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0s")
                    Text(
                        "${armingDelay}s",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("15s")
                }

                Slider(
                    value = armingDelay.toFloat(),
                    onValueChange = { 
                        armingDelay = it.toInt()
                        SecurityPreferences.setPocketArmingDelay(context, armingDelay)
                    },
                    valueRange = 0f..15f,
                    steps = 14
                )
            }

            // Grace Period
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { },
                        onLongClick = { showDialogFor = "grace" }
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Grace Period (Warning)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0s")
                    Text(
                        "${gracePeriod}s",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("10s")
                }

                Slider(
                    value = gracePeriod.toFloat(),
                    onValueChange = { 
                        gracePeriod = it.toInt()
                        SecurityPreferences.setPocketGracePeriod(context, gracePeriod)
                    },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }

            // Strobe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val newStrobe = !useStrobe
                            if (newStrobe && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                useStrobe = newStrobe
                                SecurityPreferences.setPocketUseStrobe(context, newStrobe)
                            }
                        },
                        onLongClick = { showDialogFor = "strobe" }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Strobe",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = useStrobe,
                    onCheckedChange = null // handled by row click
                )
            }

            // Force Volume
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            val newVol = !forceVolume
                            forceVolume = newVol
                            SecurityPreferences.setPocketForceVolume(context, newVol)
                        },
                        onLongClick = { showDialogFor = "volume" }
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Force Max Volume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Switch(
                    checked = forceVolume,
                    onCheckedChange = null // handled by row click
                )
            }

            // Debug & Diagnostics Section for Pocket Snatch
            com.sentinelshield.antitheft.ui.components.RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Pocket Snatch Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Debug pocket sensor responses, proximity triggers, and acceleration threshold logs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val sensorManager = context.getSystemService(android.hardware.SensorManager::class.java)
                                val proximity = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY)
                                val accel = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)
                                com.sentinelshield.antitheft.utils.DebugLogger.log(
                                    context,
                                    "PocketDiagnostics",
                                    "Proximity Sensor: ${proximity?.name ?: "NOT FOUND"} (Max range: ${proximity?.maximumRange ?: 0}cm) | Accelerometer: ${accel?.name ?: "NOT FOUND"}"
                                )
                                android.widget.Toast.makeText(context, "Pocket sensor diagnostic recorded in logs!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Sensor", style = MaterialTheme.typography.bodySmall)
                        }

                        if (onNavigateToDebugLogs != null) {
                            androidx.compose.material3.Button(
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
