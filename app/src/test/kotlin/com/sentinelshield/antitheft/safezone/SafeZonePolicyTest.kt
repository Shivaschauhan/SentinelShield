package com.sentinelshield.antitheft.safezone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZonePolicyTest {
    private val now = 1_000_000L
    private val grace = ZonePolicy.WIFI_GRACE_MS

    private fun signals(
        wifiConfigured: Boolean = true,
        wifiConnected: Boolean? = null,
        wifiLostAtMs: Long? = null,
        geoInside: Boolean? = null,
    ) = ZoneSignals(wifiConfigured, wifiConnected, wifiLostAtMs, geoInside)

    @Test
    fun connectedToHomeWifiIsHomeRegardlessOfGeofence() {
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConnected = true, geoInside = false), now))
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConnected = true, geoInside = null), now))
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConnected = true, geoInside = true), now))
    }

    @Test
    fun geofenceOutsideIsAwayImmediately() {
        assertEquals(Zone.AWAY, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = now - 1_000, geoInside = false), now))
        assertEquals(Zone.AWAY, ZonePolicy.resolve(signals(wifiConfigured = false, geoInside = false), now))
    }

    @Test
    fun insideGeofenceWithoutHomeWifiConfiguredIsHome() {
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConfigured = false, geoInside = true), now))
    }

    @Test
    fun insideGeofenceWithUnreadableWifiFallsBackToGeofence() {
        // wifiConnected == null means "cannot tell" (e.g. permission missing), not "disconnected".
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConnected = null, geoInside = true), now))
    }

    @Test
    fun insideGeofenceWifiLostIsHomeDuringGraceThenAway() {
        val lost = now - (grace - 1)
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = lost, geoInside = true), now))
        val lostLongAgo = now - grace
        assertEquals(Zone.AWAY, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = lostLongAgo, geoInside = true), now))
    }

    @Test
    fun insideGeofenceWifiOffWithNoLossTimeIsAway() {
        assertEquals(Zone.AWAY, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = null, geoInside = true), now))
    }

    @Test
    fun noGeofenceVerdictWifiLostBecomesAwayAfterGrace() {
        assertEquals(Zone.HOME, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = now - 10_000, geoInside = null), now))
        assertEquals(Zone.AWAY, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = now - grace - 1, geoInside = null), now))
    }

    @Test
    fun nothingKnownIsUnknown() {
        assertEquals(Zone.UNKNOWN, ZonePolicy.resolve(signals(wifiConfigured = false, geoInside = null), now))
        assertEquals(Zone.UNKNOWN, ZonePolicy.resolve(signals(wifiConnected = null, geoInside = null), now))
        // Wi-Fi off from the start with no recorded loss time and no geofence: still unknown.
        assertEquals(Zone.UNKNOWN, ZonePolicy.resolve(signals(wifiConnected = false, wifiLostAtMs = null, geoInside = null), now))
    }

    @Test
    fun nextDeadlineOnlyWhileInGraceWindow() {
        val inGrace = signals(wifiConnected = false, wifiLostAtMs = now - 20_000, geoInside = true)
        assertEquals(grace - 20_000, ZonePolicy.nextDeadlineDelayMs(inGrace, now))
        assertNull(ZonePolicy.nextDeadlineDelayMs(signals(wifiConnected = false, wifiLostAtMs = now - grace, geoInside = true), now))
        assertNull(ZonePolicy.nextDeadlineDelayMs(signals(wifiConnected = true, wifiLostAtMs = now - 5_000), now))
        assertNull(ZonePolicy.nextDeadlineDelayMs(signals(wifiConfigured = false, wifiConnected = false, wifiLostAtMs = now - 5_000), now))
        assertNull(ZonePolicy.nextDeadlineDelayMs(signals(wifiConnected = false, wifiLostAtMs = null), now))
    }
}

class EffectiveProtectionPolicyTest {
    private val rules = ZoneRules()

    private fun user(
        sim: Boolean = false,
        pocket: Boolean = false,
        chargingPersistent: Boolean = false,
        chargingOneTime: Boolean = false,
        intruder: Boolean = false,
    ) = UserProtection(sim, pocket, chargingPersistent, chargingOneTime, intruder)

