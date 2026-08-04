package com.sentinelshield.antitheft.ui.screens

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sentinelshield.antitheft.ui.theme.AppTheme
import com.sentinelshield.antitheft.ui.theme.DarkModeOption

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Dashboard", Icons.Default.Home)
    data object Settings : Screen("settings_menu", "Settings", Icons.Default.Settings)
    
    // Sub-screens
    data object Permissions : Screen("settings_permissions", "Permissions")
    data object RingtoneSelection : Screen("settings_ringtone", "Alarm Sound")
    data object Theme : Screen("settings_theme", "Theme")
    data object DeviceInfo : Screen("settings_device_info", "Device Info")
    data object TrustedContacts : Screen("settings_trusted_contacts", "Trusted Contacts")
    data object RemoteSmsSettings : Screen("settings_remote_sms", "Remote SMS Control")
    data object SimTamperSettings : Screen("settings_sim_tamper", "SIM Tamper Settings")
    data object ChargingSettings : Screen("charging_settings", "Charging Monitor Settings")
    data object PocketSettings : Screen("pocket_settings", "Pocket Snatch Settings")
    data object IntruderSettings : Screen("intruder_settings", "Intruder Selfie Settings")
    data object LiveMap : Screen("settings_live_map", "Live Tracking & Maps")
    data object GoogleDriveSettings : Screen("settings_google_drive", "Google Drive Cloud Backup")
    data object DebugLogs : Screen("settings_debug_logs", "Debug & Diagnostics")
    data object About : Screen("settings_about", "About")
}

@Composable
fun rememberLifecycleTrigger(): Int {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val trigger = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                trigger.intValue++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return trigger.intValue
}

