package com.sentinelshield.antitheft.safezone

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sentinelshield.antitheft.ProtectionController
import com.sentinelshield.antitheft.receivers.GeofenceBroadcastReceiver
import com.sentinelshield.antitheft.utils.DebugLogger

/**
 * Registers the saved zones with Google Play services and works out the first inside/outside
 * verdict. Play services forgets geofences on reboot, when location is switched off, and when
 * its data is cleared, so [registerAll] is safe and cheap to call repeatedly.
 */
object GeofenceRegistrar {
    private const val TAG = "GeofenceRegistrar"
    private const val LOITERING_DELAY_MS = 120_000

    fun hasFineLocation(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Geofencing needs "Allow all the time" on Android 10+. */
    fun hasBackgroundLocation(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isPlayServicesAvailable(context: Context): Boolean =
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // Play services adds the event as an extra, so the PendingIntent must be mutable on Android 12+.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    fun removeAll(context: Context) {
        val app = context.applicationContext
        runCatching { LocationServices.getGeofencingClient(app).removeGeofences(pendingIntent(app)) }
        SafeZoneStore.clearGeoState(app)
    }

    /**
     * (Re)registers every saved zone. Without Safe Zones active the geofences are removed.
     * With Play services or the permissions missing, Safe Zones falls back to Wi-Fi only.
     */
    @SuppressLint("MissingPermission")
    fun registerAll(context: Context, evaluateInitial: Boolean = true) {
        val app = context.applicationContext
        if (!SafeZoneStore.isActive(app)) {
            removeAll(app)
            return
        }
        if (!isPlayServicesAvailable(app)) {
            markUnavailable(app, "Google Play services is not available; using Wi-Fi only.")
            return
        }
        if (!hasFineLocation(app) || !hasBackgroundLocation(app)) {
            markUnavailable(app, "Location permission ('Allow all the time') is missing; using Wi-Fi only.")
            return
        }

        val zones = SafeZoneStore.zones(app)
        val geofences = zones.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.lat, zone.lng, zone.radiusM)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // DWELL (not ENTER) so GPS drift at the edge cannot flap arrive/leave.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(LOITERING_DELAY_MS)
                .build()
        }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(geofences)
            .build()

        val client = LocationServices.getGeofencingClient(app)
        val pending = pendingIntent(app)
        try {
            client.removeGeofences(pending).addOnCompleteListener {
                client.addGeofences(request, pending)
                    .addOnSuccessListener {
                        SafeZoneStore.setGeofenceAvailable(app, true)
                        DebugLogger.log(app, TAG, "Registered ${geofences.size} geofence(s).", force = true)
                        if (evaluateInitial) evaluateCurrentState(app)
                        ProtectionController.refreshZone(app, force = true)
                    }
                    .addOnFailureListener { e ->
                        markUnavailable(app, "Geofence registration failed: ${e.message}")
                    }
            }
        } catch (e: SecurityException) {
            markUnavailable(app, "Geofence registration denied: ${e.message}")
        }
    }

    private fun markUnavailable(app: Context, reason: String) {
        DebugLogger.log(app, TAG, reason, force = true)
        SafeZoneStore.setGeofenceAvailable(app, false)
        SafeZoneStore.setNote(app, reason)
        ProtectionController.refreshZone(app, force = true)
    }

    /** One fresh high-accuracy fix, or null if unavailable (no permission, location off, timeout). */
    @SuppressLint("MissingPermission")
    fun fetchFreshLocation(context: Context, onResult: (Location?) -> Unit) {
        val app = context.applicationContext
        if (!hasFineLocation(app)) {
            onResult(null)
            return
        }
        val token = CancellationTokenSource()
        runCatching {
            LocationServices.getFusedLocationProviderClient(app)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                .addOnSuccessListener { location -> onResult(location) }
                .addOnFailureListener {
                    DebugLogger.log(app, TAG, "Could not get a location fix: ${it.message}", force = true)
                    onResult(null)
                }
        }.onFailure { onResult(null) }
    }

    /**
     * Takes one fresh fix and sets the inside/outside verdict for each zone, since a geofence only
     * reports transitions. Vague or mock fixes are ignored.
     */
    fun evaluateCurrentState(context: Context, onDone: ((Location?) -> Unit)? = null) {
        val app = context.applicationContext
        fetchFreshLocation(app) { location ->
            if (location != null && !isMock(location)) applyFix(app, location)
            onDone?.invoke(location)
        }
    }

    private fun applyFix(app: Context, location: Location) {
        SafeZoneStore.zones(app).forEach { zone ->
            val result = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, zone.lat, zone.lng, result)
            val verdict = GeoVerdict.resolve(result[0], location.accuracy, zone.radiusM)
            if (verdict != null) SafeZoneStore.setGeoInside(app, zone.id, verdict)
        }
        ProtectionController.refreshZone(app)
    }

    @Suppress("DEPRECATION")
    fun isMock(location: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else location.isFromMockProvider
}
