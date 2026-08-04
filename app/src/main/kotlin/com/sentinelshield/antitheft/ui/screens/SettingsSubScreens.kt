package com.sentinelshield.antitheft.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PermDeviceInformation
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinelshield.antitheft.DeviceUtils
import com.sentinelshield.antitheft.ui.components.DeviceHeroCard
import com.sentinelshield.antitheft.ui.components.FeatureCard
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer
import com.sentinelshield.antitheft.ui.components.ThemePicker
import com.sentinelshield.antitheft.ui.theme.AppTheme
import com.sentinelshield.antitheft.ui.theme.DarkModeOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    hasPhonePermission: Boolean,
    hasNotificationPermission: Boolean,
    hasLocationPermission: Boolean,
    hasSmsPermission: Boolean,
    hasAccessibilityAccess: Boolean,
    hasNotificationListenerAccess: Boolean,
    hasDndAccess: Boolean,
    hasBatteryExemption: Boolean,
    hasCameraPermission: Boolean,
    hasOverlayPermission: Boolean,
    hasDeviceAdmin: Boolean,
    onRequestAppPermissions: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    onRequestCameraPermissions: () -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onRequestGoogleDriveConnect: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    isOnboarding: Boolean = false,
    onFinishOnboarding: (() -> Unit)? = null,
    highlightId: String? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOnboarding) "Welcome to Sentinel Shield" else "Permissions") },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
                RoundedCardContainer {
                    FeatureCard(
                        title = "App Permissions",
                        description = if (hasPhonePermission && hasNotificationPermission) "Phone state & Notifications granted" else "Action required: Grant required permissions",
                        icon = Icons.Default.PermDeviceInformation,
                        iconColor = if (hasPhonePermission && hasNotificationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasPhonePermission && hasNotificationPermission,
                        onCheckedChange = { onRequestAppPermissions() },
                        isHighlighted = highlightId == "phone" || highlightId == "notification"
                    )

                    FeatureCard(
                        title = "Location Access",
                        description = if (hasLocationPermission) "Location tracking is enabled" else "Required for device tracking during theft",
                        icon = Icons.Default.LocationOn,
                        iconColor = if (hasLocationPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasLocationPermission,
                        onCheckedChange = { onRequestLocationPermissions() }
                    )

                    FeatureCard(
                        title = "Accessibility Service",
                        description = if (hasAccessibilityAccess) "Fake Shutdown functionality enabled" else "Allows tracking device even if it's turned off",
                        icon = Icons.Default.Accessibility,
                        iconColor = if (hasAccessibilityAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasAccessibilityAccess,
                        onCheckedChange = { onOpenAccessibilitySettings() }
                    )

                    FeatureCard(
                        title = "Read Notification",
                        description = if (hasNotificationListenerAccess) "Remote control enabled" else "Control your phone remotely via notifications",
                        icon = Icons.Default.NotificationsActive,
                        iconColor = if (hasNotificationListenerAccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasNotificationListenerAccess,
                        onCheckedChange = { onOpenNotificationListenerSettings() }
                    )

                    FeatureCard(
                        title = "Contacts & SMS",
                        description = if (hasSmsPermission) "Emergency contact alerts enabled" else "Notify contacts during emergency via SMS",
                        icon = Icons.Default.Sms,
                        iconColor = if (hasSmsPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasSmsPermission,
                        onCheckedChange = { onRequestSmsPermission() }
                    )

                    FeatureCard(
                        title = "Background Battery / Autostart",
                        description = if (hasBatteryExemption) "Unrestricted battery usage allowed" else "Allow unrestricted battery and Autostart to prevent the OS from killing the alarm.",
                        icon = Icons.Default.BatteryFull,
                        iconColor = MaterialTheme.colorScheme.primary,
                        isChecked = hasBatteryExemption,
                        onCheckedChange = { onRequestBatteryExemption() },
                        isHighlighted = highlightId == "battery"
                    )

                    FeatureCard(
                        title = "Do Not Disturb Access",
                        description = if (hasDndAccess) "Access granted for ringer control" else "Grant DND policy access to restore alarm volume",
                        icon = Icons.Default.DoNotDisturbOn,
                        iconColor = MaterialTheme.colorScheme.secondary,
                        isChecked = hasDndAccess,
                        onCheckedChange = { onOpenDndSettings() }
                    )

                    FeatureCard(
                        title = "Camera Access",
                        description = if (hasCameraPermission) "Camera access granted" else "Required to take Intruder Selfies",
                        icon = Icons.Default.CameraAlt,
                        iconColor = if (hasCameraPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasCameraPermission,
                        onCheckedChange = { onRequestCameraPermissions() },
                        isHighlighted = highlightId == "camera"
                    )

                    FeatureCard(
                        title = "Display Over Other Apps",
                        description = if (hasOverlayPermission) "Overlay permission granted" else "Required to launch invisible capture on lock screen",
                        icon = Icons.Default.Layers,
                        iconColor = if (hasOverlayPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasOverlayPermission,
                        onCheckedChange = { onRequestOverlayPermission() },
                        isHighlighted = highlightId == "overlay"
                    )

                    FeatureCard(
                        title = "Device Admin",
                        description = if (hasDeviceAdmin) "Device admin active" else "Required to detect incorrect lock screen password attempts",
                        icon = Icons.Default.AdminPanelSettings,
                        iconColor = if (hasDeviceAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        isChecked = hasDeviceAdmin,
                        onCheckedChange = { onRequestDeviceAdmin() },
                        isHighlighted = highlightId == "device_admin"
                    )
                }
            if (isOnboarding) {
                Spacer(modifier = Modifier.padding(8.dp))
                androidx.compose.material3.Button(
                    onClick = { onFinishOnboarding?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    Text(
                        text = "Continue to App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    appTheme: AppTheme,
    darkMode: DarkModeOption,
    amoledMode: Boolean,
    useSystemFont: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onSystemFontChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val isDark = when (darkMode) {
        DarkModeOption.System -> androidx.compose.foundation.isSystemInDarkTheme()
        DarkModeOption.Light -> false
        DarkModeOption.Dark -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme & Appearance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RoundedCardContainer {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Dark Mode Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DarkModeOption.entries.forEach { option ->
                                val isSelected = darkMode == option
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onDarkModeChange(option) },
                                    label = {
                                        Text(
                                            text = option.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Text(
                            text = "Color Palette",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ThemePicker(
                            currentTheme = appTheme,
                            isDarkMode = isDark,
                            onThemeSelected = onThemeChange
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "AMOLED Pitch Black Mode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Uses pure #000000 black background.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = amoledMode,
                                onCheckedChange = onAmoledChange
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Use System Font",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Adapts to system font family settings.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useSystemFont,
                                onCheckedChange = onSystemFontChange
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    isArmed: Boolean,
    isPocketArmed: Boolean,
    lastAlert: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val deviceInfo = remember { DeviceUtils.getDeviceInfo(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DeviceHeroCard(
                    deviceInfo = deviceInfo
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Sentinel Shield",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Version 0.1.0",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Personal Device Anti-Theft Protection",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

private fun resolvePhoneNumberFromUri(context: android.content.Context, uri: android.net.Uri): String? {
    return runCatching {
        var resultNumber: String? = null
        
        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (idx != -1) {
                    resultNumber = cursor.getString(idx)
                }
            }
        }

        if (!resultNumber.isNullOrBlank()) return@runCatching resultNumber

        context.contentResolver.query(
            uri,
            arrayOf(android.provider.ContactsContract.Contacts._ID),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(android.provider.ContactsContract.Contacts._ID)
                if (idIdx != -1) {
                    val contactId = cursor.getString(idIdx)
                    context.contentResolver.query(
                        android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
                        "${android.provider.ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            val numIdx = phoneCursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                            if (numIdx != -1) {
                                resultNumber = phoneCursor.getString(numIdx)
                            }
                        }
                    }
                }
            }
        }
        resultNumber
    }.getOrElse { e ->
        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "ContactPicker", "Permission or query error: ${e.message}")
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteSmsSettingsScreen(
    isSmsControlArmed: Boolean,
    onToggleSmsControl: (Boolean) -> Unit,
    onNavigateToTrustedContacts: () -> Unit,
    onNavigateToDebugLogs: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAutoEnableEnabled = remember { androidx.compose.runtime.mutableStateOf(com.sentinelshield.antitheft.SecurityPreferences.isAutoEnableLocationDataEnabled(context)) }
    val showAdbDialog = remember { androidx.compose.runtime.mutableStateOf(false) }

    val adbCommandText = "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote SMS Control") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Control your device remotely via secure SMS commands sent from authorized trusted numbers.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.padding(bottom = 16.dp))

            RoundedCardContainer {
                // Option 1: Arm/Disarm Remote SMS Control
                SettingsSwitchItem(
                    title = "Remote SMS Protection",
                    subtitle = "Master switch to enable processing incoming SMS commands",
                    icon = Icons.Default.Sms,
                    isChecked = isSmsControlArmed,
                    onCheckedChange = { checked ->
                        val hasSmsPerm = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        val hasContactPerm = context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (checked && (!hasSmsPerm || !hasContactPerm)) {
                            android.widget.Toast.makeText(context, "Please grant SMS & Contacts permissions first.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onToggleSmsControl(checked)
                        }
                    }
                )

                // Option 2: Trusted Contacts Option Row
                SettingsRowItem(
                    title = "Trusted Contacts",
                    subtitle = "Manage phone numbers authorized to execute SMS commands",
                    icon = Icons.Default.People,
                    onClick = onNavigateToTrustedContacts
                )

                // Option 3: Auto Turn ON Location & Mobile Data Toggle
                SettingsSwitchItem(
                    title = "Auto-Turn ON Location & Data",
                    subtitle = "Automatically turn on Location & Data when a location command arrives",
                    icon = Icons.Default.LocationOn,
                    isChecked = isAutoEnableEnabled.value,
                    onCheckedChange = { checked ->
                        if (checked) {
                            val hasSecureSettings = context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (hasSecureSettings) {
                                isAutoEnableEnabled.value = true
                                com.sentinelshield.antitheft.SecurityPreferences.setAutoEnableLocationDataEnabled(context, true)
                                android.widget.Toast.makeText(context, "Auto Location & Data toggle enabled!", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                showAdbDialog.value = true
                            }
                        } else {
                            isAutoEnableEnabled.value = false
                            com.sentinelshield.antitheft.SecurityPreferences.setAutoEnableLocationDataEnabled(context, false)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.padding(bottom = 24.dp))

            // Remote SMS Diagnostics Section
            RoundedCardContainer {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Remote SMS Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Verify SMS permissions, WRITE_SECURE_SETTINGS status, and test command configuration.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val isArmed = com.sentinelshield.antitheft.SecurityPreferences.isSmsControlArmed(context)
                                val hasSecureSettings = context.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val savedContacts = com.sentinelshield.antitheft.SecurityPreferences.getTrustedContacts(context)

                                com.sentinelshield.antitheft.utils.DebugLogger.log(
                                    context,
                                    "RemoteSmsDiagnostics",
                                    "SMS Armed: $isArmed | WRITE_SECURE_SETTINGS: $hasSecureSettings | AutoEnable: ${isAutoEnableEnabled.value} | Trusted Contacts (${savedContacts.size}): $savedContacts",
                                    force = true
                                )
                                android.widget.Toast.makeText(context, "SMS Diagnostics logged! (Secure Settings: $hasSecureSettings)", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Config", style = MaterialTheme.typography.bodySmall)
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

    if (showAdbDialog.value) {
        AlertDialog(
            onDismissRequest = { showAdbDialog.value = false },
            icon = { Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("ADB System Permission Required", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "To allow SentinelShield to automatically turn on Location & Mobile Data when a remote location command arrives, grant the WRITE_SECURE_SETTINGS permission via ADB.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Run this command on your computer via ADB terminal:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = adbCommandText,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("ADB Command", adbCommandText)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "ADB Command copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Copy ADB Command")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdbDialog.value = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrustedContactsScreen(
    context: android.content.Context,
    onNavigateToDebugLogs: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val initialContacts = remember { com.sentinelshield.antitheft.SecurityPreferences.getTrustedContacts(context).toList() }
    val (contacts, setContacts) = remember { androidx.compose.runtime.mutableStateOf(initialContacts) }
    val (newContact, setNewContact) = remember { androidx.compose.runtime.mutableStateOf("") }
    
    fun updateContacts(newList: List<String>) {
        setContacts(newList)
        val currentSaved = com.sentinelshield.antitheft.SecurityPreferences.getTrustedContacts(context)
        currentSaved.forEach { com.sentinelshield.antitheft.SecurityPreferences.removeTrustedContact(context, it) }
        newList.forEach { com.sentinelshield.antitheft.SecurityPreferences.addTrustedContact(context, it) }
    }

    val contactPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        runCatching {
            if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    val number = resolvePhoneNumberFromUri(context, uri)
                    if (!number.isNullOrBlank()) {
                        val clean = number.trim()
                        if (!contacts.contains(clean)) {
                            updateContacts(contacts + clean)
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Selected contact has no phone number.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.onFailure { e ->
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "TrustedContacts", "Contact selection error: ${e.message}")
            android.widget.Toast.makeText(context, "Could not add contact.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trusted Contacts (SMS Control)") },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("These contacts can send Remote SMS commands:\n• LOCK (or LOCKDOWN / LOST)\n• SIREN (or ALARM / SOUND / RING)\n• LOCATION (or TRACK / GPS / LOCATE / WHERE)", 
                 style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.padding(bottom = 16.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = newContact,
                    onValueChange = { setNewContact(it) },
                    label = { Text("Phone Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.padding(end = 8.dp))
                androidx.compose.material3.Button(
                    onClick = {
                        runCatching {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_PICK,
                                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                            )
                            contactPickerLauncher.launch(intent)
                        }.onFailure { e ->
                            android.widget.Toast.makeText(context, "Could not open contacts app: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Pick")
                }
                Spacer(modifier = Modifier.padding(end = 8.dp))
                androidx.compose.material3.Button(onClick = {
                    if (newContact.isNotBlank() && !contacts.contains(newContact.trim())) {
                        updateContacts(contacts + newContact.trim())
                        setNewContact("")
                    }
                }) {
                    Text("Add")
                }
            }
            
            Spacer(modifier = Modifier.padding(bottom = 16.dp))
            
            // Remote SMS Diagnostics Section
            RoundedCardContainer {
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
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Remote SMS Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Verify SMS permissions, armed status, Device Admin policy, and simulate command matching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val isArmed = com.sentinelshield.antitheft.SecurityPreferences.isSmsControlArmed(context)
                                val hasReceiveSms = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECEIVE_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val hasSendSms = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                val dpm = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
                                val adminComponent = android.content.ComponentName(context, com.sentinelshield.antitheft.LockScreenAdminReceiver::class.java)
                                val isAdminActive = dpm?.isAdminActive(adminComponent) ?: false
                                val savedContacts = com.sentinelshield.antitheft.SecurityPreferences.getTrustedContacts(context)

                                com.sentinelshield.antitheft.utils.DebugLogger.log(
                                    context,
                                    "RemoteSmsDiagnostics",
                                    "SMS Armed: $isArmed | RECEIVE_SMS: $hasReceiveSms | SEND_SMS: $hasSendSms | DeviceAdmin: $isAdminActive | Trusted Contacts (${savedContacts.size}): $savedContacts",
                                    force = true
                                )
                                android.widget.Toast.makeText(context, "SMS Diagnostics logged! (${savedContacts.size} contact(s))", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test SMS Config", style = MaterialTheme.typography.bodySmall)
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
            
            Spacer(modifier = Modifier.padding(bottom = 16.dp))

            LazyColumn {
                items(contacts.size) { index ->
                    val contact = contacts[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(contact, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { updateContacts(contacts.filter { it != contact }) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimTamperSettingsScreen(
    initialContact: String,
    onSave: (String) -> Unit,
    onNavigateToRingtone: () -> Unit,
    onNavigateToDebugLogs: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val (contact, setContact) = remember { androidx.compose.runtime.mutableStateOf(initialContact) }

    val contactPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        runCatching {
            if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
                val uri = result.data?.data
                if (uri != null) {
                    val number = resolvePhoneNumberFromUri(context, uri)
                    if (!number.isNullOrBlank()) {
                        setContact(number.trim())
                    } else {
                        android.widget.Toast.makeText(context, "Selected contact has no phone number.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.onFailure { e ->
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "ContactPicker", "SIM Tamper Contact pick error: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIM Tamper Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Set an emergency contact to receive an SMS alert if your SIM card is removed or changed.", 
                 style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.padding(bottom = 16.dp))
            
            com.sentinelshield.antitheft.ui.components.RoundedCardContainer {
                SettingsRowItem(
                    title = "Alarm Sound",
                    subtitle = "Choose loud built-in or custom alarms",
                    icon = androidx.compose.material.icons.Icons.Default.VolumeUp,
                    onClick = onNavigateToRingtone
                )
            }
            Spacer(modifier = Modifier.padding(bottom = 16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = contact,
                    onValueChange = { setContact(it) },
                    label = { Text("Emergency Phone Number") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.padding(end = 8.dp))
                androidx.compose.material3.Button(onClick = {
                    runCatching {
                        val intent = android.content.Intent(android.content.Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPickerLauncher.launch(intent)
                    }.onFailure { e ->
                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "ContactPicker", "Failed to launch contact picker: ${e.message}")
                    }
                }) {
                    Text("Pick")
                }
            }
            
            Spacer(modifier = Modifier.padding(top = 24.dp))
            androidx.compose.material3.Button(
                onClick = {
                    try {
                        val subManager = context.getSystemService(android.telephony.SubscriptionManager::class.java)
                        val activeSubs = subManager.activeSubscriptionInfoList
                        val currentIds = activeSubs?.map { it.subscriptionId.toString() }?.toSet() ?: emptySet()
                        com.sentinelshield.antitheft.SecurityPreferences.setSavedSubscriptionIds(context, currentIds)
                        android.widget.Toast.makeText(context, "Registered ${currentIds.size} current SIM(s) as trusted.", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: SecurityException) {
                        android.widget.Toast.makeText(context, "Permission denied.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register Current SIMs as Trusted")
            }
            
            Spacer(modifier = Modifier.padding(top = 16.dp))
            androidx.compose.material3.Button(
                onClick = { onSave(contact) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Settings")
            }

            Spacer(modifier = Modifier.padding(top = 24.dp))

            // SIM Tamper Diagnostics Section
            RoundedCardContainer {
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
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "SIM Tamper Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Query active physical/eSIM subscriptions, examine registered SIM snapshots, and verify state change listeners.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                try {
                                    val subManager = context.getSystemService(android.telephony.SubscriptionManager::class.java)
                                    val activeSubs = subManager?.activeSubscriptionInfoList
                                    val currentIds = activeSubs?.map { "slot${it.simSlotIndex}_sub${it.subscriptionId}" }?.toSet() ?: emptySet()
                                    val saved = com.sentinelshield.antitheft.SecurityPreferences.getSavedSubscriptionIds(context)
                                    com.sentinelshield.antitheft.utils.DebugLogger.log(
                                        context,
                                        "SimTamperDiagnostics",
                                        "Detected Active SIMs: $currentIds | Trusted Snapshot: $saved | Emergency Contact: ${if (contact.isNotBlank()) contact else "NONE"}"
                                    )
                                    android.widget.Toast.makeText(context, "SIM diagnostic logged! Found ${currentIds.size} active slot(s).", android.widget.Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimTamperDiagnostics", "Failed: ${e.message}")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test SIMs", style = MaterialTheme.typography.bodySmall)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDebugEnabled = remember { androidx.compose.runtime.mutableStateOf(com.sentinelshield.antitheft.SecurityPreferences.isDebugLoggingEnabled(context)) }
    val logsText = remember { androidx.compose.runtime.mutableStateOf(com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = {
                        com.sentinelshield.antitheft.utils.DebugLogger.shareLogs(context)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        com.sentinelshield.antitheft.utils.DebugLogger.clearLogs(context)
                        logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                        android.widget.Toast.makeText(context, "Logs cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            RoundedCardContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Diagnostic Logging",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record low-overhead events to diagnose device issues",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDebugEnabled.value,
                        onCheckedChange = { enabled ->
                            isDebugEnabled.value = enabled
                            com.sentinelshield.antitheft.SecurityPreferences.setDebugLoggingEnabled(context, enabled)
                            if (enabled) {
                                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "DebugLogsScreen", "Diagnostic logging enabled by user")
                            }
                            logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.padding(top = 16.dp))

            RoundedCardContainer {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Feature Diagnostic Simulations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                if (!isDebugEnabled.value) {
                                    isDebugEnabled.value = true
                                    com.sentinelshield.antitheft.SecurityPreferences.setDebugLoggingEnabled(context, true)
                                }
                                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimTamperTest", "Manual SIM Diagnostic Triggered", force = true)
                                try {
                                    val subManager = context.getSystemService(android.telephony.SubscriptionManager::class.java)
                                    val activeSubs = subManager?.activeSubscriptionInfoList
                                    val currentIds = activeSubs?.map { "slot${it.simSlotIndex}_sub${it.subscriptionId}" }?.toSet() ?: emptySet()
                                    val saved = com.sentinelshield.antitheft.SecurityPreferences.getSavedSubscriptionIds(context)
                                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimTamperTest", "Current SIMs: $currentIds | Saved Trusted SIMs: $saved", force = true)
                                } catch (e: Exception) {
                                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimTamperTest", "SIM query failed: ${e.message}", force = true)
                                }
                                logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                                android.widget.Toast.makeText(context, "SIM Diagnostic Executed & Logged Below!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test SIM", style = MaterialTheme.typography.bodySmall)
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                if (!isDebugEnabled.value) {
                                    isDebugEnabled.value = true
                                    com.sentinelshield.antitheft.SecurityPreferences.setDebugLoggingEnabled(context, true)
                                }
                                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "ChargingTest", "Manual Charger Unplug Test Alarm Triggered", force = true)
                                com.sentinelshield.antitheft.SecurityAlertService.start(context, "Charger Unplug Test Alarm")
                                try {
                                    val disarmIntent = android.content.Intent(context, com.sentinelshield.antitheft.DisarmActivity::class.java).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(disarmIntent)
                                } catch (_: Exception) {}
                                logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                                android.widget.Toast.makeText(context, "🚨 Charger Unplug Test Alarm Ringing!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Charger", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.padding(top = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                if (!isDebugEnabled.value) {
                                    isDebugEnabled.value = true
                                    com.sentinelshield.antitheft.SecurityPreferences.setDebugLoggingEnabled(context, true)
                                }
                                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "PocketTest", "Manual Pocket Snatch Test Alarm Triggered", force = true)
                                com.sentinelshield.antitheft.SecurityAlertService.start(context, "Pocket Snatch Test Alarm")
                                try {
                                    val disarmIntent = android.content.Intent(context, com.sentinelshield.antitheft.DisarmActivity::class.java).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(disarmIntent)
                                } catch (_: Exception) {}
                                logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                                android.widget.Toast.makeText(context, "🚨 Pocket Snatch Test Alarm Ringing!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Pocket", style = MaterialTheme.typography.bodySmall)
                        }

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                if (!isDebugEnabled.value) {
                                    isDebugEnabled.value = true
                                    com.sentinelshield.antitheft.SecurityPreferences.setDebugLoggingEnabled(context, true)
                                }
                                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "IntruderTest", "Manual Intruder Selfie Diagnostic Triggered", force = true)
                                val hasCam = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (!hasCam) {
                                    android.widget.Toast.makeText(context, "⚠️ Camera Permission Required! Please grant Camera permission.", android.widget.Toast.LENGTH_LONG).show()
                                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "IntruderTest", "Camera permission missing during test trigger", force = true)
                                } else {
                                    try {
                                        val intent = android.content.Intent(context, com.sentinelshield.antitheft.IntruderActivity::class.java).apply {
                                            putExtra("IS_TEST_MODE", true)
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                        android.widget.Toast.makeText(context, "📸 Capturing test selfie/video in background...", android.widget.Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "IntruderTest", "Intruder launch failed: ${e.message}", force = true)
                                    }
                                }
                                logsText.value = com.sentinelshield.antitheft.utils.DebugLogger.getLogs(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Intruder", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(top = 16.dp))

            Text(
                text = "Diagnostic Log Viewer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = logsText.value,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
