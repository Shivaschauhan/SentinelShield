package com.sentinelshield.antitheft

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentinelshield.antitheft.ui.theme.DarkModeOption
import com.sentinelshield.antitheft.ui.theme.SentinelShieldTheme
import com.sentinelshield.antitheft.utils.DebugLogger

class FakePowerMenuActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        DebugLogger.log(this, "FakePowerMenu", "Stock Android Fake Power Menu displayed.", force = true)

        setContent {
            BackHandler(enabled = true) {
                finish()
            }

            SentinelShieldTheme(darkMode = DarkModeOption.Dark) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    // Exact Stock Android / Pixel Power Menu Card
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = Color(0xFF212320),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .width(320.dp)
                            .wrapContentHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {}
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            // Row 1: Emergency & Lockdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StockPowerButtonItem(
                                    label = "Emergency",
                                    icon = Icons.Default.Emergency,
                                    containerColor = Color(0xFFFF5647),
                                    onClick = {
                                        try {
                                            startActivity(Intent(Intent.ACTION_DIAL))
                                        } catch (_: Exception) {}
                                        finish()
                                    }
                                )

                                StockPowerButtonItem(
                                    label = "Lockdown",
                                    icon = Icons.Default.Lock,
                                    containerColor = Color(0xFF383B36),
                                    onClick = { triggerFakeShutdown("Lockdown") }
                                )
                            }

                            // Row 2: Power off & Restart
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StockPowerButtonItem(
                                    label = "Power off",
                                    icon = Icons.Default.PowerSettingsNew,
                                    containerColor = Color(0xFF383B36),
                                    onClick = { triggerFakeShutdown("Power off") }
                                )

                                StockPowerButtonItem(
                                    label = "Restart",
                                    icon = Icons.Default.RestartAlt,
                                    containerColor = Color(0xFF383B36),
                                    onClick = { triggerFakeShutdown("Restart") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun triggerFakeShutdown(action: String) {
        DebugLogger.log(this, "FakePowerMenu", "Thief clicked $action -> Executing heavy vibration and launching DecoyScreen", force = true)

        // Heavy vibration pulse to simulate real phone shutdown haptics
        runCatching {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(500, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(500)
            }
        }

        // Launch Pitch-Black Decoy Screen
        try {
            val intent = Intent(this, DecoyScreenActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            DebugLogger.log(this, "FakePowerMenu", "Failed to launch DecoyScreen: ${e.message}")
        }

        finish()
    }
}

@Composable
fun StockPowerButtonItem(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .width(115.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
