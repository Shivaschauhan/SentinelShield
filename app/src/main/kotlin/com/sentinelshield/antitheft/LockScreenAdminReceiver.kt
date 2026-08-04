package com.sentinelshield.antitheft

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LockScreenAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private var lastCaptureTime = 0L
        private const val COOLDOWN_MS = 15_000L
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        
        if (!SecurityPreferences.isIntruderSelfieArmed(context)) {
            com.sentinelshield.antitheft.utils.DebugLogger.log(context, "LockScreenAdmin", "Password failed attempt detected, but Intruder Selfie is DISARMED.", force = true)
            return
        }

        val manager = getManager(context)
        
        val failedAttempts = try {
            manager.getCurrentFailedPasswordAttempts()
        } catch (e: Exception) {
            1
        }
        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "LockScreenAdmin", "Password failed attempt detected. Failed count: $failedAttempts", force = true)

        if (failedAttempts >= 2) {
            val now = System.currentTimeMillis()
            if (now - lastCaptureTime < COOLDOWN_MS) {
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "LockScreenAdmin", "Intruder capture is on 15s cooldown, skipping duplicate trigger.", force = true)
                return
            }
            lastCaptureTime = now
            
            // Launch the transparent IntruderActivity to record the video or photo
            val intruderIntent = Intent(context, IntruderActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            try {
                context.startActivity(intruderIntent)
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "LockScreenAdmin", "Launched IntruderActivity to capture evidence.", force = true)
            } catch (e: Exception) {
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "LockScreenAdmin", "Failed to start IntruderActivity from background (BAL restriction): ${e.message}", force = true)
                SecurityNotifier.postTamperNotification(context, "Intruder attempt detected! Tap to open camera.")
            }
        }
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "LockScreenAdmin", "Lock screen password succeeded.", force = true)
    }
}
