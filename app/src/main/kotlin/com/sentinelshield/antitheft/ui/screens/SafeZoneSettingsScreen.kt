package com.sentinelshield.antitheft.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinelshield.antitheft.ProtectionController
import com.sentinelshield.antitheft.safezone.GeofenceRegistrar
import com.sentinelshield.antitheft.safezone.SafeZone
import com.sentinelshield.antitheft.safezone.SafeZoneStore
import com.sentinelshield.antitheft.safezone.WifiMatcher
import com.sentinelshield.antitheft.safezone.Zone
import com.sentinelshield.antitheft.ui.components.FeatureCard
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private const val MAX_HOME_FIX_ACCURACY_M = 100f
private const val PAUSE_DURATION_MS = 60L * 60L * 1000L

/** Recomposes the caller whenever Safe Zones state changes (zone, rules, Wi-Fi, pause). */
@Composable
fun rememberSafeZoneVersion(): Int {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(SafeZoneStore.FILE_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> version++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return version
}

enum class SafeZoneTone { GOOD, ACTIVE, NEUTRAL, WARNING }

/** Short badge plus one line of detail describing what Safe Zones is doing right now. */
data class SafeZoneUiStatus(val badge: String, val detail: String, val tone: SafeZoneTone)

fun safeZoneUiStatus(context: Context): SafeZoneUiStatus {
    val enabled = SafeZoneStore.isEnabled(context)
    val hasZone = SafeZoneStore.zones(context).isNotEmpty()
    return when {
        !enabled -> SafeZoneUiStatus("Off", "Automatically relax or arm protection when you are home or away.", SafeZoneTone.NEUTRAL)
        !hasZone -> SafeZoneUiStatus("Set up", "Set your home location to start.", SafeZoneTone.WARNING)
        SafeZoneStore.isPaused(context) -> {
            val until = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(SafeZoneStore.pausedUntil(context)))
            SafeZoneUiStatus("Paused", "Automation is paused until $until.", SafeZoneTone.NEUTRAL)
        }
        else -> when (SafeZoneStore.currentZone(context)) {
            Zone.HOME -> {
                val via = if (SafeZoneStore.wifiConnected(context) == true) "home Wi-Fi" else "location"
                SafeZoneUiStatus("Home", "At home (detected by $via).", SafeZoneTone.GOOD)
            }
            Zone.AWAY -> SafeZoneUiStatus("Away", "You are away from home. Protection is armed.", SafeZoneTone.ACTIVE)
            Zone.UNKNOWN -> SafeZoneUiStatus("Unknown", "Waiting for a location or Wi-Fi signal.", SafeZoneTone.WARNING)
        }
    }
}