@Composable
fun AppNavigation(
    context: Context,
    isArmed: Boolean,
    isPocketArmed: Boolean,
    isChargingArmed: Boolean,
    isIntruderSelfieArmed: Boolean,
    isSmsControlArmed: Boolean,
    lastAlert: String,
    appTheme: AppTheme,
    darkMode: DarkModeOption,
    amoledMode: Boolean,
    useSystemFont: Boolean,
    onThemeChange: (AppTheme) -> Unit,
    onDarkModeChange: (DarkModeOption) -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onSystemFontChange: (Boolean) -> Unit,
    onRequestAppPermissions: () -> Unit,
    onRequestLocationPermissions: () -> Unit,
    onRequestCameraPermissions: () -> Unit,
    onToggleSimArm: (Boolean) -> Unit,
    onTogglePocketArm: (Boolean) -> Unit,
    onToggleChargingArm: (Boolean) -> Unit,
    onToggleIntruderSelfie: (Boolean) -> Unit,
    onToggleSmsControl: (Boolean) -> Unit,
    onRequestBatteryExemption: () -> Unit,
    onOpenDndSettings: () -> Unit,
    onRestoreRinger: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onRequestSmsPermission: () -> Unit,
    onRequestGoogleDriveConnect: () -> Unit,
    onOpenScreenShield: () -> Unit
) {
    val navController = rememberNavController()

    val bottomNavItems = listOf(Screen.Home, Screen.Settings)

    val hasCompletedOnboarding = com.sentinelshield.antitheft.SecurityPreferences.hasCompletedOnboarding(context)
    val startDest = if (hasCompletedOnboarding) Screen.Home.route else "onboarding"

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom nav on main tabs
            val showBottomNav = bottomNavItems.any { it.route == currentDestination?.route }
            
            if (showBottomNav) {
                com.sentinelshield.antitheft.ui.components.SentinelFloatingToolbar(
                    items = bottomNavItems.map { com.sentinelshield.antitheft.ui.components.SentinelNavItem(it.route, it.title, it.icon!!) },
                    currentRoute = currentDestination?.route,
                    onItemClick = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 140))
            },
            exitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 140))
            },
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { fullWidth -> -(fullWidth * 0.25f).toInt() },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 140))
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 180, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 140))
            }
        ) {
            composable("onboarding") {
                val refresh = rememberLifecycleTrigger()
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                val powerManager = context.getSystemService(android.os.PowerManager::class.java)
                val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                val hasDndAccess = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted }
                val hasPhonePermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED }
                val hasNotificationPermission = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED }
                val hasBatteryExemption = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || powerManager.isIgnoringBatteryOptimizations(context.packageName) }
                val hasLocationPermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED }
                val hasSmsPermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED }
                val hasExactAlarmPermission = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms() }
                val hasAccessibilityAccess = remember(refresh) { Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(context.packageName) == true }
                val hasNotificationListenerAccess = remember(refresh) { androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }
                val hasCameraPermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED }
                val hasOverlayPermission = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context) }
                val devicePolicyManager = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
                val adminComponent = android.content.ComponentName(context, com.sentinelshield.antitheft.LockScreenAdminReceiver::class.java)
                val hasDeviceAdmin = remember(refresh) { devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent) }

                PermissionsScreen(
                    hasPhonePermission = hasPhonePermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasLocationPermission = hasLocationPermission,
                    hasSmsPermission = hasSmsPermission,
                    hasAccessibilityAccess = hasAccessibilityAccess,
                    hasNotificationListenerAccess = hasNotificationListenerAccess,
                    hasDndAccess = hasDndAccess,
                    hasBatteryExemption = hasBatteryExemption,
                    onRequestAppPermissions = onRequestAppPermissions,
                    onRequestLocationPermissions = onRequestLocationPermissions,
                    onRequestCameraPermissions = onRequestCameraPermissions,
                    onRequestBatteryExemption = onRequestBatteryExemption,
                    onOpenDndSettings = onOpenDndSettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenNotificationListenerSettings = onOpenNotificationListenerSettings,
                    onRequestSmsPermission = onRequestSmsPermission,
                    hasCameraPermission = hasCameraPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    hasDeviceAdmin = hasDeviceAdmin,
                    onRequestOverlayPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    },
                    onRequestDeviceAdmin = {
                        if (!hasDeviceAdmin) {
                            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to detect incorrect password attempts for Intruder Selfie.")
                            }
                            context.startActivity(intent)
                        }
                    },
                    onRequestGoogleDriveConnect = onRequestGoogleDriveConnect,
                    onBack = null,
                    isOnboarding = true,
                    onFinishOnboarding = {
                        com.sentinelshield.antitheft.SecurityPreferences.setHasCompletedOnboarding(context, true)
                        navController.navigate(Screen.Home.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            // Tab 1: Home Dashboard
            composable(Screen.Home.route) {
                HomeScreen(
                    isArmed = isArmed,
                    isPocketArmed = isPocketArmed,
                    isChargingArmed = isChargingArmed,
                    isIntruderSelfieArmed = isIntruderSelfieArmed,
                    isSmsControlArmed = isSmsControlArmed,
                    onToggleSimArm = onToggleSimArm,
                    onTogglePocketArm = onTogglePocketArm,
                    onToggleChargingArm = onToggleChargingArm,
                    onToggleIntruderSelfie = onToggleIntruderSelfie,
                    onToggleSmsControl = onToggleSmsControl,
                    onRestoreRinger = onRestoreRinger,
                    onOpenScreenShield = onOpenScreenShield,
                    onNavigateToPermissions = { highlightId ->
                        navController.navigate("${Screen.Permissions.route}?highlight=$highlightId")
                    },
                    navController = navController
                )
            }

            // Tab 2: Settings Menu
            composable(Screen.Settings.route) {
                SettingsMenuScreen(
                    onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                    onNavigateToTheme = { navController.navigate(Screen.Theme.route) },
                    onNavigateToDeviceInfo = { navController.navigate(Screen.DeviceInfo.route) },
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onNavigateToGoogleDrive = { navController.navigate(Screen.GoogleDriveSettings.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }
            
            // Sub-screen: Permissions
            composable(
                route = "${Screen.Permissions.route}?highlight={highlightId}",
                arguments = listOf(navArgument("highlightId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val refresh = rememberLifecycleTrigger()
                val highlightId = backStackEntry.arguments?.getString("highlightId")
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                val powerManager = context.getSystemService(android.os.PowerManager::class.java)
                val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                val hasDndAccess = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || notificationManager.isNotificationPolicyAccessGranted }
                val hasPhonePermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED }
                val hasNotificationPermission = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED }
                val hasBatteryExemption = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || powerManager.isIgnoringBatteryOptimizations(context.packageName) }
                val hasLocationPermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED }
                val hasSmsPermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED }
                val hasCameraPermission = remember(refresh) { context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED }
                val hasOverlayPermission = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context) }
                
                val devicePolicyManager = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
                val adminComponent = android.content.ComponentName(context, com.sentinelshield.antitheft.LockScreenAdminReceiver::class.java)
                val hasDeviceAdmin = remember(refresh) { devicePolicyManager != null && devicePolicyManager.isAdminActive(adminComponent) }
                
                val hasExactAlarmPermission = remember(refresh) { Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms() }
                val hasAccessibilityAccess = remember(refresh) { Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(context.packageName) == true }
                val hasNotificationListenerAccess = remember(refresh) { androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) }

                PermissionsScreen(
                    hasPhonePermission = hasPhonePermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasLocationPermission = hasLocationPermission,
                    hasSmsPermission = hasSmsPermission,
                    hasAccessibilityAccess = hasAccessibilityAccess,
                    hasNotificationListenerAccess = hasNotificationListenerAccess,
                    hasDndAccess = hasDndAccess,
                    hasBatteryExemption = hasBatteryExemption,
                    hasCameraPermission = hasCameraPermission,
                    hasOverlayPermission = hasOverlayPermission,
                    hasDeviceAdmin = hasDeviceAdmin,
                    onRequestAppPermissions = onRequestAppPermissions,
                    onRequestLocationPermissions = onRequestLocationPermissions,
                    onRequestCameraPermissions = onRequestCameraPermissions,
                    onRequestBatteryExemption = onRequestBatteryExemption,
                    onOpenDndSettings = onOpenDndSettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onOpenNotificationListenerSettings = onOpenNotificationListenerSettings,
                    onRequestSmsPermission = onRequestSmsPermission,
                    onRequestOverlayPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }
                    },
                    onRequestDeviceAdmin = {
                        if (!hasDeviceAdmin) {
                            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to detect incorrect password attempts for Intruder Selfie.")
                            }
                            context.startActivity(intent)
                        }
                    },
                    onRequestGoogleDriveConnect = onRequestGoogleDriveConnect,
                    onBack = { navController.popBackStack() },
                    highlightId = highlightId
                )
            }

            // Sub-screen: Theme
            composable(Screen.Theme.route) {
                ThemeScreen(
                    appTheme = appTheme,
                    darkMode = darkMode,
                    amoledMode = amoledMode,
                    useSystemFont = useSystemFont,
                    onThemeChange = onThemeChange,
                    onDarkModeChange = onDarkModeChange,
                    onAmoledChange = onAmoledChange,
                    onSystemFontChange = onSystemFontChange,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Ringtone Selection
            composable(
                route = "${Screen.RingtoneSelection.route}?type={type}",
                arguments = listOf(navArgument("type") {
                    type = NavType.StringType
                    defaultValue = "sim"
                })
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "sim"
                RingtoneScreen(
                    type = type,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Charging Settings
            composable(Screen.ChargingSettings.route) {
                ChargingSettingsScreen(
                    onNavigateToRingtone = { navController.navigate("${Screen.RingtoneSelection.route}?type=charging") },
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Pocket Settings
            composable(Screen.PocketSettings.route) {
                PocketSettingsScreen(
                    onNavigateToRingtone = { navController.navigate("${Screen.RingtoneSelection.route}?type=pocket") },
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Intruder Selfie Settings
            composable(Screen.IntruderSettings.route) {
                IntruderSettingsScreen(
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Live Map
            composable(Screen.LiveMap.route) {
                LiveMapScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Google Drive Settings
            composable(Screen.GoogleDriveSettings.route) {
                GoogleDriveSettingsScreen(
                    onConnectAccount = onRequestGoogleDriveConnect,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Device Info
            composable(Screen.DeviceInfo.route) {
                DeviceInfoScreen(
                    isArmed = isArmed,
                    isPocketArmed = isPocketArmed,
                    lastAlert = lastAlert,
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: About
            composable(Screen.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Debug & Diagnostics
            composable(Screen.DebugLogs.route) {
                DebugLogsScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: SIM Tamper Settings
            composable(Screen.SimTamperSettings.route) {
                val simTamperContact = com.sentinelshield.antitheft.SecurityPreferences.getSimTamperContact(context)
                SimTamperSettingsScreen(
                    initialContact = simTamperContact,
                    onSave = { contact ->
                        com.sentinelshield.antitheft.SecurityPreferences.setSimTamperContact(context, contact)
                        navController.popBackStack()
                    },
                    onNavigateToRingtone = { navController.navigate("${Screen.RingtoneSelection.route}?type=sim") },
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Sub-screen: Remote SMS Settings
            composable(Screen.RemoteSmsSettings.route) {
                RemoteSmsSettingsScreen(
                    isSmsControlArmed = isSmsControlArmed,
                    onToggleSmsControl = onToggleSmsControl,
                    onNavigateToTrustedContacts = { navController.navigate(Screen.TrustedContacts.route) },
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onBack = { navController.popBackStack() }
                )
            }

            // Sub-screen: Trusted Contacts
            composable(Screen.TrustedContacts.route) {
                TrustedContactsScreen(
                    context = context,
                    onNavigateToDebugLogs = { navController.navigate(Screen.DebugLogs.route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
