package com.sentinelshield.antitheft

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.IOException

/** Plays alarm audio only during a user-triggered or tamper-triggered incident. No wake lock is held. */
class SecurityAlertService : Service() {
    companion object {
        const val EXTRA_REASON = "reason"
        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context, reason: String): Boolean {
            val intent = Intent(context, SecurityAlertService::class.java).putExtra(EXTRA_REASON, reason)
            return try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                true
            } catch (e: Exception) {
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SecurityAlertService", "Background FGS start restricted on Android 12+: ${e.message}")
                SecurityNotifier.postTamperNotification(context, reason)
                val disarmIntent = Intent(context, DisarmActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                try {
                    context.startActivity(disarmIntent)
                } catch (_: Exception) {}
                false
            }
        }
    }

    private var player: MediaPlayer? = null
    private var isStrobeRunning = false
    private var strobeHandler = Handler(Looper.getMainLooper())
    private var strobeRunnable: Runnable? = null
    private var torchOn = false

    private var volumeHandler = Handler(Looper.getMainLooper())
    private var volumeRunnable: Runnable? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                // Legitimate device owner unlocked the phone via native lockscreen (PIN/Pattern/Fingerprint)
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SecurityAlertService", "Alarm disarmed via native phone unlock.", force = true)
                SecurityPreferences.setOneTimeChargingArmed(context, false)
                SecurityPreferences.setPocketArmed(context, false)
                stopSelf()
                SecurityMonitorService.start(context)
                android.widget.Toast.makeText(context, "Alarm Disarmed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        registerReceiver(userPresentReceiver, filter)

        // Acquire CPU WakeLock to guarantee CPU stays active during alarm even when screen turns off
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "SentinelShield::SecurityAlertWakeLock")
            wakeLock?.acquire(30 * 60 * 1000L) // 30 mins max safety limit
        } catch (e: Exception) {
            android.util.Log.w("SecurityAlertService", "Could not acquire WakeLock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val reason = intent?.getStringExtra(EXTRA_REASON)
            ?.takeIf { it.isNotBlank() }
            ?: "Security alarm is active."
        com.sentinelshield.antitheft.utils.DebugLogger.log(this, "SecurityAlertService", "Alarm Service Started. Reason: $reason")
        SecurityNotifier.createChannels(this)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    SecurityNotifier.ALERT_ID,
                    SecurityNotifier.alertNotification(this, reason),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            } else {
                startForeground(SecurityNotifier.ALERT_ID, SecurityNotifier.alertNotification(this, reason))
            }
        } catch (e: Exception) {
            android.util.Log.w("SecurityAlertService", "Could not start as foreground service", e)
            SecurityNotifier.postTamperNotification(this, reason)
        }
        RingerController.restoreNormalRinger(this)

        // Request Audio Focus to override all audio ducking/muting from other apps
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val focusRequest = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(null, android.media.AudioManager.STREAM_ALARM, android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            }
        } catch (_: Exception) {}

        // Continuously enforce maximum alarm volume across ALL alarm triggers
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        volumeRunnable = object : Runnable {
            override fun run() {
                try {
                    audioManager.setStreamVolume(
                        android.media.AudioManager.STREAM_ALARM,
                        audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM),
                        0
                    )
                    volumeHandler.postDelayed(this, 500)
                } catch (_: Exception) {}
            }
        }
        volumeHandler.post(volumeRunnable!!)

        startAlarm(reason)

        val isPocket = reason.contains("pocket", ignoreCase = true)
        if (isPocket && SecurityPreferences.isPocketUseStrobe(this)) {
            startStrobe()
        }

        notifyWearOs("triggered")
        return START_STICKY
    }

    private fun notifyWearOs(status: String) {
        try {
            val messageClient = com.google.android.gms.wearable.Wearable.getMessageClient(this)
            val nodeClient = com.google.android.gms.wearable.Wearable.getNodeClient(this)

            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                for (node in nodes) {
                    messageClient.sendMessage(node.id, "/sentinel/alarm", status.toByteArray())
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SecurityAlertService", "Failed to notify WearOS device", e)
        }
    }

    private fun startAlarm(reason: String) {
        val soundUriString = when {
            reason.contains("pocket", ignoreCase = true) -> SecurityPreferences.getPocketAlarmRingtone(this)
            reason.contains("charger", ignoreCase = true) || reason.contains("charging", ignoreCase = true) -> SecurityPreferences.getChargingAlarmRingtone(this)
            reason.contains("SIM", ignoreCase = true) -> SecurityPreferences.getSimAlarmRingtone(this)
            else -> SecurityPreferences.getPocketAlarmRingtone(this)
        }
        val soundUri = Uri.parse(soundUriString)

        try {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                isLooping = true
                setOnCompletionListener { mp ->
                    try {
                        mp.seekTo(0)
                        mp.start()
                    } catch (_: Exception) {
                        try { mp.start() } catch (_: Exception) {}
                    }
                }
                setOnErrorListener { mp, _, _ ->
                    try {
                        mp.reset()
                        mp.setDataSource(applicationContext, soundUri)
                        mp.prepare()
                        mp.isLooping = true
                        mp.start()
                    } catch (_: Exception) {}
                    true
                }
                start()
            }
        } catch (e: Exception) {
            android.util.Log.e("SecurityAlertService", "Failed to play custom alarm sound, falling back", e)
            try {
                val fallbackUri = Settings.System.DEFAULT_ALARM_ALERT_URI
                    ?: Settings.System.DEFAULT_RINGTONE_URI
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(applicationContext, fallbackUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    isLooping = true
                    setOnCompletionListener { mp ->
                        try {
                            mp.seekTo(0)
                            mp.start()
                        } catch (_: Exception) {
                            try { mp.start() } catch (_: Exception) {}
                        }
                    }
                    setOnErrorListener { mp, _, _ ->
                        try {
                            mp.reset()
                            mp.setDataSource(applicationContext, fallbackUri)
                            mp.prepare()
                            mp.isLooping = true
                            mp.start()
                        } catch (_: Exception) {}
                        true
                    }
                    start()
                }
            } catch (e2: Exception) {
                android.util.Log.e("SecurityAlertService", "Fallback alarm audio failed", e2)
            }
        }
    }

    private fun startStrobe() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager ?: return
        val cameraId = try {
            cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        } ?: return

        isStrobeRunning = true
        strobeRunnable = object : Runnable {
            override fun run() {
                if (!isStrobeRunning) return
                try {
                    torchOn = !torchOn
                    cameraManager.setTorchMode(cameraId, torchOn)
                } catch (_: Exception) {}
                strobeHandler.postDelayed(this, 150)
            }
        }
        strobeHandler.post(strobeRunnable!!)
    }

    private fun stopStrobe() {
        isStrobeRunning = false
        strobeRunnable?.let { strobeHandler.removeCallbacks(it) }
        strobeRunnable = null
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        isRunning = false
        volumeRunnable?.let { volumeHandler.removeCallbacks(it) }
        volumeRunnable = null
        stopStrobe()
        try {
            unregisterReceiver(userPresentReceiver)
        } catch (_: Exception) {}
        try {
            player?.stop()
            player?.release()
        } catch (_: Exception) {}
        player = null
        wakeLock?.let {
            try {
                if (it.isHeld) it.release()
            } catch (_: Exception) {}
        }
        wakeLock = null
        notifyWearOs("stopped")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/** Persistent foreground service that manages all background security monitors. */
class SecurityMonitorService : Service() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SecurityMonitorService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("SecurityMonitorService", "Failed to start service", e)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SecurityMonitorService::class.java))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val recheck = Runnable { evaluateSimStates() }
    private var telephonyManager: TelephonyManager? = null
    private var subscriptionManager: SubscriptionManager? = null
    private var subscriptionListener: SubscriptionManager.OnSubscriptionsChangedListener? = null
    private var simBroadcastReceiver: BroadcastReceiver? = null
    private var lastSimStates: IntArray? = null
    private var receiverRegistered = false

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var accelerometer: Sensor? = null
    private var wasNear = false
    private var isSensorListenerRegistered = false
    private var isAccelerometerRegistered = false
    private var pocketArmingRunnable: Runnable? = null
    private var pocketGraceRunnable: Runnable? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private var isSignificantMotion = false
    private var waitingForMotion = false
    private val resetMotionRunnable = Runnable {
        isSignificantMotion = false
        waitingForMotion = false
        unregisterAccelerometer()
    }

    private var proximityDebounceRunnable: Runnable? = null
    private var farStartTime: Long = 0

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                com.sentinelshield.antitheft.utils.DebugLogger.log(context, "SecurityMonitorService", "User unlocked phone. Cancelling pocket grace period and stopping vibration.")
                cancelPocketGracePeriod()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
            registerReceiver(userPresentReceiver, filter)
        } catch (_: Exception) {}
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values.isEmpty()) return

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                if (waitingForMotion) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val acceleration = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                    val gForce = Math.abs(acceleration - SensorManager.GRAVITY_EARTH)

                    if (gForce > 1.5f) { // Adjusted for sensitive snatch detection
                        isSignificantMotion = true
                        waitingForMotion = false
                        handler.removeCallbacks(resetMotionRunnable)
                        handler.postDelayed(resetMotionRunnable, 3000)
                    }
                }
                return
            }

            if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
                val distance = event.values[0]
                val maxRange = proximitySensor?.maximumRange ?: 5f
                val threshold = (maxRange / 2f).coerceAtMost(5f).coerceAtLeast(0.5f)
                val isNear = distance < threshold

                if (isNear) {
                    proximityDebounceRunnable?.let { handler.removeCallbacks(it) }
                    proximityDebounceRunnable = null
                    wasNear = true
                    waitingForMotion = false
                    unregisterAccelerometer()
                    if (farStartTime > 0 && System.currentTimeMillis() - farStartTime < 1000) {
                        cancelPocketGracePeriod()
                    }
                } else if (wasNear) {
                    if (proximityDebounceRunnable == null) {
                        proximityDebounceRunnable = Runnable {
                            wasNear = false
                            waitingForMotion = true
                            registerAccelerometer() // Dynamically register accelerometer ONLY during potential snatch!

                            handler.postDelayed({
                                if (isSignificantMotion || !wasNear) {
                                    farStartTime = System.currentTimeMillis()
                                    startPocketGracePeriod()
                                } else {
                                    // False alarm flutter, assumed stationary
                                    wasNear = true
                                    unregisterAccelerometer()
                                }
                            }, 400)
                        }
                        handler.postDelayed(proximityDebounceRunnable!!, 100)
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun registerAccelerometer() {
        if (!isAccelerometerRegistered) {
            accelerometer?.let {
                sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_GAME)
                isAccelerometerRegistered = true
            }
        }
    }

    private fun unregisterAccelerometer() {
        if (isAccelerometerRegistered) {
            accelerometer?.let {
                sensorManager?.unregisterListener(sensorEventListener, it)
            }
            isAccelerometerRegistered = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val isSimArmed = SecurityPreferences.isArmed(this)
        val isPocketArmed = SecurityPreferences.isPocketArmed(this)
        val isChargingMonitorActive = SecurityPreferences.isChargingMonitorActive(this)

        if (!isSimArmed && !isPocketArmed && !isChargingMonitorActive) {
            stopSelf()
            return START_NOT_STICKY
        }
        SecurityNotifier.createChannels(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SecurityNotifier.MONITOR_ID,
                SecurityNotifier.monitorNotification(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(SecurityNotifier.MONITOR_ID, SecurityNotifier.monitorNotification(this))
        }

        if (isSimArmed) {
            initializeMonitoring()
        } else {
            stopSimMonitoring()
        }

        if (isPocketArmed) {
            initializePocketMonitoring()
        } else {
            stopPocketMonitoring()
        }

        if (isChargingMonitorActive) {
            ChargingMonitor.start(this)
        } else {
            ChargingMonitor.stop(this)
        }

        return START_STICKY
    }

    private fun initializeMonitoring() {
        if (telephonyManager != null ||
            !packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY_SUBSCRIPTION)
        ) {
            return
        }
        telephonyManager = getSystemService(TelephonyManager::class.java) ?: return
        subscriptionManager = getSystemService(SubscriptionManager::class.java)
        lastSimStates = readSimStates()

        val oldSnapshot = SecurityPreferences.getSimSnapshot(this)
        val initialStates = lastSimStates ?: intArrayOf()
        val currentSnapshot = snapshotOf(initialStates)

        // Filter transient boot states (UNKNOWN / NOT_READY)
        if (oldSnapshot.isNotEmpty() && differsByRemoval(oldSnapshot, initialStates)) {
            val hasValidPrevious = oldSnapshot.split(',').any {
                val st = it.toIntOrNull()
                st != null && st != TelephonyManager.SIM_STATE_UNKNOWN && st != TelephonyManager.SIM_STATE_NOT_READY && st != TelephonyManager.SIM_STATE_ABSENT
            }
            if (hasValidPrevious) {
                raiseSimRemovalAlert("SIM state changed while protection was active.")
            }
        }
        SecurityPreferences.saveSimSnapshot(this, currentSnapshot)
        registerLegacySimBroadcast()
        registerSubscriptionListener()
    }

    private fun registerLegacySimBroadcast() {
        simBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = scheduleRecheck()
        }
        val filter = IntentFilter("android.intent.action.SIM_STATE_CHANGED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(simBroadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(simBroadcastReceiver, filter)
        }
        receiverRegistered = true
    }

    private fun registerSubscriptionListener() {
        val manager = subscriptionManager ?: return
        subscriptionListener = object : SubscriptionManager.OnSubscriptionsChangedListener() {
            override fun onSubscriptionsChanged() = scheduleRecheck()
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                manager.addOnSubscriptionsChangedListener(mainExecutor, subscriptionListener!!)
            } else {
                @Suppress("DEPRECATION")
                manager.addOnSubscriptionsChangedListener(subscriptionListener!!)
            }
        } catch (_: SecurityException) {
            // Legacy broadcast remains as fallback
        }
    }

    private fun scheduleRecheck() {
        handler.removeCallbacks(recheck)
        handler.postDelayed(recheck, 1_500L)
    }

    private fun evaluateSimStates() {
        if (!SecurityPreferences.isArmed(this)) return
        val current = readSimStates()
        val previousStates = lastSimStates
        if (previousStates != null) {
            for (slot in 0 until minOf(previousStates.size, current.size)) {
                if (isRemoval(previousStates[slot], current[slot])) {
                    raiseSimRemovalAlert("SIM card removed from slot ${slot + 1}.")
                    break
                }
            }
        }
        lastSimStates = current
        SecurityPreferences.saveSimSnapshot(this, snapshotOf(current))
    }

    private fun readSimStates(): IntArray {
        val manager = telephonyManager ?: return intArrayOf(TelephonyManager.SIM_STATE_UNKNOWN)
        val slotCount = try {
            @Suppress("DEPRECATION")
            maxOf(1, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) manager.activeModemCount else manager.phoneCount)
        } catch (_: SecurityException) {
            1
        }
        return IntArray(slotCount) { slot ->
            try {
                manager.getSimState(slot)
            } catch (_: RuntimeException) {
                TelephonyManager.SIM_STATE_UNKNOWN
            }
        }
    }

    private fun differsByRemoval(oldSnapshot: String, current: IntArray): Boolean {
        val previous = oldSnapshot.split(',')
        for (slot in 0 until minOf(previous.size, current.size)) {
            val previousState = previous[slot].toIntOrNull() ?: continue
            if (isRemoval(previousState, current[slot])) return true
        }
        return false
    }

    private fun isRemoval(previous: Int, current: Int): Boolean =
        previous != TelephonyManager.SIM_STATE_UNKNOWN &&
            previous != TelephonyManager.SIM_STATE_NOT_READY &&
            previous != TelephonyManager.SIM_STATE_ABSENT &&
            current == TelephonyManager.SIM_STATE_ABSENT

    private fun snapshotOf(states: IntArray): String = states.joinToString(",")

    private fun raiseSimRemovalAlert(reason: String) {
        com.sentinelshield.antitheft.utils.DebugLogger.log(this, "SimTamper", "SIM Removal Alert Triggered: $reason")
        SecurityPreferences.recordAlert(this, reason)
        SecurityAlertService.start(this, reason)
    }

    private fun raisePocketRemovalAlert(reason: String) {
        com.sentinelshield.antitheft.utils.DebugLogger.log(this, "PocketSnatch", "Pocket Removal Alert Triggered: $reason")
        SecurityPreferences.recordAlert(this, reason)
        SecurityAlertService.start(this, reason)
    }

    private fun startPocketGracePeriod() {
        if (pocketGraceRunnable != null) return

        val gracePeriodSeconds = SecurityPreferences.getPocketGracePeriod(this)
        com.sentinelshield.antitheft.utils.DebugLogger.log(this, "PocketSnatch", "Starting grace period timer (${gracePeriodSeconds}s)")
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator

        val runnable = Runnable {
            pocketGraceRunnable = null
            unregisterAccelerometer()

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            if (keyguardManager?.isKeyguardLocked == false) {
                com.sentinelshield.antitheft.utils.DebugLogger.log(this, "PocketSnatch", "Grace period ended but phone is unlocked. Cancelling alarm and vibration.")
                cancelPocketGracePeriod()
                return@Runnable
            }

            runCatching {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                v?.cancel()
            }

            raisePocketRemovalAlert("Phone removed from pocket!")
        }
        pocketGraceRunnable = runnable
        handler.postDelayed(runnable, gracePeriodSeconds * 1000L)

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 500), 0)
            }
        }.onFailure { e ->
            com.sentinelshield.antitheft.utils.DebugLogger.log(this, "PocketSnatch", "Vibration failed: ${e.message}")
        }
    }

    private fun cancelPocketGracePeriod() {
        if (pocketGraceRunnable != null) {
            com.sentinelshield.antitheft.utils.DebugLogger.log(this, "PocketSnatch", "Grace period cancelled (phone returned to pocket)")
        }
        pocketGraceRunnable?.let { handler.removeCallbacks(it) }
        pocketGraceRunnable = null
        unregisterAccelerometer()
        runCatching {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            vibrator?.cancel()
        }
    }

    private fun initializePocketMonitoring() {
        if (isSensorListenerRegistered || pocketArmingRunnable != null) return

        val armingDelaySeconds = SecurityPreferences.getPocketArmingDelay(this)
        val runnable = Runnable {
            pocketArmingRunnable = null
            if (sensorManager == null) {
                sensorManager = getSystemService(SensorManager::class.java)
            }
            if (proximitySensor == null) {
                proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            }
            if (accelerometer == null) {
                accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            }

            if (wakeLock == null) {
                val powerManager = getSystemService(android.os.PowerManager::class.java)
                wakeLock = powerManager?.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "SentinelShield::PocketMonitorWakeLock"
                )
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire() // Continuous WakeLock while armed, released on stop
            }

            proximitySensor?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL, 100_000)
                } else {
                    sensorManager?.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL)
                }
                isSensorListenerRegistered = true

                // Initial proximity state starts false until sensor reports NEAR
                wasNear = false
            }
        }
        pocketArmingRunnable = runnable
        handler.postDelayed(runnable, armingDelaySeconds * 1000L)
    }

    private fun stopPocketMonitoring() {
        pocketArmingRunnable?.let { handler.removeCallbacks(it) }
        pocketArmingRunnable = null
        cancelPocketGracePeriod()

        if (isSensorListenerRegistered) {
            sensorManager?.unregisterListener(sensorEventListener)
            isSensorListenerRegistered = false
            isAccelerometerRegistered = false
            wasNear = false
        }

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
    }

    private fun stopSimMonitoring() {
        handler.removeCallbacks(recheck)
        if (receiverRegistered) {
            simBroadcastReceiver?.let {
                runCatching { unregisterReceiver(it) }
            }
            simBroadcastReceiver = null
            receiverRegistered = false
        }
        subscriptionListener?.let {
            try {
                subscriptionManager?.removeOnSubscriptionsChangedListener(it)
            } catch (_: Exception) {}
        }
        subscriptionListener = null
        telephonyManager = null
        subscriptionManager = null
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(userPresentReceiver) }
        stopSimMonitoring()
        stopPocketMonitoring()
        ChargingMonitor.stop(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
