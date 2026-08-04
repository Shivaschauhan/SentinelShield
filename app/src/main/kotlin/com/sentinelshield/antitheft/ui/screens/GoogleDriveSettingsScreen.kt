package com.sentinelshield.antitheft.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer
import com.sentinelshield.antitheft.utils.GoogleDriveSyncManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleDriveSettingsScreen(
    onConnectAccount: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(GoogleDriveSyncManager.isUserSignedIn(context)) }
    var accountEmail by remember { mutableStateOf(GoogleDriveSyncManager.getSignedInAccountEmail(context)) }

    val refreshState = {
        isConnected = GoogleDriveSyncManager.isUserSignedIn(context)
        accountEmail = GoogleDriveSyncManager.getSignedInAccountEmail(context)
    }

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
                .padding(16.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Button(
                            onClick = {
                                GoogleDriveSyncManager.signOut(context) {
                                    refreshState()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Out / Disconnect Account", color = Color.White)
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
