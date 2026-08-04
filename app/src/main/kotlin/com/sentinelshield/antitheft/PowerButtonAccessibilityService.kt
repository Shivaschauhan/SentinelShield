package com.sentinelshield.antitheft

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.sentinelshield.antitheft.utils.DebugLogger

class PowerButtonAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var powerLongPressRunnable: Runnable? = null
    private var isPowerDown = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        DebugLogger.log(this, "PowerAccessibility", "PowerButtonAccessibilityService connected & active", force = true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Detect system power menu popup window state
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            val className = event.className?.toString() ?: ""
            
            if (packageName.contains("systemui", ignoreCase = true) && 
               (className.contains("globalactions", ignoreCase = true) || className.contains("power", ignoreCase = true))) {
                
                DebugLogger.log(this, "PowerAccessibility", "System Power Menu detected: $className")

                // Check if Fake Shutdown protection is enabled
                if (SecurityPreferences.isFakeShutdownEnabled(this)) {
                    DebugLogger.log(this, "PowerAccessibility", "Intercepting System Power Menu -> Launching Fake Power Menu Activity", force = true)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    launchFakePowerMenu()
                }
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return super.onKeyEvent(event)

        if (event.keyCode == KeyEvent.KEYCODE_POWER) {
            DebugLogger.log(this, "PowerAccessibility", "Power Button KeyEvent detected: action=${event.action}, repeatCount=${event.repeatCount}")

            // 1. Anti-Silence Guarantee: If an alarm is currently active, force max volume and consume key event
            if (isAlarmActive()) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    DebugLogger.log(this, "PowerAccessibility", "Power key pressed during active alarm! Forcing max volume", force = true)
                    forceMaxAlarmVolume()
                }
                return true // Consume key event to prevent silencing or turning off audio stream
            }

            // 2. Intercept Power button long press for Fake Power Menu
            if (SecurityPreferences.isFakeShutdownEnabled(this)) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                val isLocked = keyguardManager?.isKeyguardLocked == true

                if (event.action == KeyEvent.ACTION_DOWN) {
                    if (!isPowerDown) {
                        isPowerDown = true
                        powerLongPressRunnable = Runnable {
                            DebugLogger.log(this@PowerButtonAccessibilityService, "PowerAccessibility", "Power button held for 1.2s -> Launching Fake Power Menu", force = true)
                            launchFakePowerMenu()
                        }
                        // Launch fake power menu after 1.2s (1200ms)
                        handler.postDelayed(powerLongPressRunnable!!, 1200L)
                    }

                    if (event.isLongPress || event.repeatCount > 2 || (event.flags and KeyEvent.FLAG_LONG_PRESS) != 0 || isLocked) {
                        powerLongPressRunnable?.let { handler.removeCallbacks(it) }
                        powerLongPressRunnable = null
                        DebugLogger.log(this, "PowerAccessibility", "Power button long press/repeat -> Launching Fake Power Menu", force = true)
                        launchFakePowerMenu()
                        return true
                    }
                } else if (event.action == KeyEvent.ACTION_UP) {
                    isPowerDown = false
                    powerLongPressRunnable?.let { handler.removeCallbacks(it) }
                    powerLongPressRunnable = null
                }
            }
        }

        return super.onKeyEvent(event)
    }

    private fun isAlarmActive(): Boolean {
        return SecurityAlertService.isRunning
    }

    private fun forceMaxAlarmVolume() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
        } catch (e: Exception) {
            DebugLogger.log(this, "PowerAccessibility", "Failed to force alarm volume: ${e.message}")
        }
    }

    private fun launchFakePowerMenu() {
        try {
            val intent = Intent(this, FakePowerMenuActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            DebugLogger.log(this, "PowerAccessibility", "Failed to launch FakePowerMenuActivity: ${e.message}")
            launchDecoyScreen()
        }
    }

    private fun launchDecoyScreen() {
        try {
            val intent = Intent(this, DecoyScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            DebugLogger.log(this, "PowerAccessibility", "Failed to launch DecoyScreenActivity: ${e.message}")
        }
    }

    private fun launchDisarmActivity() {
        try {
            val intent = Intent(this, DisarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            DebugLogger.log(this, "PowerAccessibility", "Failed to launch DisarmActivity: ${e.message}")
        }
    }

    override fun onInterrupt() {
        DebugLogger.log(this, "PowerAccessibility", "PowerButtonAccessibilityService interrupted")
    }

    companion object {
        fun isServiceEnabled(context: Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val expectedService = "${context.packageName}/${PowerButtonAccessibilityService::class.java.canonicalName}"
            return enabledServices.contains(expectedService)
        }
    }
}
