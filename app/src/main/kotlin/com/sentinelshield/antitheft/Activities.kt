package com.sentinelshield.antitheft

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.sentinelshield.antitheft.ui.screens.AppNavigation
import com.sentinelshield.antitheft.ui.theme.AppTheme
import com.sentinelshield.antitheft.ui.theme.DarkModeOption
import com.sentinelshield.antitheft.ui.theme.SentinelShieldTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
class MainActivity : androidx.fragment.app.FragmentActivity() {
    companion object {
        private const val REQUEST_PERMISSIONS = 101
    }

    private val isArmedState = mutableStateOf(false)
    private val isPocketArmedState = mutableStateOf(false)
    private val isChargingArmedState = mutableStateOf(false)
    private val isIntruderSelfieArmedState = mutableStateOf(false)
    private val isSmsControlArmedState = mutableStateOf(false)
    private val lastAlertState = mutableStateOf("")

    private val appThemeState = mutableStateOf(AppTheme.Default)
    private val darkModeState = mutableStateOf(DarkModeOption.System)
    private val amoledModeState = mutableStateOf(false)
    private val useSystemFontState = mutableStateOf(true)
    private var pendingDisarmFeature: String = "ALL"
    private val disarmLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) disarm()
    }

    private val GOOGLE_DRIVE_PERMISSION_REQ_CODE = 9002

    private val googleSignInLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                if (com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(account, com.sentinelshield.antitheft.utils.GoogleDriveSyncManager.DRIVE_FILE_SCOPE)) {
                    Toast.makeText(this, "Connected to Google Drive as ${account.email}", Toast.LENGTH_LONG).show()
                    com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Google Drive connected: ${account.email}", force = true)
                    refreshState()
                } else {
                    com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Account selected (${account.email}), requesting Drive scope permission...", force = true)
                    com.google.android.gms.auth.api.signin.GoogleSignIn.requestPermissions(
                        this,
                        GOOGLE_DRIVE_PERMISSION_REQ_CODE,
                        account,
                        com.sentinelshield.antitheft.utils.GoogleDriveSyncManager.DRIVE_FILE_SCOPE
                    )
                }
            } else {
                Toast.makeText(this, "Google Sign-In returned no account.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Google Sign-In API exception (Code ${e.statusCode}): ${e.message}", force = true)
            if (e.statusCode == 12501) {
                Toast.makeText(this, "Google Sign-In cancelled.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Google Sign-In failed (Code ${e.statusCode})", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Google Sign-In error: ${e.message}", force = true)
            Toast.makeText(this, "Google Sign-In error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_DRIVE_PERMISSION_REQ_CODE) {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this)
            if (account != null && com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions(account, com.sentinelshield.antitheft.utils.GoogleDriveSyncManager.DRIVE_FILE_SCOPE)) {
                Toast.makeText(this, "Google Drive permission granted: ${account.email}", Toast.LENGTH_LONG).show()
                com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Google Drive permission granted for ${account.email}", force = true)
                refreshState()
            } else {
                Toast.makeText(this, "Google Drive permission is required for cloud backups.", Toast.LENGTH_LONG).show()
                com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Google Drive permission denied by user.", force = true)
            }
        }
    }

    private fun launchGoogleSignIn() {
        val client = com.sentinelshield.antitheft.utils.GoogleDriveSyncManager.getGoogleSignInClient(this)
        googleSignInLauncher.launch(client.signInIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshState()

        setContent {
            val appTheme by appThemeState
            val darkMode by darkModeState
            val amoledMode by amoledModeState
            val useSystemFont by useSystemFontState
            
            val isArmed by isArmedState
            val isPocketArmed by isPocketArmedState
            val isChargingArmed by isChargingArmedState
            val isIntruderSelfieArmed by isIntruderSelfieArmedState
            val isSmsControlArmed by isSmsControlArmedState
            val lastAlert by lastAlertState

            SentinelShieldTheme(
                appTheme = appTheme,
                darkMode = darkMode,
                amoledMode = amoledMode,
                useSystemFont = useSystemFont
            ) {
                AppNavigation(
                    context = this,
                    isArmed = isArmed,
                    isPocketArmed = isPocketArmed,
                    isChargingArmed = isChargingArmed,
                    isIntruderSelfieArmed = isIntruderSelfieArmed,
                    isSmsControlArmed = isSmsControlArmed,
                    lastAlert = lastAlert,
                    appTheme = appTheme,
                    darkMode = darkMode,
                    amoledMode = amoledMode,
                    useSystemFont = useSystemFont,
                    onThemeChange = { newTheme ->
                        SecurityPreferences.setAppTheme(this, newTheme)
                        appThemeState.value = newTheme
                    },
                    onDarkModeChange = { newMode ->
                        SecurityPreferences.setDarkMode(this, newMode)
                        darkModeState.value = newMode
                    },
                    onAmoledChange = { newAmoled ->
                        SecurityPreferences.setAmoledMode(this, newAmoled)
                        amoledModeState.value = newAmoled
                    },
                    onSystemFontChange = { newFont ->
                        SecurityPreferences.setUseSystemFont(this, newFont)
                        useSystemFontState.value = newFont
                    },
                    onRequestAppPermissions = { requestAppPermissions() },
                    onRequestLocationPermissions = { requestLocationPermissions() },
                    onRequestCameraPermissions = { requestCameraPermissions() },
                    onToggleSimArm = { shouldArm ->
                        if (shouldArm) armMonitor() else confirmDisarm("SIM")
                    },
                    onTogglePocketArm = { shouldArm ->
                        if (shouldArm) armPocketMonitor() else disarmPocketMonitor()
                    },
                    onToggleChargingArm = { shouldArm ->
                        if (shouldArm) armChargingMonitor() else confirmDisarm("CHARGE")
                    },
                    onToggleIntruderSelfie = { shouldArm ->
                        if (shouldArm) armIntruderSelfie() else disarmIntruderSelfie()
                    },
                    onToggleSmsControl = { shouldArm ->
                        if (shouldArm) armSmsControl() else disarmSmsControl()
                    },
                    onRequestBatteryExemption = { requestIgnoreBatteryOptimizations() },
                    onOpenDndSettings = { openPolicyAccessSettings() },
                    onRestoreRinger = { restoreRinger() },

                    onOpenAccessibilitySettings = { openAccessibilitySettings() },
                    onOpenNotificationListenerSettings = { openNotificationListenerSettings() },
                    onRequestSmsPermission = { requestSmsPermission() },
                    onRequestGoogleDriveConnect = { launchGoogleSignIn() },
                    onOpenScreenShield = {
                        startActivity(Intent(this@MainActivity, DecoyScreenActivity::class.java))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        isArmedState.value = SecurityPreferences.isArmed(this)
        isPocketArmedState.value = SecurityPreferences.isPocketArmed(this)
        isChargingArmedState.value = SecurityPreferences.isPersistentChargingArmed(this)
        isIntruderSelfieArmedState.value = SecurityPreferences.isIntruderSelfieArmed(this)
        isSmsControlArmedState.value = SecurityPreferences.isSmsControlArmed(this)
        lastAlertState.value = SecurityPreferences.getLastAlert(this)

        appThemeState.value = SecurityPreferences.getAppTheme(this)
        darkModeState.value = SecurityPreferences.getDarkMode(this)
        amoledModeState.value = SecurityPreferences.isAmoledMode(this)
        useSystemFontState.value = SecurityPreferences.useSystemFont(this)
    }

    private fun requestAppPermissions() {
        val permissions = buildList {
            if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.READ_PHONE_STATE)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isEmpty()) {
            toast("App permissions are already granted.")
        } else {
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun requestLocationPermissions() {
        val permissions = buildList {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
        if (permissions.isEmpty()) {
            toast("Location permissions are already granted.")
        } else {
            requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS)
        }
    }

    private fun requestCameraPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSIONS)
            return
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS)
            return
        }
        toast("Camera permissions are already granted.")
    }
    private fun armSmsControl() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            toast("Grant SMS and Contacts permissions first.")
            return
        }
        SecurityPreferences.setSmsControlArmed(this, true)
        com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Remote SMS Control feature ARMED.", force = true)
        toast("Remote SMS Control armed.")
        refreshState()
    }

    private fun disarmSmsControl() {
        SecurityPreferences.setSmsControlArmed(this, false)
        com.sentinelshield.antitheft.utils.DebugLogger.log(this, "MainActivity", "Remote SMS Control feature DISARMED.", force = true)
        toast("Remote SMS Control disarmed.")
        refreshState()
    }

    private fun armMonitor() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardSecure) {
            toast("Set a secure device lock before arming protection.")
            return
        }
        if (checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            toast("Grant phone permission first so SIM changes can be monitored.")
            return
        }
        SecurityPreferences.setArmed(this, true)
        try {
            SecurityMonitorService.start(this)
            toast("SIM tamper monitor armed.")
        } catch (_: RuntimeException) {
            SecurityPreferences.setArmed(this, false)
            toast("Android could not start the monitor. Open the app again and check battery restrictions.")
        }
        refreshState()
    }

    private fun confirmDisarm(feature: String = "ALL") {
        pendingDisarmFeature = feature
        val simArmed = SecurityPreferences.isArmed(this)
        val chargeArmed = SecurityPreferences.isPersistentChargingArmed(this) || SecurityPreferences.isOneTimeChargingArmed(this)
        if (!simArmed && !chargeArmed) {
            stopService(Intent(this, SecurityAlertService::class.java))
            SecurityPreferences.setOneTimeChargingArmed(this, false)
            toast("No monitor is armed; any active test alarm was stopped.")
            refreshState()
            return
        }
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardSecure) {
            // If device is not secure, there is no way to authenticate, so just disarm
            disarm()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                 androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Disarm Sentinel Shield")
                .setSubtitle("Confirm your device credential to disarm protection.")
                .setAllowedAuthenticators(authenticators)
                .build()
            val biometricPrompt = androidx.biometric.BiometricPrompt(
                this,
                androidx.core.content.ContextCompat.getMainExecutor(this),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        disarm()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED) {
                            toast(errString.toString())
                        }
                    }
                }
            )
            biometricPrompt.authenticate(promptInfo)
        } else {
            @Suppress("DEPRECATION")
            keyguard.createConfirmDeviceCredentialIntent(
                "Disarm Sentinel Shield",
                "Confirm your device credential to disarm protection.",
            )?.let { disarmLauncher.launch(it) } ?: toast("Unable to open device lock.")
        }
    }

    private fun disarm() {
        if (pendingDisarmFeature == "SIM" || pendingDisarmFeature == "ALL") {
            SecurityPreferences.setArmed(this, false)
        }
        if (pendingDisarmFeature == "CHARGE" || pendingDisarmFeature == "ALL") {
            SecurityPreferences.setPersistentChargingArmed(this, false)
            SecurityPreferences.setOneTimeChargingArmed(this, false)
        }
        
        if (!SecurityPreferences.isPocketArmed(this) && !SecurityPreferences.isPersistentChargingArmed(this) && !SecurityPreferences.isOneTimeChargingArmed(this) && !SecurityPreferences.isArmed(this)) {
            stopService(Intent(this, SecurityMonitorService::class.java))
        } else {
            // Need to restart it to update the notification if one-time charging was cleared
            SecurityMonitorService.start(this)
        }
        stopService(Intent(this, SecurityAlertService::class.java))
        toast("Protection disarmed and alarm stopped.")
        refreshState()
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        } else {
            toast("Not required on this Android version.")
        }
    }

    private fun armPocketMonitor() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardSecure) {
            toast("Set a secure device lock before arming protection.")
            return
        }
        SecurityPreferences.setPocketArmed(this, true)
        try {
            SecurityMonitorService.start(this)
            toast("Pocket monitor armed.")
        } catch (_: RuntimeException) {
            SecurityPreferences.setPocketArmed(this, false)
            toast("Android could not start the monitor. Open the app again and check battery restrictions.")
        }
        refreshState()
    }

    private fun disarmPocketMonitor() {
        SecurityPreferences.setPocketArmed(this, false)
        toast("Pocket monitor disarmed.")
        if (!SecurityPreferences.isArmed(this) && !SecurityPreferences.isChargingMonitorActive(this)) {
            stopService(Intent(this, SecurityMonitorService::class.java))
        } else {
            SecurityMonitorService.start(this)
        }
        refreshState()
    }

    private fun armChargingMonitor() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardSecure) {
            toast("Set a secure device lock before arming protection.")
            return
        }
        SecurityPreferences.setPersistentChargingArmed(this, true)
        try {
            SecurityMonitorService.start(this)
            toast("Charging monitor armed.")
        } catch (_: RuntimeException) {
            SecurityPreferences.setPersistentChargingArmed(this, false)
            toast("Android could not start the monitor. Open the app again and check battery restrictions.")
        }
        refreshState()
    }

    private val deviceAdminLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            armIntruderSelfie()
        } else {
            toast("Device admin must be enabled to use Intruder Selfie.")
        }
    }

    private fun armIntruderSelfie() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSIONS)
            return
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSIONS)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:$packageName"))
            startActivity(intent)
            toast("Please allow Display Over Other Apps for Intruder Selfie.")
            return
        }
        val adminComponent = android.content.ComponentName(this, LockScreenAdminReceiver::class.java)
        val devicePolicyManager = getSystemService(android.app.admin.DevicePolicyManager::class.java)
        if (devicePolicyManager != null && !devicePolicyManager.isAdminActive(adminComponent)) {
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to detect incorrect password attempts for Intruder Selfie.")
            }
            deviceAdminLauncher.launch(intent)
            return
        }
        
        SecurityPreferences.setIntruderSelfieArmed(this, true)
        toast("Intruder Selfie armed.")
        refreshState()
    }

    private fun disarmIntruderSelfie() {
        SecurityPreferences.setIntruderSelfieArmed(this, false)
        toast("Intruder Selfie disarmed.")
        refreshState()
    }

    private fun openPolicyAccessSettings() = startActivity(
        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS),
    )

    private fun openAccessibilitySettings() = startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    )

    private fun openNotificationListenerSettings() = startActivity(
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    )

    private fun requestSmsPermission() {
        requestPermissions(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_CONTACTS), REQUEST_PERMISSIONS)
    }

    private fun restoreRinger() {
        toast(
            if (RingerController.restoreNormalRinger(this)) {
                "Ringer mode restored to normal."
            } else {
                "Grant Do Not Disturb access first, then try again."
            },
        )
        refreshState()
    }




    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
