package com.sentinelshield.antitheft.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer
import com.sentinelshield.antitheft.utils.GoogleDriveSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveSettingsScreen(
    onConnectAccount: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var isConnected by remember { mutableStateOf(GoogleDriveSyncManager.isUserSignedIn(context)) }
    var accountEmail by remember { mutableStateOf(GoogleDriveSyncManager.getSignedInAccountEmail(context)) }
    var isTestingUpload by remember { mutableStateOf(false) }

    val refreshState = {
        isConnected = GoogleDriveSyncManager.isUserSignedIn(context)
        accountEmail = GoogleDriveSyncManager.getSignedInAccountEmail(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sha1Fingerprint = remember { GoogleDriveSyncManager.getSigningCertificateSha1(context) }
    val packageName = context.packageName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Google Drive Cloud Backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            RoundedCardContainer {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier
                                    .size(40.dp)
                                    .padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = if (isConnected) "Google Account Connected" else "Account Not Connected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isConnected) accountEmail ?: "Connected" else "Sign in to enable cloud backup",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Badge
                        Surface(
                            shape = CircleShape,
                            color = if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isConnected) "ACTIVE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    if (isConnected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isTestingUpload) return@OutlinedButton
                                    isTestingUpload = true
                                    coroutineScope.launch {
                                        val testContent = "SentinelShield Cloud Connectivity Test - ${System.currentTimeMillis()}\nStatus: Verified\n"
                                        val testBytes = testContent.toByteArray(Charsets.UTF_8)
                                        val success = GoogleDriveSyncManager.uploadBytes(
                                            context,
                                            testBytes,
                                            "sentinel_connection_test_${System.currentTimeMillis()}.txt",
                                            "text/plain"
                                        )
                                        isTestingUpload = false
                                        if (success) {
                                            Toast.makeText(context, "Cloud upload verified successfully!", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Upload test failed. Check internet and Drive permissions.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !isTestingUpload
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (isTestingUpload) "Testing..." else "Test Upload")
                            }

                            Button(
                                onClick = {
                                    GoogleDriveSyncManager.signOut(context) {
                                        refreshState()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF4444)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disconnect", color = Color.White)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                onConnectAccount()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect Google Drive Account")
                        }
                    }
                }
            }

            // Developer / OAuth Setup Info Card
            RoundedCardContainer {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "OAuth Configuration & SHA-1",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Google Sign-In requires registering this APK's SHA-1 fingerprint in your Google Cloud / Firebase Console under OAuth 2.0 Client IDs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Package: $packageName",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                            )
                            Text(
                                text = "SHA-1:\n$sha1Fingerprint",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SHA-1 Fingerprint", sha1Fingerprint))
                            Toast.makeText(context, "SHA-1 copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy SHA-1 Fingerprint")
                    }
                }
            }

            // Target Folder Card
            RoundedCardContainer {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Cloud Folder: SentinelShield",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Intruder photos (.jpg), videos (.mp4), and diagnostics logs are uploaded to a private folder named 'SentinelShield' in your Drive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Privacy Security Card
            RoundedCardContainer {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Privacy & Access Scope",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "SentinelShield uses the restricted 'drive.file' scope. This means SentinelShield ONLY has access to files created by the app itself and CANNOT view your personal Google Drive documents or photos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
