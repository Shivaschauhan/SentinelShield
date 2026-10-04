package com.sentinelshield.antitheft

import android.Manifest
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.sentinelshield.antitheft.safezone.EffectiveProtection
import com.sentinelshield.antitheft.safezone.EffectiveProtectionPolicy
import com.sentinelshield.antitheft.safezone.SafeZoneStore
import com.sentinelshield.antitheft.safezone.UserProtection
import com.sentinelshield.antitheft.safezone.Zone
import com.sentinelshield.antitheft.safezone.ZoneFeature
import com.sentinelshield.antitheft.safezone.ZonePolicy
import com.sentinelshield.antitheft.utils.DebugLogger

/** Outcome of an attempt to arm a feature. */
sealed interface ArmResult {
    data object Ok : ArmResult
    data class Blocked(val reason: String) : ArmResult
}

/**
 * Single place that changes protection state and keeps [SecurityMonitorService] in step with it.
 *
 * The owner's own switches live in [SecurityPreferences] and are never overwritten by Safe Zones.
 * What is actually running is [effective]: the owner's switches with the zone layer applied.
 */
object ProtectionController {
    private const val TAG = "ProtectionController"
    private const val DELAYED_RECONCILE_MS = 1_500L

    // ---- State ---------------------------------------------------------------------------

    fun userProtection(context: Context) = UserProtection(
        sim = SecurityPreferences.isArmed(context),
        pocket = SecurityPreferences.isPocketArmed(context),
        chargingPersistent = SecurityPreferences.isPersistentChargingArmed(context),
        chargingOneTime = SecurityPreferences.isOneTimeChargingArmed(context),
        intruder = SecurityPreferences.isIntruderSelfieArmed(context),
    )

    fun effective(context: Context): EffectiveProtection {
        val app = context.applicationContext
        return EffectiveProtectionPolicy.compute(
            user = userProtection(app),
            zone = SafeZoneStore.currentZone(app),
            rules = SafeZoneStore.getRules(app),
            automationOn = SafeZoneStore.automationOn(app),
            pauseAllowed = !SecurityAlertService.isRunning,
            suppressed = SafeZoneStore.suppressed(app),
        )
    }

    /** The monitor service is needed for armed features or to host Safe Zone monitoring. */
    fun needsService(context: Context): Boolean =
        effective(context).anyMonitor || SafeZoneStore.isActive(context)

    /** Starts, refreshes or stops the monitor service so it matches the effective state. */
    fun reconcile(context: Context) {
        val app = context.applicationContext
        if (needsService(app)) SecurityMonitorService.start(app) else SecurityMonitorService.stop(app)
    }

    private fun reconcileLater(context: Context) {
        val app = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({ reconcile(app) }, DELAYED_RECONCILE_MS)
    }

    /** One line for the monitor notification. */
    fun statusLine(context: Context): String {
        val app = context.applicationContext
        if (!SafeZoneStore.isActive(app)) return "Sentinel Shield is running"
        if (SafeZoneStore.isPaused(app)) return "Safe Zones paused"
        val e = effective(app)
        return when (SafeZoneStore.currentZone(app)) {
            Zone.HOME -> {
                val paused = listOfNotNull(
                    "pocket".takeIf { e.pocketPausedByZone },
                    "charger".takeIf { e.chargingPausedByZone },
                )
                if (paused.isEmpty()) "At home" else "At home: ${paused.joinToString(" & ")} paused"
            }
            Zone.AWAY -> "Away: protection armed"
            Zone.UNKNOWN -> "Safe Zones: location unknown"
        }
    }

    // ---- Arming ----------------------------------------------------------------------------

    private fun hasSecureLock(context: Context): Boolean =
        context.getSystemService(KeyguardManager::class.java)?.isKeyguardSecure == true

