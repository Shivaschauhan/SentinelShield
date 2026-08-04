package com.sentinelshield.antitheft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootResilienceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val isSimArmed = SecurityPreferences.isArmed(context)
            val isPocketArmed = SecurityPreferences.isPocketArmed(context)
            val isChargingMonitorActive = SecurityPreferences.isChargingMonitorActive(context)

            if (isSimArmed || isPocketArmed || isChargingMonitorActive) {
                SecurityMonitorService.start(context)
            }
        }
    }
}
