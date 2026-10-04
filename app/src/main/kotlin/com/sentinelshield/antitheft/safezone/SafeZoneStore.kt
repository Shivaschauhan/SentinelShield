package com.sentinelshield.antitheft.safezone

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** One saved place. Rules are global, a zone only describes where it is and how to recognise it. */
data class SafeZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lng: Double,
    val radiusM: Float = SafeZoneStore.DEFAULT_RADIUS_M,
    val ssid: String? = null,
    val bssids: Set<String> = emptySet(),
)

/** Persistent configuration and runtime state for Safe Zones (separate file from SecurityPreferences). */
object SafeZoneStore {
    const val MIN_RADIUS_M = 100f
    const val MAX_RADIUS_M = 500f
    const val DEFAULT_RADIUS_M = 150f
    const val MAX_BSSIDS_PER_ZONE = 16

    private const val FILE = "sentinel_safezone"
    private const val ENABLED = "enabled"
    private const val ZONES = "zones"
    private const val RULE_PAUSE_POCKET = "rule_pause_pocket"
    private const val RULE_PAUSE_CHARGING = "rule_pause_charging"
    private const val RULE_ARM_POCKET = "rule_arm_pocket"
    private const val RULE_ARM_CHARGING = "rule_arm_charging"
    private const val RULE_ARM_SIM = "rule_arm_sim"
    private const val RULE_ARM_INTRUDER = "rule_arm_intruder"
    private const val GEO_INSIDE_PREFIX = "geo_inside_"
    private const val GEO_AVAILABLE = "geo_available"
    private const val WIFI_CONNECTED = "wifi_connected"
    private const val WIFI_LOST_AT = "wifi_lost_at"
    private const val PAUSED_UNTIL = "paused_until"
    private const val CURRENT_ZONE = "current_zone"
    private const val SUPPRESSED = "suppressed"
    private const val NOTE = "note"

