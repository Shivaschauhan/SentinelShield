package com.sentinelshield.antitheft


import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/** Best-effort receiver for the legacy public SIM-state broadcast when the monitor is not alive. */
class SimStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!SecurityPreferences.isArmed(context) ||
            intent.action != "android.intent.action.SIM_STATE_CHANGED"
        ) {
            return
        }

        // "ss" is the legacy public SIM-state extra. Do not rely on hidden/System API extras.
        if (!"ABSENT".equals(intent.getStringExtra("ss"), ignoreCase = true)) return

        val reason = "SIM card removal was reported by Android."
        SecurityPreferences.recordAlert(context, reason)
        SecurityAlertService.start(context, reason)
    }
}

/** Starts the monitor service from boot. Android 12+ exempts ACTION_BOOT_COMPLETED for foreground services. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val isSimArmed = SecurityPreferences.isArmed(context)
            val isPocketArmed = SecurityPreferences.isPocketArmed(context)
            val isChargingArmed = SecurityPreferences.isChargingMonitorActive(context)
            if (isSimArmed || isPocketArmed || isChargingArmed) {
                SecurityMonitorService.start(context)
            }
        }
    }
}


