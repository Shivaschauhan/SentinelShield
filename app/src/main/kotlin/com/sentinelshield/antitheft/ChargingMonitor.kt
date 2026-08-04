package com.sentinelshield.antitheft

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat

object ChargingMonitor {
    private var isRegistered = false
    private var wakeLock: PowerManager.WakeLock? = null
    private const val ALARM_REQ_CODE = 1001

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (SecurityPreferences.isChargingMonitorActive(context)) {
                        com.sentinelshield.antitheft.utils.DebugLogger.log(context, "ChargingMonitor", "Power disconnected. Active delay: ${SecurityPreferences.getChargingAlarmDelaySeconds(context)}s")
                        cancelPendingAlarm(context)

                        val delaySeconds = SecurityPreferences.getChargingAlarmDelaySeconds(context)

                        // Safely release previous WakeLock to prevent leaks
                        wakeLock?.let { if (it.isHeld) it.release() }

                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        wakeLock = powerManager?.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "SentinelShield::ChargingMonitorDelay"
                        )
                        val lockDurationMs = (delaySeconds + 10) * 1000L
                        wakeLock?.acquire(lockDurationMs)

                        if (delaySeconds <= 0) {
                            wakeLock?.let { if (it.isHeld) it.release() }
                            SecurityAlertService.start(context, "Charger unplugged!")
                        } else {
                            scheduleExactAlarm(context, delaySeconds)
                        }
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    com.sentinelshield.antitheft.utils.DebugLogger.log(context, "ChargingMonitor", "Power connected. Cancelling unplug alarm.")
                    cancelPendingAlarm(context)
                    wakeLock?.let { if (it.isHeld) it.release() }
                }
            }
        }
    }

    private fun scheduleExactAlarm(context: Context, delaySeconds: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerTimeMs = System.currentTimeMillis() + (delaySeconds * 1000L)

        val intent = Intent(context, SecurityAlertService::class.java).apply {
            putExtra(SecurityAlertService.EXTRA_REASON, "Charger unplugged!")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, ALARM_REQ_CODE, intent, flags)
        } else {
            PendingIntent.getService(context, ALARM_REQ_CODE, intent, flags)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
            }
            Log.d("ChargingMonitor", "Scheduled exact unplug alarm for $delaySeconds seconds from now.")
        } catch (e: Exception) {
            Log.e("ChargingMonitor", "Failed to schedule exact alarm, launching fallback", e)
            SecurityAlertService.start(context, "Charger unplugged!")
        }
    }

    private fun cancelPendingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, SecurityAlertService::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, ALARM_REQ_CODE, intent, flags)
        } else {
            PendingIntent.getService(context, ALARM_REQ_CODE, intent, flags)
        }

        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
            Log.d("ChargingMonitor", "Cancelled pending unplug alarm.")
        }
    }

    fun start(context: Context) {
        if (!isRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_POWER_CONNECTED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    context.applicationContext,
                    receiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.applicationContext.registerReceiver(receiver, filter)
            }
            isRegistered = true
            Log.d("ChargingMonitor", "ChargingMonitor receiver registered successfully.")
        }
    }

    fun stop(context: Context) {
        if (isRegistered) {
            try {
                context.applicationContext.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if not registered
            }
            cancelPendingAlarm(context)
            wakeLock?.let { if (it.isHeld) it.release() }
            isRegistered = false
            Log.d("ChargingMonitor", "ChargingMonitor stopped.")
        }
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE_ONETIME_CHARGE = "com.sentinelshield.antitheft.TOGGLE_ONETIME_CHARGE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE_ONETIME_CHARGE) {
            val isCurrentlyArmed = SecurityPreferences.isOneTimeChargingArmed(context)
            val turningOn = !isCurrentlyArmed
            SecurityPreferences.setOneTimeChargingArmed(context, turningOn)

            if (!turningOn) {
                // If we are disarming, also stop the alarm if it's ringing
                context.stopService(Intent(context, SecurityAlertService::class.java))
            }

            // Restart the monitor service so it updates the notification and listeners
            SecurityMonitorService.start(context)
        }
    }
}
