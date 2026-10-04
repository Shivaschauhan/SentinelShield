package com.sentinelshield.antitheft.safezone

/**
 * Turns one location fix into a geofence verdict using positive evidence only.
 *
 * Returns true when the whole accuracy circle is inside the zone, false when the whole accuracy
 * circle is outside it, and null when the fix is too vague to say (the previous verdict stands).
 */
object GeoVerdict {
    fun resolve(distanceM: Float, accuracyM: Float, radiusM: Float): Boolean? {
        if (accuracyM > radiusM) return null
        return when {
            distanceM + accuracyM <= radiusM -> true
            distanceM - accuracyM > radiusM -> false
            else -> null
        }
    }
}

/** A zone as the Wi-Fi matcher sees it. */
data class WifiZoneRef(val zoneId: String, val ssid: String?, val bssids: Set<String>)

sealed interface WifiVerdict {
    /** Connected to a home access point of [zoneId]; [learnBssid] is a new mesh node to remember. */
    data class Home(val zoneId: String, val learnBssid: String? = null) : WifiVerdict

    /** Connected to some other network. */
    data object NotHome : WifiVerdict

    /** Connected, but Android hid the details (missing permission or location off). */
    data object Unreadable : WifiVerdict
}

object WifiMatcher {
    /** Android returns this placeholder when the caller may not see the real BSSID. */
    const val REDACTED_BSSID = "02:00:00:00:00:00"
    private const val UNKNOWN_SSID = "<unknown ssid>"
    private val MAC = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")

    fun normalizeBssid(raw: String?): String? {
        val value = raw?.trim()?.uppercase() ?: return null
        if (value == REDACTED_BSSID || !MAC.matches(value)) return null
        return value
    }

    fun normalizeSsid(raw: String?): String? {
        val value = raw?.trim()?.removeSurrounding("\"") ?: return null
        if (value.isBlank() || value.equals(UNKNOWN_SSID, ignoreCase = true)) return null
        return value
    }

    /**
     * Matches on the access point's hardware address, never on the name alone (anyone can name a
     * hotspot after your network). A new access point with the same name is only learned while the
     * geofence also says you are inside, so both signals have to agree.
     */
    fun evaluate(zones: List<WifiZoneRef>, ssidRaw: String?, bssidRaw: String?, geoInside: Boolean?): WifiVerdict {
        val bssid = normalizeBssid(bssidRaw) ?: return WifiVerdict.Unreadable
        zones.firstOrNull { zone -> zone.bssids.any { it.equals(bssid, ignoreCase = true) } }
            ?.let { return WifiVerdict.Home(it.zoneId) }

        val ssid = normalizeSsid(ssidRaw)
        if (ssid != null && geoInside == true) {
            zones.firstOrNull { zone -> zone.bssids.isNotEmpty() && zone.ssid == ssid }
                ?.let { return WifiVerdict.Home(it.zoneId, learnBssid = bssid) }
        }
        return WifiVerdict.NotHome
    }
}