    private fun compute(
        user: UserProtection,
        zone: Zone,
        rules: ZoneRules = this.rules,
        automationOn: Boolean = true,
        pauseAllowed: Boolean = true,
        suppressed: Set<ZoneFeature> = emptySet(),
    ) = EffectiveProtectionPolicy.compute(user, zone, rules, automationOn, pauseAllowed, suppressed)

    @Test
    fun atHomePocketAndPersistentChargerArePaused() {
        val e = compute(user(pocket = true, chargingPersistent = true), Zone.HOME)
        assertFalse(e.pocket)
        assertFalse(e.chargingPersistent)
        assertTrue(e.pocketPausedByZone)
        assertTrue(e.chargingPausedByZone)
        assertFalse(e.anyMonitor)
    }

    @Test
    fun atHomeSimIntruderAndOneTimeChargerAreNeverPaused() {
        val e = compute(user(sim = true, intruder = true, chargingOneTime = true, pocket = true), Zone.HOME)
        assertTrue(e.sim)
        assertTrue(e.intruder)
        assertTrue(e.chargingOneTime)
        assertTrue(e.charging)
        assertTrue(e.anyMonitor)
        assertFalse(e.pocket)
    }

    @Test
    fun pausesCanBeDisabledPerFeature() {
        val r = ZoneRules(pausePocketAtHome = false, pauseChargingAtHome = true)
        val e = compute(user(pocket = true, chargingPersistent = true), Zone.HOME, r)
        assertTrue(e.pocket)
        assertFalse(e.chargingPersistent)
    }

    @Test
    fun pauseDoesNotApplyWhileAlarmIsRinging() {
        val e = compute(user(pocket = true, chargingPersistent = true), Zone.HOME, pauseAllowed = false)
        assertTrue(e.pocket)
        assertTrue(e.chargingPersistent)
        assertFalse(e.pocketPausedByZone)
    }

    @Test
    fun pausingNeverTurnsOnAFeatureTheOwnerLeftOff() {
        val e = compute(user(), Zone.HOME)
        assertFalse(e.pocket)
        assertFalse(e.pocketPausedByZone)
        assertFalse(e.pocketArmedByZone)
    }

    @Test
    fun awayArmsPocketAndChargerOnTopOfOwnerSwitches() {
        val e = compute(user(), Zone.AWAY)
        assertTrue(e.pocket)
        assertTrue(e.chargingPersistent)
        assertTrue(e.pocketArmedByZone)
        assertTrue(e.chargingArmedByZone)
        assertTrue(e.anyMonitor)
    }

    @Test
    fun awayDoesNotReportZoneArmedWhenOwnerAlreadyArmed() {
        val e = compute(user(pocket = true, chargingPersistent = true), Zone.AWAY)
        assertTrue(e.pocket)
        assertFalse(e.pocketArmedByZone)
        assertFalse(e.chargingArmedByZone)
    }

    @Test
    fun awayRespectsDisabledRulesAndOwnerSwitches() {
        val r = ZoneRules(armPocketWhenAway = false, armChargingWhenAway = false)
        val e = compute(user(), Zone.AWAY, r)
        assertFalse(e.pocket)
        assertFalse(e.chargingPersistent)
        val own = compute(user(pocket = true), Zone.AWAY, r)
        assertTrue(own.pocket)
    }

    @Test
    fun suppressedFeaturesAreNotReArmedByTheZone() {
        val e = compute(user(), Zone.AWAY, suppressed = setOf(ZoneFeature.POCKET))
        assertFalse(e.pocket)
        assertTrue(e.chargingPersistent)
    }

    @Test
    fun unknownZoneOrAutomationOffMeansOwnerSwitchesOnly() {
        val owner = user(pocket = true, chargingPersistent = true)
        for (e in listOf(
            compute(owner, Zone.UNKNOWN),
            compute(owner, Zone.HOME, automationOn = false),
            compute(user(), Zone.AWAY, automationOn = false),
        )) {
            assertFalse(e.pocketPausedByZone)
            assertFalse(e.chargingPausedByZone)
            assertFalse(e.pocketArmedByZone)
            assertFalse(e.chargingArmedByZone)
        }
        assertTrue(compute(owner, Zone.UNKNOWN).pocket)
        assertTrue(compute(owner, Zone.HOME, automationOn = false).chargingPersistent)
        assertFalse(compute(user(), Zone.AWAY, automationOn = false).pocket)
    }
}