    const val FILE_NAME = FILE

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    // ---- Master switch -------------------------------------------------------------------

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(ENABLED, enabled).apply()
    }

    /** Enabled and at least one place saved: the monitor service has Safe Zone work to do. */
    fun isActive(context: Context): Boolean = isEnabled(context) && zones(context).isNotEmpty()

    // ---- Zones ---------------------------------------------------------------------------

    @Synchronized
    fun zones(context: Context): List<SafeZone> {
        val raw = prefs(context).getString(ZONES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                val bssids = o.optJSONArray("bssids")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() }
                    ?: emptySet()
                SafeZone(
                    id = o.getString("id"),
                    name = o.optString("name", "Home"),
                    lat = o.getDouble("lat"),
                    lng = o.getDouble("lng"),
                    radiusM = o.optDouble("radius", DEFAULT_RADIUS_M.toDouble()).toFloat()
                        .coerceIn(MIN_RADIUS_M, MAX_RADIUS_M),
                    ssid = o.optString("ssid", "").ifBlank { null },
                    bssids = bssids,
                )
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun saveZones(context: Context, zones: List<SafeZone>) {
        val array = JSONArray()
        zones.forEach { z ->
            array.put(
                JSONObject()
                    .put("id", z.id)
                    .put("name", z.name)
                    .put("lat", z.lat)
                    .put("lng", z.lng)
                    .put("radius", z.radiusM.toDouble())
                    .put("ssid", z.ssid ?: "")
                    .put("bssids", JSONArray(z.bssids.toList())),
            )
        }
        prefs(context).edit().putString(ZONES, array.toString()).apply()
    }

    @Synchronized
    fun upsertZone(context: Context, zone: SafeZone) {
        val current = zones(context).toMutableList()
        val index = current.indexOfFirst { it.id == zone.id }
        if (index >= 0) current[index] = zone else current.add(zone)
        saveZones(context, current)
    }

    @Synchronized
    fun removeZone(context: Context, id: String) {
        saveZones(context, zones(context).filterNot { it.id == id })
        prefs(context).edit().remove(GEO_INSIDE_PREFIX + id).apply()
    }

    /** Adds [bssid] to the zone's learned access points (capped). Returns true if it was new. */
    @Synchronized
    fun learnBssid(context: Context, zoneId: String, bssid: String): Boolean {
        val zone = zones(context).firstOrNull { it.id == zoneId } ?: return false
        if (bssid in zone.bssids || zone.bssids.size >= MAX_BSSIDS_PER_ZONE) return false
        upsertZone(context, zone.copy(bssids = zone.bssids + bssid))
        return true
    }

    // ---- Rules ---------------------------------------------------------------------------

    fun getRules(context: Context): ZoneRules {
        val p = prefs(context)
        val d = ZoneRules()
        return ZoneRules(
            pausePocketAtHome = p.getBoolean(RULE_PAUSE_POCKET, d.pausePocketAtHome),
            pauseChargingAtHome = p.getBoolean(RULE_PAUSE_CHARGING, d.pauseChargingAtHome),
            armPocketWhenAway = p.getBoolean(RULE_ARM_POCKET, d.armPocketWhenAway),
            armChargingWhenAway = p.getBoolean(RULE_ARM_CHARGING, d.armChargingWhenAway),
            armSimWhenAway = p.getBoolean(RULE_ARM_SIM, d.armSimWhenAway),
            armIntruderWhenAway = p.getBoolean(RULE_ARM_INTRUDER, d.armIntruderWhenAway),
        )
    }

    fun setRules(context: Context, rules: ZoneRules) {
        prefs(context).edit()
            .putBoolean(RULE_PAUSE_POCKET, rules.pausePocketAtHome)
            .putBoolean(RULE_PAUSE_CHARGING, rules.pauseChargingAtHome)
            .putBoolean(RULE_ARM_POCKET, rules.armPocketWhenAway)
            .putBoolean(RULE_ARM_CHARGING, rules.armChargingWhenAway)
            .putBoolean(RULE_ARM_SIM, rules.armSimWhenAway)
            .putBoolean(RULE_ARM_INTRUDER, rules.armIntruderWhenAway)
            .apply()
    }

    // ---- Geofence runtime state ----------------------------------------------------------

    /** False when Play services is missing or registration failed: Safe Zones is Wi-Fi only. */
    fun isGeofenceAvailable(context: Context): Boolean = prefs(context).getBoolean(GEO_AVAILABLE, true)

    fun setGeofenceAvailable(context: Context, available: Boolean) {
        prefs(context).edit().putBoolean(GEO_AVAILABLE, available).apply()
    }

    fun setGeoInside(context: Context, zoneId: String, inside: Boolean?) {
        val editor = prefs(context).edit()
        if (inside == null) editor.remove(GEO_INSIDE_PREFIX + zoneId) else editor.putBoolean(GEO_INSIDE_PREFIX + zoneId, inside)
        editor.apply()
    }

    fun clearGeoState(context: Context) {
        val p = prefs(context)
        val editor = p.edit()
        p.all.keys.filter { it.startsWith(GEO_INSIDE_PREFIX) }.forEach { editor.remove(it) }
        editor.apply()
    }

    /** True if inside any zone, false if outside all of them, null if any verdict is missing. */
    fun geoInside(context: Context): Boolean? {
        if (!isGeofenceAvailable(context)) return null
        val zones = zones(context)
        if (zones.isEmpty()) return null
        val p = prefs(context)
        val verdicts = zones.map { z ->
            if (p.contains(GEO_INSIDE_PREFIX + z.id)) p.getBoolean(GEO_INSIDE_PREFIX + z.id, false) else null
        }
        return when {
            verdicts.any { it == true } -> true
            verdicts.all { it == false } -> false
            else -> null
        }
    }

    // ---- Wi-Fi runtime state -------------------------------------------------------------

    /** null = cannot tell (no permission / no Wi-Fi info). */
    fun setWifiConnected(context: Context, connected: Boolean?) {
        val editor = prefs(context).edit()
        if (connected == null) editor.remove(WIFI_CONNECTED) else editor.putBoolean(WIFI_CONNECTED, connected)
        editor.apply()
    }

    fun wifiConnected(context: Context): Boolean? {
        val p = prefs(context)
        return if (p.contains(WIFI_CONNECTED)) p.getBoolean(WIFI_CONNECTED, false) else null
    }

    fun setWifiLostAt(context: Context, atMs: Long?) {
        val editor = prefs(context).edit()
        if (atMs == null) editor.remove(WIFI_LOST_AT) else editor.putLong(WIFI_LOST_AT, atMs)
        editor.apply()
    }

    fun wifiLostAt(context: Context): Long? {
        val p = prefs(context)
        return if (p.contains(WIFI_LOST_AT)) p.getLong(WIFI_LOST_AT, 0L) else null
    }

    fun signals(context: Context): ZoneSignals {
        val configured = zones(context).any { it.bssids.isNotEmpty() }
        return ZoneSignals(
            wifiConfigured = configured,
            wifiConnected = if (configured) wifiConnected(context) else null,
            wifiLostAtMs = wifiLostAt(context),
            geoInside = geoInside(context),
        )
    }

    // ---- Manual pause --------------------------------------------------------------------

    fun pausedUntil(context: Context): Long = prefs(context).getLong(PAUSED_UNTIL, 0L)

    fun setPausedUntil(context: Context, untilMs: Long) {
        prefs(context).edit().putLong(PAUSED_UNTIL, untilMs).apply()
    }

    fun isPaused(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs < pausedUntil(context)

    /** Automation only acts while enabled, configured and not manually paused. */
    fun automationOn(context: Context, nowMs: Long = System.currentTimeMillis()): Boolean =
        isActive(context) && !isPaused(context, nowMs)

    // ---- Resolved zone cache and layer suppression ----------------------------------------

    fun currentZone(context: Context): Zone =
        runCatching { Zone.valueOf(prefs(context).getString(CURRENT_ZONE, null) ?: "") }.getOrDefault(Zone.UNKNOWN)

    fun setCurrentZone(context: Context, zone: Zone) {
        prefs(context).edit().putString(CURRENT_ZONE, zone.name).apply()
    }

    fun suppressed(context: Context): Set<ZoneFeature> =
        (prefs(context).getStringSet(SUPPRESSED, emptySet()) ?: emptySet())
            .mapNotNull { runCatching { ZoneFeature.valueOf(it) }.getOrNull() }
            .toSet()

    fun suppress(context: Context, feature: ZoneFeature) {
        val updated = (suppressed(context) + feature).map { it.name }.toSet()
        prefs(context).edit().putStringSet(SUPPRESSED, updated).apply()
    }

    fun clearSuppressed(context: Context) {
        prefs(context).edit().remove(SUPPRESSED).apply()
    }

    // ---- Last automation note (shown in settings) ------------------------------------------

    fun note(context: Context): String = prefs(context).getString(NOTE, "") ?: ""

    fun setNote(context: Context, note: String) {
        prefs(context).edit().putString(NOTE, note).apply()
    }
}
