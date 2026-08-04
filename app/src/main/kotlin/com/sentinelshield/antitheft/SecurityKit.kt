package com.sentinelshield.antitheft

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.sentinelshield.antitheft.ui.theme.AppTheme
import com.sentinelshield.antitheft.ui.theme.DarkModeOption

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkInt: Int,
    val securityPatch: String,
    val totalRam: String,
    val totalStorage: String
)

object DeviceUtils {
    fun getDeviceInfo(context: Context): DeviceInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamBytes = memoryInfo.totalMem
        val totalRamGb = String.format("%.1f GB", totalRamBytes / (1024.0 * 1024.0 * 1024.0))

        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val totalStorageBytes = stat.blockCountLong * stat.blockSizeLong
        val totalStorageGb = String.format("%.0f GB", totalStorageBytes / (1024.0 * 1024.0 * 1024.0))

        val securityPatch = Build.VERSION.SECURITY_PATCH

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = securityPatch,
            totalRam = totalRamGb,
            totalStorage = totalStorageGb
        )
    }
}

/** Stores local protection and theme state. The app deliberately has no network permission. */
object SecurityPreferences {
    private const val FILE = "sentinel_protection"
    private const val ARMED = "armed"
    private const val LAST_ALERT = "last_alert"
    private const val SIM_SNAPSHOT = "sim_snapshot"
    private const val POCKET_ARMED = "pocket_armed"
    private const val APP_THEME = "app_theme"
    private const val DARK_MODE = "dark_mode"
    private const val AMOLED_MODE = "amoled_mode"
    private const val SYSTEM_FONT = "system_font"
    private const val HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"
    private const val POCKET_ALARM_RINGTONE_URI = "pocket_alarm_ringtone_uri"
    private const val CHARGING_ALARM_RINGTONE_URI = "charging_alarm_ringtone_uri"
    private const val SIM_ALARM_RINGTONE_URI = "sim_alarm_ringtone_uri"
    private const val PERSISTENT_CHARGING_ARMED = "persistent_charging_armed"
    private const val ONE_TIME_CHARGING_ARMED = "one_time_charging_armed"
    private const val INTRUDER_SELFIE_ARMED = "intruder_selfie_armed"
    private const val TRUSTED_CONTACTS = "trusted_contacts"
    private const val SAVED_SUBSCRIPTION_IDS = "saved_subscription_ids"
    private const val SMS_CONTROL_ARMED = "sms_control_armed"
    private const val SIM_TAMPER_CONTACT = "sim_tamper_contact"
    private const val CHARGING_ALARM_DELAY_SECONDS = "charging_alarm_delay_seconds"
    private const val POCKET_ARMING_DELAY = "pocket_arming_delay"
    private const val POCKET_GRACE_PERIOD = "pocket_grace_period"
    private const val POCKET_USE_STROBE = "pocket_use_strobe"
    private const val POCKET_FORCE_VOLUME = "pocket_force_volume"
    private const val INTRUDER_CAPTURE_MODE = "intruder_capture_mode"
    private const val INTRUDER_VIDEO_DURATION = "intruder_video_duration"
    private const val DEBUG_LOGGING_ENABLED = "debug_logging_enabled"
    private const val FAKE_SHUTDOWN_ENABLED = "fake_shutdown_enabled"

    private fun preferences(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isFakeShutdownEnabled(context: Context): Boolean = preferences(context).getBoolean(FAKE_SHUTDOWN_ENABLED, true)

    fun setFakeShutdownEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(FAKE_SHUTDOWN_ENABLED, enabled).apply()
    }

    fun isDebugLoggingEnabled(context: Context): Boolean = preferences(context).getBoolean(DEBUG_LOGGING_ENABLED, false)

