package com.sentinelshield.antitheft

import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.sentinelshield.antitheft.utils.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SentinelNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        DebugLogger.log(this, "NotificationListener", "SentinelNotificationListenerService connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        DebugLogger.log(this, "NotificationListener", "SentinelNotificationListenerService disconnected. Requesting rebind.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, SentinelNotificationListenerService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        runCatching {
            val context = applicationContext
            if (!SecurityPreferences.isSmsControlArmed(context)) return

            val extras = sbn.notification?.extras ?: return
            val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
            val fullMessage = if (text.isNotBlank()) text else bigText

            if (fullMessage.isBlank()) return

            val upperMsg = fullMessage.uppercase(java.util.Locale.ROOT)
            val hasCommand = listOf("LOCK", "LOCKDOWN", "LOST", "SIREN", "ALARM", "SOUND", "RING", "LOCATION", "TRACK", "GPS", "LOCATE", "WHERE")
                .any { upperMsg.contains(it) }

            if (hasCommand) {
                DebugLogger.log(context, "NotificationListener", "Detected potential remote command in notification from '$title': '$fullMessage'", force = true)
                
                val trustedContacts = SecurityPreferences.getTrustedContacts(context).filter { it.isNotBlank() }
                if (trustedContacts.isEmpty()) return

                val senderStr = title.replace("[^0-9+]".toRegex(), "")
                val isSenderTrusted = trustedContacts.any { trusted ->
                    val cleanSender = senderStr.replace("[^0-9]".toRegex(), "")
                    val cleanTrusted = trusted.replace("[^0-9]".toRegex(), "")
                    cleanSender.isNotEmpty() && cleanTrusted.isNotEmpty() &&
                            (cleanSender.endsWith(cleanTrusted) || cleanTrusted.endsWith(cleanSender) ||
                             cleanSender.takeLast(7) == cleanTrusted.takeLast(7))
                }

                if (isSenderTrusted) {
                    DebugLogger.log(context, "NotificationListener", "Notification command AUTHORIZED from '$title'. Processing phrase...", force = true)
                    val receiver = com.sentinelshield.antitheft.receivers.SmsCommandReceiver()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        receiver.handleCommand(context, title, fullMessage)
                    }
                } else {
                    DebugLogger.log(context, "NotificationListener", "Notification command rejected: Title '$title' did not match trusted contacts.", force = true)
                }
            }
        }.onFailure { e ->
            DebugLogger.log(applicationContext, "NotificationListener", "Error processing notification: ${e.message}")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
