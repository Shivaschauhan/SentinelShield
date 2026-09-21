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

            val command = com.sentinelshield.antitheft.receivers.SmsCommandParser.parse(fullMessage)
            if (command != com.sentinelshield.antitheft.receivers.RemoteCommand.UNKNOWN) {
                DebugLogger.log(context, "NotificationListener", "Detected potential remote command $command in notification from '$title': '$fullMessage'", force = true)
                
                val trustedContacts = SecurityPreferences.getTrustedContacts(context).filter { it.isNotBlank() }
                if (trustedContacts.isEmpty()) return

                val senderStr = title.replace("[^0-9+]".toRegex(), "")
                val cleanSender = senderStr.replace("[^0-9]".toRegex(), "")
                var matchedContact: String? = null
                var isSenderTrusted = false

                // 1. Check direct phone number match from title
                if (cleanSender.length >= 7) {
                    isSenderTrusted = trustedContacts.any { trusted ->
                        val matches = com.sentinelshield.antitheft.receivers.SmsCommandParser.isContactMatch(senderStr, trusted)
                        if (matches) matchedContact = trusted
                        matches
                    }
                }

                // 2. If title is a contact display name (e.g. "Mom" or "John"), resolve contact name via ContactsContract
                if (!isSenderTrusted && title.isNotBlank()) {
                    val resolvedNumbers = resolvePhoneNumbersForContactName(context, title)
                    for (num in resolvedNumbers) {
                        for (trusted in trustedContacts) {
                            if (com.sentinelshield.antitheft.receivers.SmsCommandParser.isContactMatch(num, trusted)) {
                                isSenderTrusted = true
                                matchedContact = trusted
                                break
                            }
                        }
                        if (isSenderTrusted) break
                    }
                }

                if (isSenderTrusted && matchedContact != null) {
                    val responseDestination = if (senderStr.startsWith("+")) senderStr else matchedContact
                    DebugLogger.log(context, "NotificationListener", "Notification command AUTHORIZED from '$title' (Target destination: '$responseDestination'). Processing...", force = true)
                    val receiver = com.sentinelshield.antitheft.receivers.SmsCommandReceiver()
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        receiver.handleCommand(context, responseDestination, fullMessage)
                    }
                } else {
                    DebugLogger.log(context, "NotificationListener", "Notification command rejected: Title '$title' did not match trusted contacts.", force = true)
                }
            }
        }.onFailure { e ->
            DebugLogger.log(applicationContext, "NotificationListener", "Error processing notification: ${e.message}")
        }
    }

    private fun resolvePhoneNumbersForContactName(context: android.content.Context, contactName: String): List<String> {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        val numbers = mutableListOf<String>()
        try {
            val projection = arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
            val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ? OR ${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} = ?"
            val selectionArgs = arrayOf(contactName.trim(), contactName.trim())

            context.contentResolver.query(
                android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val numIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    if (numIdx != -1) {
                        cursor.getString(numIdx)?.let { numbers.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            DebugLogger.log(context, "NotificationListener", "Error resolving contact name '$contactName': ${e.message}")
        }
        return numbers
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}
