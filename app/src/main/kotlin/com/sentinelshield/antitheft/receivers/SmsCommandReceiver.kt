package com.sentinelshield.antitheft.receivers

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.util.Log
import com.sentinelshield.antitheft.LockScreenAdminReceiver
import com.sentinelshield.antitheft.SecurityAlertService
import com.sentinelshield.antitheft.SecurityPreferences
import com.sentinelshield.antitheft.utils.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SmsCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DebugLogger.log(context, "SmsCommandReceiver", "SMS Intent Received: ${intent.action}", force = true)

        val isArmed = SecurityPreferences.isSmsControlArmed(context)
        if (!isArmed) {
            DebugLogger.log(context, "SmsCommandReceiver", "REJECTED: Remote SMS Control is currently DISARMED in settings.", force = true)
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            DebugLogger.log(context, "SmsCommandReceiver", "REJECTED: Received null or empty SMS message payload from intent.", force = true)
            return
        }

        val sender = messages[0].originatingAddress ?: "Unknown"
        val trustedContacts = SecurityPreferences.getTrustedContacts(context).filter { it.isNotBlank() }

        DebugLogger.log(context, "SmsCommandReceiver", "Incoming SMS from Sender: '$sender' | Configured Trusted Contacts: $trustedContacts", force = true)

        if (trustedContacts.isEmpty()) {
            DebugLogger.log(context, "SmsCommandReceiver", "REJECTED: SMS received from '$sender', but NO trusted contacts are configured in SentinelShield.", force = true)
            return
        }

        val fullBodyBuilder = StringBuilder()
        for (sms in messages) {
            sms.messageBody?.let { fullBodyBuilder.append(it) }
        }
        val fullBody = fullBodyBuilder.toString().uppercase(Locale.getDefault()).trim()

        DebugLogger.log(context, "SmsCommandReceiver", "Extracted SMS Body: '$fullBody' from '$sender'", force = true)

        var matchedContact: String? = null
        val isTrusted = trustedContacts.any { trusted ->
            if (trusted.isBlank()) false
            else {
                val cleanSender = sender.replace("[^0-9+]".toRegex(), "")
                val cleanTrusted = trusted.replace("[^0-9+]".toRegex(), "")
                val phoneUtilsMatch = PhoneNumberUtils.compare(sender, trusted)
                val suffixMatch = (cleanSender.length >= 7 && cleanTrusted.length >= 7 && cleanSender.endsWith(cleanTrusted))
                val exactMatch = (cleanSender.isNotEmpty() && cleanSender == cleanTrusted)
                
                val matches = phoneUtilsMatch || suffixMatch || exactMatch
                DebugLogger.log(
                    context,
                    "SmsCommandReceiver",
                    "Comparing Sender '$sender' against Trusted '$trusted' -> PhoneUtilsMatch: $phoneUtilsMatch, SuffixMatch: $suffixMatch, ExactMatch: $exactMatch => Result: $matches",
                    force = true
                )
                if (matches) matchedContact = trusted
                matches
            }
        }

        if (!isTrusted) {
            DebugLogger.log(context, "SmsCommandReceiver", "UNAUTHORIZED REJECTION: Incoming number '$sender' did NOT match any trusted contact in $trustedContacts.", force = true)
            return
        }

        DebugLogger.log(context, "SmsCommandReceiver", "AUTHORIZED SUCCESS: Number '$sender' matched trusted contact '$matchedContact'. Executing command: '$fullBody'", force = true)

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleCommand(context, sender, fullBody)
            } catch (e: Exception) {
                DebugLogger.log(context, "SmsCommandReceiver", "EXECUTION ERROR: Failed processing command '$fullBody': ${e.message}", force = true)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleCommand(context: Context, sender: String, command: String) {
        DebugLogger.log(context, "SmsCommandReceiver", "Received trusted command content: '$command' from $sender", force = true)

        val isLockCommand = listOf("LOCK", "LOCKDOWN", "LOST").any { command.contains(it) }
        val isSirenCommand = listOf("SIREN", "ALARM", "SOUND", "RING").any { command.contains(it) }
        val isTrackCommand = listOf("LOCATION", "TRACK", "GPS", "LOCATE", "WHERE").any { command.contains(it) }

        when {
            isLockCommand -> {
                DebugLogger.log(context, "SmsCommandReceiver", "Executing LOCK command: Locking screen via DeviceAdmin...", force = true)
                val dpm = context.getSystemService(DevicePolicyManager::class.java)
                val adminComponent = ComponentName(context, LockScreenAdminReceiver::class.java)
                if (dpm != null && dpm.isAdminActive(adminComponent)) {
                    dpm.lockNow()
                    sendSms(context, sender, "[SentinelShield] Device locked successfully.")
                    DebugLogger.log(context, "SmsCommandReceiver", "Device locked successfully via LOCK command.", force = true)
                } else {
                    sendSms(context, sender, "[SentinelShield] Failed: Device Admin permission not granted.")
                    DebugLogger.log(context, "SmsCommandReceiver", "LOCK command failed: Device Admin is not enabled.", force = true)
                }
            }
            isSirenCommand -> {
                DebugLogger.log(context, "SmsCommandReceiver", "Executing SIREN/ALARM command: Triggering emergency siren at 100% volume...", force = true)
                SecurityAlertService.start(context, "Remote Alarm Triggered!")
                sendSms(context, sender, "[SentinelShield] Emergency siren activated at max volume.")
            }
            isTrackCommand -> {
                DebugLogger.log(context, "SmsCommandReceiver", "Executing LOCATION/TRACK command: Requesting GPS coordinates...", force = true)
                sendSms(context, sender, "[SentinelShield] Acquiring live location, please wait...")
                trackLocation(context, sender)
            }
            else -> {
                DebugLogger.log(context, "SmsCommandReceiver", "Unrecognized command phrase: '$command'. (Supported: LOCK, SIREN, LOCATION / TRACK / GPS)", force = true)
                sendSms(context, sender, "[SentinelShield] Unrecognized command. Available commands: LOCK, SIREN, LOCATION, TRACK, GPS")
            }
        }
    }

    private fun sendSms(context: Context, destination: String, message: String) {
        val smsManager = context.getSystemService(SmsManager::class.java)
        try {
            smsManager?.sendTextMessage(destination, null, message, null, null)
            DebugLogger.log(context, "SmsCommandReceiver", "Sent SMS response to $destination: '$message'", force = true)
        } catch (e: Exception) {
            DebugLogger.log(context, "SmsCommandReceiver", "Failed to send SMS response to $destination: ${e.message}", force = true)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun trackLocation(context: Context, requesterNumber: String) {
        DebugLogger.log(context, "SmsCommandReceiver", "Beginning LOCATION tracking flow...", force = true)
        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            DebugLogger.log(context, "SmsCommandReceiver", "LocationManager service is unavailable.", force = true)
            sendSms(context, requesterNumber, "[SentinelShield] Failed: Location service unavailable on device.")
            return
        }

        try {
            var isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            var isNetOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            DebugLogger.log(context, "SmsCommandReceiver", "Initial Location State -> GPS: $isGpsOn, Network: $isNetOn", force = true)

            val isAutoEnableAllowed = SecurityPreferences.isAutoEnableLocationDataEnabled(context)
            if (isAutoEnableAllowed) {
                DebugLogger.log(context, "SmsCommandReceiver", "Auto-enable setting is ON. Enabling Location & Mobile Data in one go...", force = true)
                enableLocationAndMobileData(context)
                
                // Wait 2 seconds for Location & Data hardware services to start up
                DebugLogger.log(context, "SmsCommandReceiver", "Waiting 2s for hardware startup...", force = true)
                kotlinx.coroutines.delay(2000L)
            } else {
                DebugLogger.log(context, "SmsCommandReceiver", "Auto-enable setting is OFF. Skipping hardware toggle.", force = true)
            }

            isGpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetOn = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            DebugLogger.log(context, "SmsCommandReceiver", "Post-startup Location State -> GPS: $isGpsOn, Network: $isNetOn", force = true)

            val lastKnown = getLastKnownLocation(locationManager)

            // Attempt 1: Get initial location fix
            DebugLogger.log(context, "SmsCommandReceiver", "Attempt 1: Requesting high accuracy location fix...", force = true)
            val attempt1Fix = requestFreshLocationFix(context, locationManager)
            if (attempt1Fix != null) {
                DebugLogger.log(context, "SmsCommandReceiver", "Attempt 1 Fix: Lat=${attempt1Fix.latitude}, Lng=${attempt1Fix.longitude}, Accuracy=${attempt1Fix.accuracy}m", force = true)
            } else {
                DebugLogger.log(context, "SmsCommandReceiver", "Attempt 1: Timed out or no fix.", force = true)
            }

            // Wait 2 seconds for GPS satellites to settle
            DebugLogger.log(context, "SmsCommandReceiver", "Waiting 2s for GPS satellite precision settling...", force = true)
            kotlinx.coroutines.delay(2000L)

            // Attempt 2: Get latest location fix after settling
            DebugLogger.log(context, "SmsCommandReceiver", "Attempt 2 (Settled): Requesting latest location fix...", force = true)
            val attempt2Fix = requestFreshLocationFix(context, locationManager)
            if (attempt2Fix != null) {
                DebugLogger.log(context, "SmsCommandReceiver", "Attempt 2 (Latest) Fix: Lat=${attempt2Fix.latitude}, Lng=${attempt2Fix.longitude}, Accuracy=${attempt2Fix.accuracy}m", force = true)
            } else {
                DebugLogger.log(context, "SmsCommandReceiver", "Attempt 2: Timed out or no fix.", force = true)
            }

            // Use the LATEST location fix after the second attempt (falling back to Attempt 1 or Last Known)
            val latestLocation = attempt2Fix ?: attempt1Fix ?: lastKnown

            if (latestLocation != null) {
                val accuracyStr = if (latestLocation.hasAccuracy()) "${latestLocation.accuracy.toInt()}m" else "approx"
                val mapsLink = "https://maps.google.com/maps?q=loc:${latestLocation.latitude},${latestLocation.longitude}&z=17"
                val responseMsg = "See my real-time location on Maps (Accuracy: ~$accuracyStr):\n$mapsLink"

                DebugLogger.log(context, "SmsCommandReceiver", "Location acquired successfully (Latest Fix, Accuracy: ~$accuracyStr): $mapsLink", force = true)
                sendSms(context, requesterNumber, responseMsg)
            } else {
                DebugLogger.log(context, "SmsCommandReceiver", "Location Error: Unable to fix coordinates after retries.", force = true)
                sendSms(context, requesterNumber, "[SentinelShield] Unable to acquire location fix. GPS signal may be weak/indoor.")
            }
        } catch (e: SecurityException) {
            sendSms(context, requesterNumber, "[SentinelShield] Failed: Location permission missing on device.")
            DebugLogger.log(context, "SmsCommandReceiver", "Location tracking failed: ACCESS_FINE_LOCATION permission missing: ${e.message}", force = true)
        } catch (e: Exception) {
            sendSms(context, requesterNumber, "[SentinelShield] Failed: Location tracking error.")
            DebugLogger.log(context, "SmsCommandReceiver", "Location tracking error: ${e.message}", force = true)
        }
    }

    private fun enableLocationAndMobileData(context: Context) {
        // 1. Turn ON Location
        runCatching {
            android.provider.Settings.Secure.putInt(
                context.contentResolver,
                android.provider.Settings.Secure.LOCATION_MODE,
                android.provider.Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            )
            DebugLogger.log(context, "SmsCommandReceiver", "Enabled LOCATION_MODE_HIGH_ACCURACY via Secure Settings.", force = true)
        }.onFailure { e ->
            DebugLogger.log(context, "SmsCommandReceiver", "Location toggle attempt: ${e.message}", force = true)
        }

        runCatching {
            val locationManager = context.getSystemService(LocationManager::class.java)
            val setLocationEnabledMethod = locationManager?.javaClass?.getMethod("setLocationEnabledForUser", Boolean::class.javaPrimitiveType, android.os.UserHandle::class.java)
            setLocationEnabledMethod?.invoke(locationManager, true, android.os.Process.myUserHandle())
        }.onFailure { _ -> }

        // 2. Turn ON Mobile Data
        runCatching {
            android.provider.Settings.Global.putInt(
                context.contentResolver,
                "mobile_data",
                1
            )
            DebugLogger.log(context, "SmsCommandReceiver", "Enabled Mobile Data (mobile_data=1) via Global Settings.", force = true)
        }.onFailure { e ->
            DebugLogger.log(context, "SmsCommandReceiver", "Mobile data toggle attempt: ${e.message}", force = true)
        }

        runCatching {
            val telephonyManager = context.getSystemService(android.telephony.TelephonyManager::class.java)
            val setDataEnabledMethod = telephonyManager?.javaClass?.getDeclaredMethod("setDataEnabled", Boolean::class.javaPrimitiveType)
            setDataEnabledMethod?.isAccessible = true
            setDataEnabledMethod?.invoke(telephonyManager, true)
        }.onFailure { _ -> }

        runCatching {
            Runtime.getRuntime().exec(arrayOf("su", "-c", "svc data enable"))
        }.onFailure { _ -> }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestFreshLocationFix(context: Context, locationManager: LocationManager): Location? {
        var listener: LocationListener? = null
        return try {
            withTimeoutOrNull(4000L) {
                suspendCoroutine { continuation ->
                    var isResumed = false
                    fun resumeOnce(loc: Location?) {
                        if (!isResumed) {
                            isResumed = true
                            continuation.resume(loc)
                        }
                    }

                    val locListener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            resumeOnce(loc)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }
                    listener = locListener

                    try {
                        var requestedAny = false
                        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locListener, Looper.getMainLooper())
                            requestedAny = true
                        }
                        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locListener, Looper.getMainLooper())
                            requestedAny = true
                        }
                        if (!requestedAny) {
                            resumeOnce(null)
                        }
                    } catch (e: Exception) {
                        resumeOnce(null)
                    }
                }
            }
        } finally {
            listener?.let {
                runCatching { locationManager.removeUpdates(it) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(locationManager: LocationManager): Location? {
        return try {
            val providers = locationManager.getProviders(true)
            var bestLocation: Location? = null
            for (provider in providers) {
                val l = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }
            bestLocation
        } catch (e: Exception) {
            null
        }
    }
}
