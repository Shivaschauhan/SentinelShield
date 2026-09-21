package com.sentinelshield.antitheft

import com.sentinelshield.antitheft.receivers.RemoteCommand
import com.sentinelshield.antitheft.receivers.SmsCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCommandParserTest {

    @Test
    fun testLockCommands() {
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("LOCK"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("lock"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("LOCKDOWN"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("LOST"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("SECURE"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("#LOCK"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("Please LOCK the phone now!"))
        assertEquals(RemoteCommand.LOCK, SmsCommandParser.parse("[SentinelShield] LOCK"))
    }

    @Test
    fun testLockFalsePositivesArePrevented() {
        // Words containing "LOCK" as substring must NOT trigger lock
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("What time is it on the clock?"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("CLOCK"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("UNLOCK"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("Please unlock the door"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("BLOCK"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("FLOCK"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("INTERLOCK"))
    }

    @Test
    fun testSirenCommands() {
        assertEquals(RemoteCommand.SIREN, SmsCommandParser.parse("SIREN"))
        assertEquals(RemoteCommand.SIREN, SmsCommandParser.parse("siren"))
        assertEquals(RemoteCommand.SIREN, SmsCommandParser.parse("ALARM"))
        assertEquals(RemoteCommand.SIREN, SmsCommandParser.parse("SOUND"))
        assertEquals(RemoteCommand.SIREN, SmsCommandParser.parse("RING"))
        assertEquals(RemoteCommand.SIREN, SmsCommandParser.parse("Sound the ALARM immediately!"))
    }

    @Test
    fun testSirenFalsePositivesArePrevented() {
        // Words containing "RING" or "ALARM" as substring must NOT trigger siren
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("Please bring my coat"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("BRING"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("STRING"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("SPRING"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("DURING the meeting"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("HEARING"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("BORING"))
    }

    @Test
    fun testStopSirenPriority() {
        // Stop commands must override siren keywords
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("STOP"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("SILENCE"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("DISARM"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("STOP SIREN"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("STOP ALARM"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("SILENCE SIREN"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("CANCEL ALARM"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("turn OFF the siren"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("PLEASE STOP"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("STOP NOW"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("TURN OFF"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("MUTE"))
        assertEquals(RemoteCommand.STOP_SIREN, SmsCommandParser.parse("QUIET"))
    }

    @Test
    fun testLocationCommands() {
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("LOCATION"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("location"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("TRACK"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("GPS"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("LOCATE"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("WHERE"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("FIND"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("MAP"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("COORDINATES"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("Where is my phone?"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("Please find my phone"))
        assertEquals(RemoteCommand.LOCATION, SmsCommandParser.parse("#GPS"))
    }

    @Test
    fun testLocationFalsePositivesArePrevented() {
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("What is our budget allocation?"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("ALLOCATION"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("DISLOCATION"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("Listening to the soundtrack"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("SOUNDTRACK"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("BACKTRACK"))
        assertEquals(RemoteCommand.UNKNOWN, SmsCommandParser.parse("TRACKSUIT"))
    }

    @Test
    fun testHelpCommands() {
        assertEquals(RemoteCommand.HELP, SmsCommandParser.parse("HELP"))
        assertEquals(RemoteCommand.HELP, SmsCommandParser.parse("COMMANDS"))
        assertEquals(RemoteCommand.HELP, SmsCommandParser.parse("STATUS"))
        assertEquals(RemoteCommand.HELP, SmsCommandParser.parse("INFO"))
    }

    @Test
    fun testContactMatching() {
        // Exact
        assertTrue(SmsCommandParser.isContactMatch("+919876543210", "+919876543210"))
        assertTrue(SmsCommandParser.isContactMatch("9876543210", "9876543210"))

        // Country code prefix differences
        assertTrue(SmsCommandParser.isContactMatch("+919876543210", "9876543210"))
        assertTrue(SmsCommandParser.isContactMatch("9876543210", "+919876543210"))

        // Domestic trunk prefix 0 vs international
        assertTrue(SmsCommandParser.isContactMatch("09876543210", "+919876543210"))
        assertTrue(SmsCommandParser.isContactMatch("+919876543210", "09876543210"))

        // Formatted contact numbers
        assertTrue(SmsCommandParser.isContactMatch("+1 (555) 123-4567", "+15551234567"))
        assertTrue(SmsCommandParser.isContactMatch("555-123-4567", "5551234567"))

        // Different numbers should NOT match
        assertFalse(SmsCommandParser.isContactMatch("+15551234567", "+15559994567"))
        assertFalse(SmsCommandParser.isContactMatch("1234567890", "9994567890"))

        // Length difference boundary: difference > 4 digits must not match even if suffix matches
        assertFalse(SmsCommandParser.isContactMatch("1234567", "9999991234567")) // 7 digits vs 13 digits (diff = 6)
        assertTrue(SmsCommandParser.isContactMatch("9876543210", "00919876543210")) // 10 digits vs 14 digits (diff = 4)

        // Numbers shorter than 7 digits must not suffix-match
        assertFalse(SmsCommandParser.isContactMatch("123456", "9876543210"))

        // Blank or non-numeric
        assertFalse(SmsCommandParser.isContactMatch("", "+919876543210"))
        assertFalse(SmsCommandParser.isContactMatch("Mom", "+919876543210"))
        assertFalse(SmsCommandParser.isContactMatch("Unknown", "+919876543210"))
    }
}