    private fun hasPermission(context: Context, permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private const val NO_SECURE_LOCK = "Set a secure device lock before arming protection."

    fun armSim(context: Context): ArmResult {
        val app = context.applicationContext
        if (!hasSecureLock(app)) return ArmResult.Blocked(NO_SECURE_LOCK)
        if (!hasPermission(app, Manifest.permission.READ_PHONE_STATE)) {
            return ArmResult.Blocked("Grant phone permission first so SIM changes can be monitored.")
        }
        SecurityPreferences.setArmed(app, true)
        reconcile(app)
        return ArmResult.Ok
    }

    fun armPocket(context: Context): ArmResult {
        val app = context.applicationContext
        if (!hasSecureLock(app)) return ArmResult.Blocked(NO_SECURE_LOCK)
        SecurityPreferences.setPocketArmed(app, true)
        reconcile(app)
        return ArmResult.Ok
    }

    fun armCharging(context: Context): ArmResult {
        val app = context.applicationContext
        if (!hasSecureLock(app)) return ArmResult.Blocked(NO_SECURE_LOCK)
        SecurityPreferences.setPersistentChargingArmed(app, true)
        reconcile(app)
        return ArmResult.Ok
    }

    /**
     * Arms intruder selfie only if every prerequisite is already in place. The interactive
     * permission flow stays in the UI; automation can never prompt.
     */
    fun armIntruderSilently(context: Context): ArmResult {
        val app = context.applicationContext
        if (!hasPermission(app, Manifest.permission.CAMERA)) return ArmResult.Blocked("Camera permission is missing.")
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            !hasPermission(app, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            return ArmResult.Blocked("Storage permission is missing.")
        }
        if (!Settings.canDrawOverlays(app)) return ArmResult.Blocked("Display over other apps is not allowed.")
        val admin = ComponentName(app, LockScreenAdminReceiver::class.java)
        val dpm = app.getSystemService(DevicePolicyManager::class.java)
        if (dpm == null || !dpm.isAdminActive(admin)) return ArmResult.Blocked("Device admin is not enabled.")
        SecurityPreferences.setIntruderSelfieArmed(app, true)
        return ArmResult.Ok
    }

    // ---- Disarming -------------------------------------------------------------------------

    /** Owner switched SIM and/or charger protection off (authentication is the caller's job). */
    fun disarm(context: Context, sim: Boolean, charging: Boolean) {
        val app = context.applicationContext
        if (sim) SecurityPreferences.setArmed(app, false)
        if (charging) {
            SecurityPreferences.setPersistentChargingArmed(app, false)
            SecurityPreferences.setOneTimeChargingArmed(app, false)
            SafeZoneStore.suppress(app, ZoneFeature.CHARGING)
        }
        SecurityAlertService.stop(app)
        reconcile(app)
    }

    fun disarmPocket(context: Context) {
        val app = context.applicationContext
        SecurityPreferences.setPocketArmed(app, false)
        SafeZoneStore.suppress(app, ZoneFeature.POCKET)
        reconcile(app)
    }

    fun disarmOneTimeCharging(context: Context) {
        val app = context.applicationContext
        SecurityPreferences.setOneTimeChargingArmed(app, false)
        SecurityAlertService.stop(app)
        reconcile(app)
    }

    fun toggleOneTimeCharging(context: Context) {
        val app = context.applicationContext
        val turningOn = !SecurityPreferences.isOneTimeChargingArmed(app)
        SecurityPreferences.setOneTimeChargingArmed(app, turningOn)
        if (!turningOn) SecurityAlertService.stop(app)
        reconcile(app)
    }

    /**
     * The owner proved they hold the phone (native unlock or disarm screen) while an alarm was
     * ringing: stop it, drop the one-time charger alarm and pocket switch, and keep the zone layer
     * from re-arming them until the next zone change.
     */
    fun ownerDisarmedAlarm(context: Context) {
        val app = context.applicationContext
        SecurityPreferences.setOneTimeChargingArmed(app, false)
        SecurityPreferences.setPocketArmed(app, false)
        SafeZoneStore.suppress(app, ZoneFeature.POCKET)
        SafeZoneStore.suppress(app, ZoneFeature.CHARGING)
        SecurityAlertService.stop(app)
        reconcile(app)
        // The alert service clears its running flag asynchronously; settle once it has.
        reconcileLater(app)
    }

    // ---- Safe Zones ------------------------------------------------------------------------

    /**
     * Recomputes the zone from the stored signals. On a change it clears the owner-disarm
     * suppression, applies sticky arming when leaving, and reconciles the service. Pass
     * [force] after changing configuration to reconcile even without a zone change.
     */
    @Synchronized
    fun refreshZone(context: Context, force: Boolean = false) {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val newZone = if (SafeZoneStore.automationOn(app, now)) {
            ZonePolicy.resolve(SafeZoneStore.signals(app), now)
        } else {
            Zone.UNKNOWN
        }
        val oldZone = SafeZoneStore.currentZone(app)
        val changed = newZone != oldZone
        if (changed) {
            SafeZoneStore.setCurrentZone(app, newZone)
            SafeZoneStore.clearSuppressed(app)
            DebugLogger.log(app, TAG, "Zone changed: $oldZone -> $newZone", force = true)
            if (newZone == Zone.AWAY) applyStickyArming(app)
        }
        if (changed || force) reconcile(app)
    }

    /** Milliseconds until the zone could change by itself (Wi-Fi grace closing, pause ending). */
    fun nextZoneDeadlineDelayMs(context: Context): Long? {
        val app = context.applicationContext
        val now = System.currentTimeMillis()
        val pausedUntil = SafeZoneStore.pausedUntil(app)
        val pauseDelay = (pausedUntil - now).takeIf { it > 0 }
        val graceDelay = if (SafeZoneStore.automationOn(app, now)) {
            ZonePolicy.nextDeadlineDelayMs(SafeZoneStore.signals(app), now)
        } else {
            null
        }
        return listOfNotNull(pauseDelay, graceDelay).minOrNull()
    }

    /** Leaving home may switch SIM tamper / intruder selfie on. They are never switched off by a zone. */
    private fun applyStickyArming(app: Context) {
        val rules = SafeZoneStore.getRules(app)
        val notes = mutableListOf<String>()
        if (rules.armSimWhenAway && !SecurityPreferences.isArmed(app)) {
            (armSim(app) as? ArmResult.Blocked)?.let { notes += "SIM tamper not armed: ${it.reason}" }
        }
        if (rules.armIntruderWhenAway && !SecurityPreferences.isIntruderSelfieArmed(app)) {
            (armIntruderSilently(app) as? ArmResult.Blocked)?.let { notes += "Intruder selfie not armed: ${it.reason}" }
        }
        SafeZoneStore.setNote(app, notes.joinToString("\n"))
        notes.forEach { DebugLogger.log(app, TAG, it, force = true) }
    }
}