    fun setDebugLoggingEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(DEBUG_LOGGING_ENABLED, enabled).apply()
    }

    fun hasCompletedOnboarding(context: Context): Boolean = preferences(context).getBoolean(HAS_COMPLETED_ONBOARDING, false)

    fun setHasCompletedOnboarding(context: Context, completed: Boolean) {
        preferences(context).edit().putBoolean(HAS_COMPLETED_ONBOARDING, completed).apply()
    }

    fun isArmed(context: Context): Boolean = preferences(context).getBoolean(ARMED, false)

    fun setArmed(context: Context, armed: Boolean) {
        preferences(context).edit().putBoolean(ARMED, armed).apply()
    }

    fun isPocketArmed(context: Context): Boolean = preferences(context).getBoolean(POCKET_ARMED, false)

    fun setPocketArmed(context: Context, armed: Boolean) {
        preferences(context).edit().putBoolean(POCKET_ARMED, armed).apply()
    }
    
    fun getPocketArmingDelay(context: Context): Int = preferences(context).getInt(POCKET_ARMING_DELAY, 5)

    fun setPocketArmingDelay(context: Context, seconds: Int) {
        preferences(context).edit().putInt(POCKET_ARMING_DELAY, seconds).apply()
    }
    
    fun getPocketGracePeriod(context: Context): Int = preferences(context).getInt(POCKET_GRACE_PERIOD, 3)

    fun setPocketGracePeriod(context: Context, seconds: Int) {
        preferences(context).edit().putInt(POCKET_GRACE_PERIOD, seconds).apply()
    }
    
    fun isPocketUseStrobe(context: Context): Boolean = preferences(context).getBoolean(POCKET_USE_STROBE, true)

    fun setPocketUseStrobe(context: Context, useStrobe: Boolean) {
        preferences(context).edit().putBoolean(POCKET_USE_STROBE, useStrobe).apply()
    }
    
    fun isPocketForceVolume(context: Context): Boolean = preferences(context).getBoolean(POCKET_FORCE_VOLUME, true)

    fun setPocketForceVolume(context: Context, forceVolume: Boolean) {
        preferences(context).edit().putBoolean(POCKET_FORCE_VOLUME, forceVolume).apply()
    }
    
    fun isIntruderSelfieArmed(context: Context): Boolean = preferences(context).getBoolean(INTRUDER_SELFIE_ARMED, false)

    fun setIntruderSelfieArmed(context: Context, armed: Boolean) {
        preferences(context).edit().putBoolean(INTRUDER_SELFIE_ARMED, armed).apply()
    }
    
    fun getIntruderCaptureMode(context: Context): String = preferences(context).getString(INTRUDER_CAPTURE_MODE, "VIDEO") ?: "VIDEO"
    
    fun setIntruderCaptureMode(context: Context, mode: String) {
        preferences(context).edit().putString(INTRUDER_CAPTURE_MODE, mode).apply()
    }
    
    fun getIntruderVideoDuration(context: Context): Int = preferences(context).getInt(INTRUDER_VIDEO_DURATION, 3)
    
    fun setIntruderVideoDuration(context: Context, seconds: Int) {
        preferences(context).edit().putInt(INTRUDER_VIDEO_DURATION, seconds).apply()
    }

    private const val AUTO_ENABLE_LOCATION_DATA = "auto_enable_location_data"

    fun isAutoEnableLocationDataEnabled(context: Context): Boolean = preferences(context).getBoolean(AUTO_ENABLE_LOCATION_DATA, true)

    fun setAutoEnableLocationDataEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(AUTO_ENABLE_LOCATION_DATA, enabled).apply()
    }

    fun isSmsControlArmed(context: Context): Boolean = preferences(context).getBoolean(SMS_CONTROL_ARMED, false)

    fun setSmsControlArmed(context: Context, armed: Boolean) {
        preferences(context).edit().putBoolean(SMS_CONTROL_ARMED, armed).apply()
    }

    fun getSimTamperContact(context: Context): String = preferences(context).getString(SIM_TAMPER_CONTACT, "") ?: ""

    fun setSimTamperContact(context: Context, contact: String) {
        preferences(context).edit().putString(SIM_TAMPER_CONTACT, contact).apply()
    }

    fun getChargingAlarmDelaySeconds(context: Context): Int = preferences(context).getInt(CHARGING_ALARM_DELAY_SECONDS, 3)

    fun setChargingAlarmDelaySeconds(context: Context, seconds: Int) {
        preferences(context).edit().putInt(CHARGING_ALARM_DELAY_SECONDS, seconds).apply()
    }

    fun getTrustedContacts(context: Context): Set<String> = preferences(context).getStringSet(TRUSTED_CONTACTS, emptySet()) ?: emptySet()

    fun addTrustedContact(context: Context, contact: String) {
        val current = getTrustedContacts(context).toMutableSet()
        current.add(contact.replace("\\s+".toRegex(), ""))
        preferences(context).edit().putStringSet(TRUSTED_CONTACTS, current).apply()
    }

    fun removeTrustedContact(context: Context, contact: String) {
        val current = getTrustedContacts(context).toMutableSet()
        current.remove(contact.replace("\\s+".toRegex(), ""))
        preferences(context).edit().putStringSet(TRUSTED_CONTACTS, current).apply()
    }

    fun getSavedSubscriptionIds(context: Context): Set<String> = preferences(context).getStringSet(SAVED_SUBSCRIPTION_IDS, emptySet()) ?: emptySet()

    fun setSavedSubscriptionIds(context: Context, ids: Set<String>) {
        preferences(context).edit().putStringSet(SAVED_SUBSCRIPTION_IDS, ids).apply()
    }

    fun isPersistentChargingArmed(context: Context): Boolean = preferences(context).getBoolean(PERSISTENT_CHARGING_ARMED, false)

    fun setPersistentChargingArmed(context: Context, armed: Boolean) {
        preferences(context).edit().putBoolean(PERSISTENT_CHARGING_ARMED, armed).apply()
    }

    fun isOneTimeChargingArmed(context: Context): Boolean = preferences(context).getBoolean(ONE_TIME_CHARGING_ARMED, false)

    fun setOneTimeChargingArmed(context: Context, armed: Boolean) {
        preferences(context).edit().putBoolean(ONE_TIME_CHARGING_ARMED, armed).apply()
    }

    fun isChargingMonitorActive(context: Context): Boolean {
        return isPersistentChargingArmed(context) || isOneTimeChargingArmed(context)
    }

    fun saveSimSnapshot(context: Context, snapshot: String) {
        preferences(context).edit().putString(SIM_SNAPSHOT, snapshot).apply()
    }

    fun getSimSnapshot(context: Context): String =
        preferences(context).getString(SIM_SNAPSHOT, "").orEmpty()

    fun recordAlert(context: Context, alert: String) {
        preferences(context).edit().putString(LAST_ALERT, alert).apply()
    }

    fun getLastAlert(context: Context): String =
        preferences(context).getString(LAST_ALERT, "No security alerts recorded.")
            ?: "No security alerts recorded."

    fun getAppTheme(context: Context): AppTheme {
        val name = preferences(context).getString(APP_THEME, AppTheme.Default.name) ?: AppTheme.Default.name
        return try {
            AppTheme.valueOf(name)
        } catch (_: Exception) {
            AppTheme.Default
        }
    }

    fun setAppTheme(context: Context, theme: AppTheme) {
        preferences(context).edit().putString(APP_THEME, theme.name).apply()
    }

    fun getDarkMode(context: Context): DarkModeOption {
        val name = preferences(context).getString(DARK_MODE, DarkModeOption.System.name) ?: DarkModeOption.System.name
        return try {
            DarkModeOption.valueOf(name)
        } catch (_: Exception) {
            DarkModeOption.System
        }
    }

    fun setDarkMode(context: Context, mode: DarkModeOption) {
        preferences(context).edit().putString(DARK_MODE, mode.name).apply()
    }

    fun isAmoledMode(context: Context): Boolean = preferences(context).getBoolean(AMOLED_MODE, false)

    fun setAmoledMode(context: Context, amoled: Boolean) {
        preferences(context).edit().putBoolean(AMOLED_MODE, amoled).apply()
    }

    fun useSystemFont(context: Context): Boolean = preferences(context).getBoolean(SYSTEM_FONT, true)

    fun setUseSystemFont(context: Context, systemFont: Boolean) {
        preferences(context).edit().putBoolean(SYSTEM_FONT, systemFont).apply()
    }

    fun getPocketAlarmRingtone(context: Context): String {
        val defaultUri = "android.resource://${context.packageName}/${R.raw.alarm_klaxon}"
        return preferences(context).getString(POCKET_ALARM_RINGTONE_URI, defaultUri) ?: defaultUri
    }

    fun setPocketAlarmRingtone(context: Context, uri: String) {
        preferences(context).edit().putString(POCKET_ALARM_RINGTONE_URI, uri).apply()
    }

    fun getChargingAlarmRingtone(context: Context): String {
        val defaultUri = "android.resource://${context.packageName}/${R.raw.alarm_klaxon}"
        return preferences(context).getString(CHARGING_ALARM_RINGTONE_URI, defaultUri) ?: defaultUri
    }

    fun setChargingAlarmRingtone(context: Context, uri: String) {
        preferences(context).edit().putString(CHARGING_ALARM_RINGTONE_URI, uri).apply()
    }

    fun getSimAlarmRingtone(context: Context): String {
        val defaultUri = "android.resource://${context.packageName}/${R.raw.alarm_klaxon}"
        return preferences(context).getString(SIM_ALARM_RINGTONE_URI, defaultUri) ?: defaultUri
    }

    fun setSimAlarmRingtone(context: Context, uri: String) {
        preferences(context).edit().putString(SIM_ALARM_RINGTONE_URI, uri).apply()
    }
}

