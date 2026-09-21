package com.sentinelshield.antitheft.receivers

import java.util.Locale

enum class RemoteCommand {
    LOCK,
    SIREN,
    STOP_SIREN,
    LOCATION,
    HELP,
    UNKNOWN
}

object SmsCommandParser {
    fun parse(rawMessage: String): RemoteCommand {
        val upperMsg = rawMessage.uppercase(Locale.ROOT)
        val tokens = upperMsg.split("[^A-Z0-9]".toRegex()).filter { it.isNotBlank() }

        if (tokens.isEmpty()) return RemoteCommand.UNKNOWN

        val stopKeywords = setOf("STOP", "SILENCE", "DISARM", "OFF", "CANCEL", "MUTE", "QUIET")
        val sirenKeywords = setOf("SIREN", "ALARM", "SOUND", "RING", "BUZZ")
        val isStop = tokens.any { it in stopKeywords }
        val isSirenWord = tokens.any { it in sirenKeywords }

        val hasOtherAction = tokens.any { it in setOf("LOCK", "LOCKDOWN", "LOST", "SECURE", "LOCATION", "TRACK", "GPS", "LOCATE", "WHERE", "FIND", "COORDINATES", "MAP", "HELP", "COMMANDS", "STATUS", "INFO") }

        if (isStop && (isSirenWord || !hasOtherAction || tokens.size == 1)) {
            return RemoteCommand.STOP_SIREN
        }

        if (tokens.any { it in setOf("LOCK", "LOCKDOWN", "LOST", "SECURE") }) {
            return RemoteCommand.LOCK
        }

        if (isSirenWord) {
            return RemoteCommand.SIREN
        }

        if (tokens.any { it in setOf("LOCATION", "TRACK", "GPS", "LOCATE", "WHERE", "FIND", "COORDINATES", "MAP") }) {
            return RemoteCommand.LOCATION
        }

        if (tokens.any { it in setOf("HELP", "COMMANDS", "STATUS", "INFO") }) {
            return RemoteCommand.HELP
        }

        return RemoteCommand.UNKNOWN
    }

    fun isContactMatch(sender: String, trusted: String): Boolean {
        val cleanSender = sender.replace("[^0-9]".toRegex(), "").trimStart('0')
        val cleanTrusted = trusted.replace("[^0-9]".toRegex(), "").trimStart('0')
        if (cleanSender.isEmpty() || cleanTrusted.isEmpty()) return false
        if (cleanSender == cleanTrusted) return true
        if (cleanSender.length >= 7 && cleanTrusted.length >= 7) {
            val lenDiff = Math.abs(cleanSender.length - cleanTrusted.length)
            if (lenDiff <= 4 && (cleanSender.endsWith(cleanTrusted) || cleanTrusted.endsWith(cleanSender))) {
                return true
            }
        }
        return false
    }
}
