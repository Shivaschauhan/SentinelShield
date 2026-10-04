package com.sentinelshield.antitheft

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootResilienceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (ProtectionController.needsService(context)) {
                SecurityMonitorService.start(context)
            }
            com.sentinelshield.antitheft.safezone.GeofenceRegistrar.registerAll(context)
        }
    }
}