/** Notification factory shared by activities, services, and broadcast receivers. */
object SecurityNotifier {
    const val MONITOR_CHANNEL = "protection_status"
    const val ALERT_CHANNEL = "tamper_alerts"
    const val MONITOR_ID = 41
    const val ALERT_ID = 42

    fun createChannels(context: Context) {

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val monitor = NotificationChannel(
            MONITOR_CHANNEL,
            "Protection status",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Visible while SIM tamper monitoring is armed."
        }
        val alert = NotificationChannel(
            ALERT_CHANNEL,
            "Tamper alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts raised by Sentinel Shield."
        }
        manager.createNotificationChannel(monitor)
        manager.createNotificationChannel(alert)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
        )
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun monitorNotification(context: Context): Notification {
        val isOneTimeChargingArmed = SecurityPreferences.isOneTimeChargingArmed(context)
        
        val builder = androidx.core.app.NotificationCompat.Builder(context, MONITOR_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setContentTitle("Sentinel Shield")
            .setContentText("Sentinel Shield is running")
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)

        val isPersistentArmed = SecurityPreferences.isPersistentChargingArmed(context)
        if (!isPersistentArmed) {
            if (!isOneTimeChargingArmed) {
                val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_TOGGLE_ONETIME_CHARGE
                }
                val actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(R.drawable.ic_shield_lock, "Enable Charge Alarm", actionPendingIntent)
            } else {
                val actionIntent = Intent(context, DisarmActivity::class.java)
                val actionPendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val actionBuilder = androidx.core.app.NotificationCompat.Action.Builder(
                    R.drawable.ic_shield_lock, "Disarm", actionPendingIntent
                )
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    actionBuilder.setAuthenticationRequired(true)
                }
                
                builder.addAction(actionBuilder.build())
            }
        }
        
        return builder.build()
    }

    fun alertNotification(context: Context, reason: String): Notification {
        val actionIntent = Intent(context, DisarmActivity::class.java)
        val actionPendingIntent = PendingIntent.getActivity(
            context,
            2,
            actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val actionBuilder = androidx.core.app.NotificationCompat.Action.Builder(
            R.drawable.ic_shield_lock, "Disarm Alarm", actionPendingIntent
        )
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            actionBuilder.setAuthenticationRequired(true)
        }

        return androidx.core.app.NotificationCompat.Builder(context, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setContentTitle("Security alert")
            .setContentText(reason)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(reason))
            .setContentIntent(openAppIntent(context))
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(actionBuilder.build())
            .build()
    }

    fun postTamperNotification(context: Context, reason: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            createChannels(context)
            context.getSystemService(NotificationManager::class.java)
                ?.notify(ALERT_ID, alertNotification(context, reason))
        } catch (_: Exception) {}
    }

    fun cancelAlert(context: Context) {
        try {
            context.getSystemService(NotificationManager::class.java)?.cancel(ALERT_ID)
        } catch (_: Exception) {}
    }
}

/** Restores normal ringer mode only after the owner grants Notification Policy access. */
object RingerController {
    fun restoreNormalRinger(context: Context): Boolean {
        val notifications = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!notifications.isNotificationPolicyAccessGranted) {
            return false
        }
        return try {
            context.getSystemService(AudioManager::class.java)
                ?.ringerMode = AudioManager.RINGER_MODE_NORMAL
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
