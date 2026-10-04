package com.sentinelshield.antitheft.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.sentinelshield.antitheft.ProtectionController
import com.sentinelshield.antitheft.safezone.GeofenceRegistrar
import com.sentinelshield.antitheft.safezone.SafeZoneStore
import com.sentinelshield.antitheft.utils.DebugLogger

/**
 * Receives geofence transitions from Google Play services. DWELL means "arrived and stayed",
 * EXIT means "left". Events from mock locations are dropped.
 */
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            val code = event.errorCode
            DebugLogger.log(
                context, TAG,
                "Geofence error ${GeofenceStatusCodes.getStatusCodeString(code)} ($code)",
                force = true,
            )
            if (code == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                // Location was switched off or is unavailable: verdicts are stale until it returns.
                SafeZoneStore.clearGeoState(context)
                ProtectionController.refreshZone(context, force = true)
            }
            return
        }

        event.triggeringLocation?.let { location ->
            if (GeofenceRegistrar.isMock(location)) {
                DebugLogger.log(context, TAG, "Ignored geofence transition from a mock location.", force = true)
                return
            }
        }

        val inside = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_DWELL -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }
        val ids = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        DebugLogger.log(context, TAG, "Geofence ${if (inside) "DWELL (arrived)" else "EXIT (left)"}: $ids", force = true)
        ids.forEach { SafeZoneStore.setGeoInside(context, it, inside) }
        ProtectionController.refreshZone(context)
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
