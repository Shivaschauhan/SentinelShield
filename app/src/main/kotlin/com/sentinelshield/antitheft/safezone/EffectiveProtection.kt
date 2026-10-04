package com.sentinelshield.antitheft.safezone

/** Features whose protection the zone layer may pause or add on top of the owner's own switches. */
enum class ZoneFeature { POCKET, CHARGING }

/** What the owner has switched on themselves. Safe Zones never overwrites these. */
data class UserProtection(
    val sim: Boolean,
    val pocket: Boolean,
    val chargingPersistent: Boolean,
    val chargingOneTime: Boolean,
    val intruder: Boolean,
)

/** Owner-configured automation rules. */
data class ZoneRules(
    val pausePocketAtHome: Boolean = true,
    val pauseChargingAtHome: Boolean = true,
    val armPocketWhenAway: Boolean = true,
    val armChargingWhenAway: Boolean = true,
    /** Sticky: switches the owner's SIM tamper toggle on when leaving; never switched off by a zone. */
    val armSimWhenAway: Boolean = false,
    /** Sticky: switches the owner's intruder selfie toggle on when leaving; never switched off by a zone. */
    val armIntruderWhenAway: Boolean = false,
)

/** What is actually active once the zone layer has been applied to the owner's switches. */
data class EffectiveProtection(
    val sim: Boolean,
    val pocket: Boolean,
    val chargingPersistent: Boolean,
    val chargingOneTime: Boolean,
    val intruder: Boolean,
    val pocketPausedByZone: Boolean,
    val chargingPausedByZone: Boolean,
    val pocketArmedByZone: Boolean,
    val chargingArmedByZone: Boolean,
) {
    /** Any charger alarm (persistent or one-time) is live. */
    val charging: Boolean get() = chargingPersistent || chargingOneTime

    /** Something needs the monitor service to be running. */
    val anyMonitor: Boolean get() = sim || pocket || charging
}

/**
 * Applies the zone layer to the owner's switches.
 *
 * - Only pocket snatch and the persistent charger alarm can be paused (at home).
 * - SIM tamper, intruder selfie and the one-time charger alarm are never paused.
 * - Pausing is skipped while an alarm is ringing ([pauseAllowed] = false).
 * - When away, pocket/charger can be armed by the zone unless the owner just disarmed them
 *   ([suppressed]), which lasts until the next zone change.
 */
object EffectiveProtectionPolicy {
    fun compute(
        user: UserProtection,
        zone: Zone,
        rules: ZoneRules,
        automationOn: Boolean,
        pauseAllowed: Boolean = true,
        suppressed: Set<ZoneFeature> = emptySet(),
    ): EffectiveProtection {
        val home = automationOn && zone == Zone.HOME
        val away = automationOn && zone == Zone.AWAY

        val pocketPaused = home && pauseAllowed && rules.pausePocketAtHome && user.pocket
        val chargingPaused = home && pauseAllowed && rules.pauseChargingAtHome && user.chargingPersistent

        val pocketByZone = away && rules.armPocketWhenAway && !user.pocket && ZoneFeature.POCKET !in suppressed
        val chargingByZone = away && rules.armChargingWhenAway && !user.chargingPersistent &&
            ZoneFeature.CHARGING !in suppressed

        return EffectiveProtection(
            sim = user.sim,
            pocket = (user.pocket && !pocketPaused) || pocketByZone,
            chargingPersistent = (user.chargingPersistent && !chargingPaused) || chargingByZone,
            chargingOneTime = user.chargingOneTime,
            intruder = user.intruder,
            pocketPausedByZone = pocketPaused,
            chargingPausedByZone = chargingPaused,
            pocketArmedByZone = pocketByZone,
            chargingArmedByZone = chargingByZone,
        )
    }
}
