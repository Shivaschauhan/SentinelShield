package com.sentinelshield.antitheft.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinelshield.antitheft.SecurityPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingSettingsScreen(
    onNavigateToRingtone: () -> Unit,
    onNavigateToDebugLogs: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var delaySeconds by remember { mutableIntStateOf(SecurityPreferences.getChargingAlarmDelaySeconds(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charging Monitor Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            com.sentinelshield.antitheft.ui.components.RoundedCardContainer {
                SettingsRowItem(
                    title = "Alarm Sound",
                    subtitle = "Choose loud built-in or custom alarms",
                    icon = androidx.compose.material.icons.Icons.Default.VolumeUp,
                    onClick = onNavigateToRingtone
                )
                
                var isOneTimeArmed by remember { mutableStateOf(SecurityPreferences.isOneTimeChargingArmed(context)) }
                
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                SettingsRowItem(
                    title = "One-Time Alarm: " + (if (isOneTimeArmed) "Active" else "Inactive"),
                    subtitle = if (isOneTimeArmed) "Armed from notification. Tap to disarm." else "Arm this from the notification panel when plugged in.",
                    icon = androidx.compose.material.icons.Icons.Default.NotificationsActive,
                    iconTint = if (isOneTimeArmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        if (isOneTimeArmed) {
                            SecurityPreferences.setOneTimeChargingArmed(context, false)
                            isOneTimeArmed = false
                            // Stop the alarm if it's currently ringing
                            context.stopService(android.content.Intent(context, com.sentinelshield.antitheft.SecurityAlertService::class.java))
                            // We need to restart the service to clear the notification
                            com.sentinelshield.antitheft.SecurityMonitorService.start(context)
                            android.widget.Toast.makeText(context, "One-Time Charge Alarm Disarmed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Alarm Delay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Wait before triggering the alarm when the charger is unplugged. This gives you time to unlock and disarm the device before the alarm sounds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0s")
                    Text(
                        "${delaySeconds}s",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("30s")
                }

                Slider(
                    value = delaySeconds.toFloat(),
                    onValueChange = { 
                        delaySeconds = it.toInt()
                        SecurityPreferences.setChargingAlarmDelaySeconds(context, delaySeconds)
                    },
                    valueRange = 0f..30f,
                    steps = 29
                )
            }

            // Charging Monitor Diagnostics Section
            com.sentinelshield.antitheft.ui.components.RoundedCardContainer {
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
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Charging Monitor Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Check current power connection status, battery percentage, and unplug alarm response.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { filter ->
                                    context.registerReceiver(null, filter)
                                }
                                val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
                                val isCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING || status == android.os.BatteryManager.BATTERY_STATUS_FULL
                                val chargePlug = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, -1) ?: -1
                                val usbCharge = chargePlug == android.os.BatteryManager.BATTERY_PLUGGED_USB
                                val acCharge = chargePlug == android.os.BatteryManager.BATTERY_PLUGGED_AC
                                val wirelessCharge = chargePlug == android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS

                                val plugType = when {
                                    acCharge -> "AC Charger"
                                    usbCharge -> "USB Port"
                                    wirelessCharge -> "Wireless Pad"
                                    else -> "Not Plugged"
                                }

                                com.sentinelshield.antitheft.utils.DebugLogger.log(
                                    context,
                                    "ChargingDiagnostics",
                                    "Is Charging: $isCharging | Power Source: $plugType | Alarm Delay Config: ${delaySeconds}s"
                                )
                                android.widget.Toast.makeText(context, "Power Status logged: $plugType (Charging: $isCharging)", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Power", style = MaterialTheme.typography.bodySmall)
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
