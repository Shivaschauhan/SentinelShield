package com.sentinelshield.antitheft

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer
import com.sentinelshield.antitheft.ui.theme.DarkModeOption
import com.sentinelshield.antitheft.ui.theme.SentinelShieldTheme

@OptIn(ExperimentalMaterial3Api::class)
class DisarmActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Lock activity strictly over lockscreen & keep screen on
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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            BackHandler(enabled = true) {
                // Intercept and consume back button to prevent escaping disarm prompt
            }

            SentinelShieldTheme(
                darkMode = DarkModeOption.Dark,
                dynamicColor = true
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_shield_3d),
                                            contentDescription = null,
                                            modifier = Modifier.padding(7.dp)
                                        )
                                    }
                                    Text(
                                        text = "SentinelShield Security",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.background
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Alarm Status Hero Badge Card
                            RoundedCardContainer(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp,
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onError,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Protection Lock Engaged",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = "Authentication required to stop alarm and unlock phone.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }

                            // Disarm Instructions Card
                            RoundedCardContainer(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "Device Verification",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "Please enter your registered Device PIN, Pattern, Password, or Biometric fingerprint to disarm SentinelShield.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Bottom Actions Section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { requestDeviceCredential() },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Authenticate & Disarm", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "Secured by SentinelShield Anti-Theft Protection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Intercept back gesture on disarm prompt
            }
        })

        // Post to decorView to ensure window view root is fully attached and drawn
        window.decorView.post {
            hideSystemUI()
        }
        window.decorView.postDelayed({
            requestDeviceCredential()
        }, 350L)
    }

    private fun hideSystemUI() {
        runCatching {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun requestDeviceCredential() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                                 BiometricManager.Authenticators.DEVICE_CREDENTIAL
                                 
            val biometricManager = BiometricManager.from(this)
            val authStatus = biometricManager.canAuthenticate(authenticators)
            if (authStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                // Only disarm if they explicitly have no secure lock screen set up
                stopAlarmAndFinish()
                return
            } else if (authStatus != BiometricManager.BIOMETRIC_SUCCESS) {
                android.widget.Toast.makeText(this, "Biometric unavailable. Tap Authenticate to try again.", android.widget.Toast.LENGTH_SHORT).show()
                return
            }
            
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Disarm SentinelShield")
                .setSubtitle("Confirm your device credential to disarm protection.")
                .setAllowedAuthenticators(authenticators)
                .build()
                
            val biometricPrompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        stopAlarmAndFinish()
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }
                }
            )
            
            biometricPrompt.authenticate(promptInfo)
        } else {
            val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
            if (!keyguardManager.isDeviceSecure) {
                stopAlarmAndFinish()
                return
            }
            
            @Suppress("DEPRECATION")
            val intent = keyguardManager.createConfirmDeviceCredentialIntent("Disarm Protection", "Confirm your credential to stop the alarm.")
            if (intent != null) {
                @Suppress("DEPRECATION")
                startActivityForResult(intent, 1001)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            stopAlarmAndFinish()
        }
    }

    private fun stopAlarmAndFinish() {
        SecurityPreferences.setOneTimeChargingArmed(this, false)
        SecurityPreferences.setPocketArmed(this, false)
        
        stopService(Intent(this, SecurityAlertService::class.java))
        SecurityMonitorService.start(this) // refresh monitor notification
        
        android.widget.Toast.makeText(this, "Alarm Disarmed", android.widget.Toast.LENGTH_SHORT).show()
        finish()
    }
}
