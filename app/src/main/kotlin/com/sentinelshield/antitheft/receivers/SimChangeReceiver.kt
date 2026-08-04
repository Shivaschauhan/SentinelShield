package com.sentinelshield.antitheft.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.sentinelshield.antitheft.SecurityPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SimChangeReceiver : BroadcastReceiver() {
    
    companion object {
        private var lastProcessTime = 0L
        private const val DEBOUNCE_MS = 5_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            val simStateExtra = intent.getStringExtra("ss") ?: "UNKNOWN"
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "SIM_STATE_CHANGED broadcast received (Extra State: $simStateExtra)", force = true)

            val now = System.currentTimeMillis()
            if (now - lastProcessTime < DEBOUNCE_MS) {
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "Debounced rapid SIM state change broadcast (${now - lastProcessTime}ms < ${DEBOUNCE_MS}ms)", force = true)
                return
            }
            lastProcessTime = now

            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Small delay to allow modem state to stabilize
                    delay(3000)
                    
                    val subscriptionManager = context.getSystemService(SubscriptionManager::class.java)
                    val activeSubscriptionInfoList = try {
                        subscriptionManager?.activeSubscriptionInfoList
                    } catch (e: SecurityException) {
                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "READ_PHONE_STATE permission missing for activeSubscriptionInfoList: ${e.message}", force = true)
                        null
                    }
                    
                    if (activeSubscriptionInfoList.isNullOrEmpty()) {
                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "No active SIM subscriptions detected (SIM removed or slot empty).", force = true)
                        pendingResult.finish()
                        return@launch
                    }

                    // Composite SIM ID: slotIndex + subscriptionId to detect slot swaps
                    val currentIds = activeSubscriptionInfoList.map { info ->
                        val slot = info.simSlotIndex
                        val subId = info.subscriptionId
                        "slot${slot}_sub${subId}"
                    }.toSet()

                    val savedIds = SecurityPreferences.getSavedSubscriptionIds(context)
                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "Active SIM IDs: $currentIds | Trusted Saved IDs: $savedIds", force = true)

                    if (savedIds.isEmpty()) {
                        // First time, save snapshot
                        SecurityPreferences.setSavedSubscriptionIds(context, currentIds)
                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "First-time setup: Registered initial SIM snapshot: $currentIds", force = true)
                    } else {
                        // Check if any new SIM or slot arrangement was detected
                        val newSims = currentIds.subtract(savedIds)
                        if (newSims.isNotEmpty()) {
                            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "ALERT: New SIM or slot swap detected! New SIMs: $newSims", force = true)
                            SecurityPreferences.setSavedSubscriptionIds(context, currentIds)

                            for (newSimId in newSims) {
                                val simInfo = activeSubscriptionInfoList.firstOrNull { info ->
                                    val slot = info.simSlotIndex
                                    val subId = info.subscriptionId
                                    newSimId.contains("slot${slot}_sub${subId}")
                                } ?: activeSubscriptionInfoList.firstOrNull()

                                val newCarrierName = simInfo?.carrierName?.toString() ?: "Unknown Carrier"
                                val newPhoneNumber = getPhoneNumber(context, subscriptionManager, simInfo?.subscriptionId ?: -1)

                                sendAlertSms(context, newCarrierName, newPhoneNumber)
                            }
                        } else {
                            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "SIM state verified. Current SIMs match trusted saved snapshot.", force = true)
                        }
                    }
                } catch (e: Exception) {
                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "Error processing SIM change: ${e.message}", force = true)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun getPhoneNumber(context: Context, subscriptionManager: SubscriptionManager?, subId: Int): String {
        if (subscriptionManager == null) return "Unknown Number"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                subscriptionManager.getPhoneNumber(subId) ?: "Unknown Number"
            } else {
                @Suppress("DEPRECATION")
                val telephonyManager = context.getSystemService(TelephonyManager::class.java)
                telephonyManager?.line1Number ?: "Unknown Number"
            }
        } catch (e: SecurityException) {
            "Unknown Number"
        }
    }

    private fun sendAlertSms(context: Context, carrierName: String, phoneNumber: String) {
        val simTamperContact = SecurityPreferences.getSimTamperContact(context)
        
        if (simTamperContact.isBlank()) {
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "Alert SMS skipped: Emergency contact is not set in SIM Tamper settings.", force = true)
            return
        }

        val message = "[SentinelShield] SECURITY ALERT: Protected device detected SIM card change!\n" +
                "New Carrier: $carrierName\n" +
                "New Phone Number: $phoneNumber"

        val smsManager = context.getSystemService(SmsManager::class.java)
        try {
            smsManager?.sendTextMessage(simTamperContact, null, message, null, null)
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "Emergency SIM alert SMS sent successfully to $simTamperContact!", force = true)
        } catch (e: Exception) {
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SimChangeReceiver", "Failed to send emergency SIM alert SMS to $simTamperContact: ${e.message}", force = true)
        }
    }
}
