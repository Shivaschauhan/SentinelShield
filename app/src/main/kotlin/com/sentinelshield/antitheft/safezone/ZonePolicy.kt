package com.sentinelshield.antitheft.safezone

/** Where the owner is, as far as Safe Zones can tell. */
enum class Zone { HOME, AWAY, UNKNOWN }

/**
 * Raw location signals. All fields are plain values so [ZonePolicy] stays free of Android
 * dependencies and can be unit-tested on the JVM.
 *
 * @property wifiConfigured at least one home access point has been saved.
 * @property wifiConnected connected to a saved home access point right now; null when it cannot be
 * determined (for example the location permission needed to read the BSSID is missing).
 * @property wifiLostAtMs when the home access point was last lost, or null if never observed.
 * @property geoInside last geofence verdict; null when unknown (no fix yet, location off, no Play
 * services).
 */
data class ZoneSignals(
    val wifiConfigured: Boolean,
    val wifiConnected: Boolean?,
    val wifiLostAtMs: Long?,
    val geoInside: Boolean?,
)

/**
 * Combines the home Wi-Fi and geofence signals into a [Zone].
 *
 * Bias: slow to relax, quick to arm. HOME needs a positive signal; AWAY is declared as soon as a
 * strong signal drops. A wrong AWAY costs a notification, a wrong HOME could cost the phone.
 */
object ZonePolicy {
    /** Time the home Wi-Fi may be gone (router reboot, brief drop-out) before it counts as left. */
    const val WIFI_GRACE_MS = 60_000L

    fun resolve(signals: ZoneSignals, nowMs: Long, graceMs: Long = WIFI_GRACE_MS): Zone {
        if (signals.wifiConfigured && signals.wifiConnected == true) return Zone.HOME

        val wifiAbsent = signals.wifiConfigured && signals.wifiConnected == false
        val lostAt = signals.wifiLostAtMs
        val inGrace = lostAt != null && nowMs - lostAt < graceMs

        return when (signals.geoInside) {
            false -> Zone.AWAY
            true -> if (!wifiAbsent || inGrace) Zone.HOME else Zone.AWAY
            // No geofence verdict: a lost home Wi-Fi is still a strong "left" signal.
            null -> when {
                wifiAbsent && lostAt != null -> if (inGrace) Zone.HOME else Zone.AWAY
                else -> Zone.UNKNOWN
            }
        }
    }

    /**
     * Milliseconds until [resolve] could change purely because time passed (the Wi-Fi grace
     * window closing), or null if nothing is pending.
     */
    fun nextDeadlineDelayMs(signals: ZoneSignals, nowMs: Long, graceMs: Long = WIFI_GRACE_MS): Long? {
        val lostAt = signals.wifiLostAtMs ?: return null
        if (!signals.wifiConfigured || signals.wifiConnected != false) return null
        val remaining = graceMs - (nowMs - lostAt)
        return if (remaining > 0) remaining else null
    }
}