@Composable
fun safeZoneToneColor(tone: SafeZoneTone): Color = when (tone) {
    SafeZoneTone.GOOD -> Color(0xFF4CAF50)
    SafeZoneTone.ACTIVE -> MaterialTheme.colorScheme.primary
    SafeZoneTone.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    SafeZoneTone.WARNING -> MaterialTheme.colorScheme.error
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeZoneSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleTrigger = rememberLifecycleTrigger()
    val version = rememberSafeZoneVersion()
    var busy by remember { mutableStateOf(false) }
    var permissionVersion by remember { mutableIntStateOf(0) }
    val key = Triple(lifecycleTrigger, version, permissionVersion)

    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    // Play services forgets geofences in several situations; restore them whenever this screen opens.
    LaunchedEffect(lifecycleTrigger) {
        if (SafeZoneStore.isActive(context)) GeofenceRegistrar.registerAll(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionVersion++
        if (SafeZoneStore.isActive(context)) GeofenceRegistrar.registerAll(context)
    }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionVersion++
        if (SafeZoneStore.isActive(context)) GeofenceRegistrar.registerAll(context)
    }

    val enabled = remember(key) { SafeZoneStore.isEnabled(context) }
    val zone = remember(key) { SafeZoneStore.zones(context).firstOrNull() }
    val rules = remember(key) { SafeZoneStore.getRules(context) }
    val status = remember(key) { safeZoneUiStatus(context) }
    val paused = remember(key) { SafeZoneStore.isPaused(context) }
    val note = remember(key) { SafeZoneStore.note(context) }
    val hasFine = remember(key) { GeofenceRegistrar.hasFineLocation(context) }
    val hasBackground = remember(key) { GeofenceRegistrar.hasBackgroundLocation(context) }
    val playServices = remember(key) { GeofenceRegistrar.isPlayServicesAvailable(context) }
    val locationOn = remember(key) {
        val manager = context.getSystemService(LocationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) manager?.isLocationEnabled == true else true
    }

    fun applyChange() {
        ProtectionController.refreshZone(context, force = true)
    }

    fun setEnabled(on: Boolean) {
        SafeZoneStore.setEnabled(context, on)
        if (on) GeofenceRegistrar.registerAll(context) else GeofenceRegistrar.removeAll(context)
        applyChange()
    }

    fun saveHomeFromFix(latitude: Double, longitude: Double) {
        val existing = SafeZoneStore.zones(context).firstOrNull()
        val updated = (existing ?: SafeZone(name = "Home", lat = latitude, lng = longitude))
            .copy(lat = latitude, lng = longitude)
        SafeZoneStore.upsertZone(context, updated)
        // The owner is standing at home right now, so inside is the starting verdict.
        SafeZoneStore.setGeoInside(context, updated.id, true)
        if (SafeZoneStore.isEnabled(context)) GeofenceRegistrar.registerAll(context)
        applyChange()
    }

    fun useCurrentLocation() {
        if (!hasFine) {
            toast("Allow precise location first.")
            return
        }
        busy = true
        GeofenceRegistrar.fetchFreshLocation(context) { location ->
            busy = false
            when {
                location == null -> toast("Could not get your location. Turn Location on and try near a window.")
                GeofenceRegistrar.isMock(location) -> toast("Mock locations cannot be used to set home.")
                location.accuracy > MAX_HOME_FIX_ACCURACY_M ->
                    toast("The location is not accurate enough (about ${location.accuracy.toInt()} m). Move near a window and try again.")
                else -> {
                    saveHomeFromFix(location.latitude, location.longitude)
                    toast("Home location saved.")
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    fun useCurrentWifi() {
        val home = SafeZoneStore.zones(context).firstOrNull()
        if (home == null) {
            toast("Set your home location first.")
            return
        }
        val info = context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo
        val bssid = WifiMatcher.normalizeBssid(info?.bssid)
        val ssid = WifiMatcher.normalizeSsid(info?.ssid)
        if (bssid == null || ssid == null) {
            toast("Cannot read the Wi-Fi details. Connect to your home Wi-Fi, allow precise location and turn Location on.")
            return
        }
        val known = if (ssid == home.ssid) home.bssids else emptySet()
        SafeZoneStore.upsertZone(context, home.copy(ssid = ssid, bssids = known + bssid))
        SafeZoneStore.setWifiConnected(context, true)
        SafeZoneStore.setWifiLostAt(context, null)
        applyChange()
        toast("Home Wi-Fi saved: $ssid")
    }

    fun forgetWifi() {
        val home = SafeZoneStore.zones(context).firstOrNull() ?: return
        SafeZoneStore.upsertZone(context, home.copy(ssid = null, bssids = emptySet()))
        SafeZoneStore.setWifiConnected(context, null)
        SafeZoneStore.setWifiLostAt(context, null)
        applyChange()
    }

    fun removeHome() {
        val home = SafeZoneStore.zones(context).firstOrNull() ?: return
        SafeZoneStore.removeZone(context, home.id)
        GeofenceRegistrar.removeAll(context)
        SafeZoneStore.setWifiConnected(context, null)
        SafeZoneStore.setWifiLostAt(context, null)
        applyChange()
    }

    fun updateRules(transform: (com.sentinelshield.antitheft.safezone.ZoneRules) -> com.sentinelshield.antitheft.safezone.ZoneRules) {
        SafeZoneStore.setRules(context, transform(SafeZoneStore.getRules(context)))
        applyChange()
    }

    fun openMap(home: SafeZone) {
        val uri = Uri.parse("geo:${home.lat},${home.lng}?q=${home.lat},${home.lng}(Home)")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            toast("No map app is installed.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safe Zones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Master switch and live status
            RoundedCardContainer {
                FeatureCard(
                    title = "Safe Zones",
                    description = status.detail,
                    icon = Icons.Default.Home,
                    isChecked = enabled,
                    onCheckedChange = { setEnabled(it) },
                    statusBadgeText = status.badge,
                    statusBadgeColor = safeZoneToneColor(status.tone),
                )
            }

            if (note.isNotBlank()) {
                NoticeCard(note)
            }

            // Permissions
            if (enabled) {
                if (!playServices) {
                    NoticeCard("Google Play services is not available on this device. Safe Zones works with home Wi-Fi only.")
                }
                if (!hasFine) {
                    NoticeCard(
                        text = "Step 1 of 2: Allow precise location so Sentinel Shield can tell when you are home.",
                        actionLabel = "Allow precise location",
                        onAction = {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                        },
                    )
                } else if (!hasBackground) {
                    NoticeCard(
                        text = "Step 2 of 2: Choose \"Allow all the time\" so location changes are noticed while the app is closed.",
                        actionLabel = "Allow all the time",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                // Android 11+ sends the user to the permission page in Settings.
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            } else {
                                backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        },
                        secondaryLabel = "Open app settings",
                        onSecondary = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                            )
                        },
                    )
                } else if (!locationOn) {
                    NoticeCard("Location is turned off. Turn it on so Safe Zones can tell where you are.")
                }
            }

            // Home location
            SectionTitle("Home location")
            RoundedCardContainer {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                text = if (zone == null) "Not set" else "Home is set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (zone == null) {
                                    "Stand at home, then tap the button below."
                                } else {
                                    String.format(Locale.US, "%.5f, %.5f", zone.lat, zone.lng)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Button(onClick = { useCurrentLocation() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                        Text(if (busy) "Getting location..." else "Use my current location")
                    }
                    if (zone != null) {
                        var sliderValue by remember(zone.id, zone.radiusM) { mutableStateOf(zone.radiusM) }
                        Text(
                            text = "Zone size: ${sliderValue.toInt()} m",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = SafeZoneStore.MIN_RADIUS_M..SafeZoneStore.MAX_RADIUS_M,
                            steps = 7,
                            onValueChangeFinished = {
                                SafeZoneStore.upsertZone(context, zone.copy(radiusM = sliderValue))
                                if (SafeZoneStore.isEnabled(context)) GeofenceRegistrar.registerAll(context)
                                applyChange()
                            },
                        )
                        Text(
                            text = "Larger zones are more reliable. Small ones can miss you arriving or leaving.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { openMap(zone) }) { Text("Check on map") }
                            TextButton(onClick = { removeHome() }) { Text("Remove home") }
                        }
                    }
                }
            }

            // Home Wi-Fi
            SectionTitle("Home Wi-Fi")
            RoundedCardContainer {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(
                                text = zone?.ssid ?: "Not set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (zone?.ssid == null) {
                                    "Connected to your home Wi-Fi? Save it so leaving is noticed within seconds."
                                } else {
                                    val count = zone.bssids.size
                                    "$count access point${if (count == 1) "" else "s"} remembered"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { useCurrentWifi() }, enabled = zone != null) { Text("Use current Wi-Fi") }
                        if (zone?.ssid != null) {
                            TextButton(onClick = { forgetWifi() }) { Text("Forget") }
                        }
                    }
                    if (zone?.ssid != null) {
                        Text(
                            text = "Keep Wi-Fi switched on at home. If it is off, you will be treated as away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Rules
            SectionTitle("When I am home, pause")
            RoundedCardContainer {
                SettingsSwitchItem(
                    title = "Pocket snatch protection",
                    subtitle = "Stops false alarms while you move around the house.",
                    icon = Icons.Default.Vibration,
                    isChecked = rules.pausePocketAtHome,
                    onCheckedChange = { value -> updateRules { it.copy(pausePocketAtHome = value) } },
                )
                SettingsSwitchItem(
                    title = "Charger unplug alarm",
                    subtitle = "Unplugging at home will not sound the alarm.",
                    icon = Icons.Default.PlayArrow,
                    isChecked = rules.pauseChargingAtHome,
                    onCheckedChange = { value -> updateRules { it.copy(pauseChargingAtHome = value) } },
                )
            }
            Text(
                text = "SIM tamper and Intruder selfie are never paused at home.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("When I leave, arm")
            RoundedCardContainer {
                SettingsSwitchItem(
                    title = "Pocket snatch protection",
                    subtitle = "Armed while you are away, then back to your own setting at home.",
                    icon = Icons.Default.Vibration,
                    isChecked = rules.armPocketWhenAway,
                    onCheckedChange = { value -> updateRules { it.copy(armPocketWhenAway = value) } },
                )
                SettingsSwitchItem(
                    title = "Charger unplug alarm",
                    subtitle = "Armed while you are away, then back to your own setting at home.",
                    icon = Icons.Default.PlayArrow,
                    isChecked = rules.armChargingWhenAway,
                    onCheckedChange = { value -> updateRules { it.copy(armChargingWhenAway = value) } },
                )
                SettingsSwitchItem(
                    title = "SIM tamper monitor",
                    subtitle = "Turns on when you leave and stays on until you turn it off.",
                    icon = Icons.Default.Sms,
                    isChecked = rules.armSimWhenAway,
                    onCheckedChange = { value -> updateRules { it.copy(armSimWhenAway = value) } },
                )
                SettingsSwitchItem(
                    title = "Intruder selfie",
                    subtitle = "Turns on when you leave and stays on until you turn it off.",
                    icon = Icons.Default.LocationOn,
                    isChecked = rules.armIntruderWhenAway,
                    onCheckedChange = { value -> updateRules { it.copy(armIntruderWhenAway = value) } },
                )
            }

            // Manual pause
            SectionTitle("Manual control")
            RoundedCardContainer {
                if (paused) {
                    SettingsRowItem(
                        title = "Resume automation",
                        subtitle = status.detail,
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            SafeZoneStore.setPausedUntil(context, 0L)
                            applyChange()
                        },
                    )
                } else {
                    SettingsRowItem(
                        title = "Pause for 1 hour",
                        subtitle = "Your own switches apply until the hour is up.",
                        icon = Icons.Default.Pause,
                        onClick = {
                            SafeZoneStore.setPausedUntil(context, System.currentTimeMillis() + PAUSE_DURATION_MS)
                            applyChange()
                        },
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Text(
                text = "Zone changes are noticed from Wi-Fi within seconds and from location within a few minutes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun NoticeCard(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAction) { Text(actionLabel) }
                    if (secondaryLabel != null && onSecondary != null) {
                        TextButton(onClick = onSecondary) { Text(secondaryLabel) }
                    }
                }
            }
        }
    }
}
