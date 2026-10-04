package com.sentinelshield.antitheft.safezone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoVerdictTest {
    @Test
    fun clearlyInsideAndOutside() {
        assertEquals(true, GeoVerdict.resolve(distanceM = 10f, accuracyM = 20f, radiusM = 150f))
        assertEquals(false, GeoVerdict.resolve(distanceM = 400f, accuracyM = 20f, radiusM = 150f))
    }

    @Test
    fun boundaryBandIsUndecided() {
        // 140 m away with 20 m accuracy could be on either side of a 150 m edge.
        assertNull(GeoVerdict.resolve(distanceM = 140f, accuracyM = 20f, radiusM = 150f))
        assertNull(GeoVerdict.resolve(distanceM = 160f, accuracyM = 20f, radiusM = 150f))
    }

    @Test
    fun exactEdgesAreInclusiveForInsideOnly() {
        assertEquals(true, GeoVerdict.resolve(distanceM = 130f, accuracyM = 20f, radiusM = 150f))
        assertNull(GeoVerdict.resolve(distanceM = 170f, accuracyM = 20f, radiusM = 150f))
        assertEquals(false, GeoVerdict.resolve(distanceM = 171f, accuracyM = 20f, radiusM = 150f))
    }

    @Test
    fun fixWorseThanRadiusIsIgnored() {
        assertNull(GeoVerdict.resolve(distanceM = 0f, accuracyM = 200f, radiusM = 150f))
        assertNull(GeoVerdict.resolve(distanceM = 5000f, accuracyM = 200f, radiusM = 150f))
    }
}

class WifiMatcherTest {
    private val home = WifiZoneRef("home", "HomeNet", setOf("AA:BB:CC:DD:EE:01"))

    @Test
    fun matchesOnBssidCaseInsensitively() {
        val v = WifiMatcher.evaluate(listOf(home), "\"HomeNet\"", "aa:bb:cc:dd:ee:01", geoInside = null)
        assertEquals(WifiVerdict.Home("home"), v)
    }

    @Test
    fun sameNameDifferentBssidIsNotHomeWithoutGeofenceAgreement() {
        assertEquals(WifiVerdict.NotHome, WifiMatcher.evaluate(listOf(home), "\"HomeNet\"", "11:22:33:44:55:66", geoInside = null))
        assertEquals(WifiVerdict.NotHome, WifiMatcher.evaluate(listOf(home), "\"HomeNet\"", "11:22:33:44:55:66", geoInside = false))
    }

    @Test
    fun sameNameNewBssidIsLearnedOnlyWhenInsideGeofence() {
        val v = WifiMatcher.evaluate(listOf(home), "\"HomeNet\"", "11:22:33:44:55:66", geoInside = true)
        assertEquals(WifiVerdict.Home("home", "11:22:33:44:55:66"), v)
    }

    @Test
    fun differentNameInsideGeofenceIsNotLearned() {
        assertEquals(WifiVerdict.NotHome, WifiMatcher.evaluate(listOf(home), "\"CafeWifi\"", "11:22:33:44:55:66", geoInside = true))
    }

    @Test
    fun redactedOrMissingBssidIsUnreadableNotDisconnected() {
        assertEquals(WifiVerdict.Unreadable, WifiMatcher.evaluate(listOf(home), "\"HomeNet\"", "02:00:00:00:00:00", true))
        assertEquals(WifiVerdict.Unreadable, WifiMatcher.evaluate(listOf(home), "<unknown ssid>", null, true))
        assertEquals(WifiVerdict.Unreadable, WifiMatcher.evaluate(listOf(home), "x", "not-a-mac", true))
    }

    @Test
    fun normalizers() {
        assertEquals("HomeNet", WifiMatcher.normalizeSsid("\"HomeNet\""))
        assertNull(WifiMatcher.normalizeSsid("<unknown ssid>"))
        assertNull(WifiMatcher.normalizeSsid("  "))
        assertEquals("AA:BB:CC:DD:EE:01", WifiMatcher.normalizeBssid(" aa:bb:cc:dd:ee:01 "))
        assertTrue(WifiMatcher.normalizeBssid("02:00:00:00:00:00") == null)
    }
}
