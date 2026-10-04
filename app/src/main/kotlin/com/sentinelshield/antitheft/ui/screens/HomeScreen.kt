package com.sentinelshield.antitheft.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.navigation.NavController
import com.sentinelshield.antitheft.ui.components.FeatureCard
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isArmed: Boolean,
    isPocketArmed: Boolean,
    isChargingArmed: Boolean,
    isIntruderSelfieArmed: Boolean,
    isSmsControlArmed: Boolean,
    onToggleSimArm: (Boolean) -> Unit,
    onTogglePocketArm: (Boolean) -> Unit,
    onToggleChargingArm: (Boolean) -> Unit,
    onToggleIntruderSelfie: (Boolean) -> Unit,
    onToggleSmsControl: (Boolean) -> Unit,
    onRestoreRinger: () -> Unit,
    onOpenScreenShield: () -> Unit,
    onNavigateToPermissions: (String) -> Unit,
    navController: NavController
) {
    var showSmsInfoDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleTrigger = rememberLifecycleTrigger()
    val safeZoneVersion = rememberSafeZoneVersion()
    val effective = remember(safeZoneVersion, lifecycleTrigger, isPocketArmed, isChargingArmed) {
        com.sentinelshield.antitheft.ProtectionController.effective(context)
    }
    val powerManager = context.getSystemService(android.os.PowerManager::class.java)
    val hasPhonePermission = context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasNotificationPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val hasBatteryExemption = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M || powerManager.isIgnoringBatteryOptimizations(context.packageName)
    val hasAccessibilityAccess = remember(lifecycleTrigger) {
        android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(context.packageName) == true
    }
    var isFakeShutdownEnabled by remember(lifecycleTrigger) {
        mutableStateOf(com.sentinelshield.antitheft.SecurityPreferences.isFakeShutdownEnabled(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.sentinelshield.antitheft.R.drawable.ic_shield_3d),
                            contentDescription = "Sentinel Shield Logo",
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Sentinel Shield",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Category 1: Protection Control
            item {
                CategorySectionHeader(title = "Active Protection")
            }
            item {
                RoundedCardContainer {
                    FeatureCard(
                        title = "SIM Tamper Monitor",
                        description = "Foreground service listening for SIM card removal and state changes.",
                        iconRes = com.sentinelshield.antitheft.R.drawable.ic_feature_sim_tamper,
                        isChecked = isArmed,
                        onCheckedChange = { checked ->
                            if (checked && (!hasPhonePermission || !hasNotificationPermission)) {
                                val missing = if (!hasPhonePermission) "phone" else "notification"
                                onNavigateToPermissions(missing)
                            } else {
                                onToggleSimArm(checked)
                            }
                        },
                        onClick = {
                            navController.navigate(Screen.SimTamperSettings.route)
                        }
                    )

                    FeatureCard(
                        title = "Pocket Snatch Protection",
                        description = "Proximity sensor listener to trigger alarm when removed from pocket.",
                        iconRes = com.sentinelshield.antitheft.R.drawable.ic_feature_pocket_snatch,
                        isChecked = isPocketArmed,
                        statusBadgeText = when {
                            effective.pocketPausedByZone -> "Paused at home"
                            effective.pocketArmedByZone -> "Armed by Safe Zones"
                            else -> null
                        },
                        onCheckedChange = { checked ->
                            if (checked && (!hasNotificationPermission || !hasBatteryExemption)) {
                                val missing = if (!hasNotificationPermission) "notification" else "battery"
                                onNavigateToPermissions(missing)
                            } else {
                                onTogglePocketArm(checked)
                            }
                        },
                        onClick = {
                            navController.navigate(Screen.PocketSettings.route)
                        }
                    )

                    FeatureCard(
                        title = "Charging Monitor",
                        description = "Triggers alarm if charger is unplugged.",
                        iconRes = com.sentinelshield.antitheft.R.drawable.ic_feature_charger_disconnect,
                        isChecked = isChargingArmed,
                        statusBadgeText = when {
                            effective.chargingPausedByZone -> "Paused at home"
                            effective.chargingArmedByZone -> "Armed by Safe Zones"
                            else -> null
                        },
                        onCheckedChange = { checked ->
                            if (checked && !hasNotificationPermission) {
                                onNavigateToPermissions("notification")
                            } else {
                                onToggleChargingArm(checked)
                            }
                        },
                        onClick = {
                            navController.navigate(Screen.ChargingSettings.route)
                        }
                    )
                    
                    FeatureCard(
                        title = "Remote SMS Control",
                        description = "Send commands via SMS from trusted contacts.",
                        iconRes = com.sentinelshield.antitheft.R.drawable.ic_feature_remote_sms,
                        isChecked = isSmsControlArmed,
                        onCheckedChange = { checked ->
                            val hasSmsPerm = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            val hasContactPerm = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (checked && (!hasSmsPerm || !hasContactPerm)) {
                                onNavigateToPermissions("sms")
                            } else {
                                onToggleSmsControl(checked)
                            }
                        },
                        onClick = {
                            navController.navigate(Screen.RemoteSmsSettings.route)
                        },
                        onLongClick = {
                            showSmsInfoDialog = true
                        }
                    )
                }
            }

            item {
                CategorySectionHeader(title = "Privacy Actions")
            }
            item {
                RoundedCardContainer {
                    FeatureCard(
                        title = "Decoy Power Off",
                        description = "Intercepts the lock screen power menu with a realistic fake shutdown decoy.",
                        iconRes = com.sentinelshield.antitheft.R.drawable.ic_feature_decoy_power,
                        isChecked = hasAccessibilityAccess && isFakeShutdownEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !hasAccessibilityAccess) {
                                onNavigateToPermissions("accessibility")
                            } else {
                                isFakeShutdownEnabled = checked
                                com.sentinelshield.antitheft.SecurityPreferences.setFakeShutdownEnabled(context, checked)
                            }
                        },
                        onClick = onOpenScreenShield
                    )

                    FeatureCard(
                        title = "Intruder Selfie",
                        description = "Captures an image or video if the lock screen password is failed twice.",
                        icon = Icons.Default.CameraAlt,
                        iconColor = MaterialTheme.colorScheme.error,
                        isChecked = isIntruderSelfieArmed,
                        onCheckedChange = { checked ->
                            onToggleIntruderSelfie(checked)
                        },
                        onClick = {
                            navController.navigate(Screen.IntruderSettings.route)
                        }
                    )
                }
            }

            item {
                CategorySectionHeader(title = "Smart Automation")
            }
            item {
                val zoneStatus = remember(safeZoneVersion, lifecycleTrigger) { safeZoneUiStatus(context) }
                RoundedCardContainer {
                    FeatureCard(
                        title = "Safe Zones",
                        description = zoneStatus.detail,
                        icon = Icons.Default.LocationOn,
                        statusBadgeText = zoneStatus.badge,
                        statusBadgeColor = safeZoneToneColor(zoneStatus.tone),
                        onClick = { navController.navigate(Screen.SafeZones.route) }
                    )
                }
            }
        }
        
        if (showSmsInfoDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showSmsInfoDialog = false },
                title = { Text("Remote SMS Commands", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Authorized trusted contacts can send any of these SMS commands:", style = MaterialTheme.typography.bodyMedium)
                        Text("• LOCK (or LOCKDOWN / LOST)\n  Instantly locks the device screen.", style = MaterialTheme.typography.bodySmall)
                        Text("• SIREN (or ALARM / SOUND / RING)\n  Triggers anti-theft siren at max volume.", style = MaterialTheme.typography.bodySmall)
                        Text("• LOCATION (or TRACK / GPS / LOCATE / WHERE)\n  Enables location & mobile data and sends back the live Google Maps pin.", style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showSmsInfoDialog = false }) {
                        Text("Got it")
                    }
                }
            )
        }
    }
}

@Composable
fun CategorySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}
