package com.sentinelshield.antitheft

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.sentinelshield.antitheft.utils.DebugLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DecoyScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make Activity pitch-black full screen over lockscreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.decorView.post {
            hideSystemUI()
        }

        DebugLogger.log(this, "DecoyScreenActivity", "Fake Shutdown Decoy Screen displayed. Pitch-black overlay active.", force = true)

        setContent {
            val scope = rememberCoroutineScope()
            var holdJob: Job? = null

            androidx.activity.compose.BackHandler(enabled = true) {
                // Intercept back gesture on pitch-black decoy screen
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                DebugLogger.log(this@DecoyScreenActivity, "DecoyScreenActivity", "Screen press started...")
                                holdJob = scope.launch {
                                    delay(3000L) // 3-second long press requirement
                                    DebugLogger.log(this@DecoyScreenActivity, "DecoyScreenActivity", "3-second long press detected! Unlocking disarm screen.", force = true)
                                    vibrateSuccess()
                                    launchDisarmAndFinish()
                                }
                                tryAwaitRelease()
                                holdJob?.cancel()
                                DebugLogger.log(this@DecoyScreenActivity, "DecoyScreenActivity", "Screen released before 3s.")
                            }
                        )
                    }
            )
        }
    }

    private fun hideSystemUI() {
        runCatching {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun vibrateSuccess() {
        runCatching {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(100, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        }
    }

    private fun launchDisarmAndFinish() {
        try {
            val intent = Intent(this, DisarmActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            DebugLogger.log(this, "DecoyScreenActivity", "Failed to launch DisarmActivity: ${e.message}")
        }
        finish()
    }
}
