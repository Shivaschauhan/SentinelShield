package com.sentinelshield.antitheft.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentinelshield.antitheft.R
import com.sentinelshield.antitheft.SecurityPreferences
import com.sentinelshield.antitheft.ui.components.RoundedCardContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RingtoneScreen(type: String, onBack: () -> Unit) {
    val context = LocalContext.current
    
    val initialUri = remember(type) {
        when (type) {
            "pocket" -> SecurityPreferences.getPocketAlarmRingtone(context)
            "charging" -> SecurityPreferences.getChargingAlarmRingtone(context)
            else -> SecurityPreferences.getSimAlarmRingtone(context)
        }
    }
    var currentUri by remember { mutableStateOf(initialUri) }
    
    fun saveRingtone(uri: String) {
        when (type) {
            "pocket" -> SecurityPreferences.setPocketAlarmRingtone(context, uri)
            "charging" -> SecurityPreferences.setChargingAlarmRingtone(context, uri)
            else -> SecurityPreferences.setSimAlarmRingtone(context, uri)
        }
        currentUri = uri
    }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val scope = rememberCoroutineScope()

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                saveRingtone(uri.toString())
            }
        }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            saveRingtone(uri.toString())
        }
    }

    fun playPreview(uriString: String) {
        mediaPlayer?.release()
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, Uri.parse(uriString))
                isLooping = false
                setOnCompletionListener {
                    isPlaying = false
                }
                prepareAsync()
                setOnPreparedListener {
                    it.start()
                    isPlaying = true
                }
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
            isPlaying = false
        }
    }

    fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        isPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val bundledAlarms = listOf(
        Pair("Klaxon (Extremely Loud)", "android.resource://${context.packageName}/${R.raw.alarm_klaxon}"),
        Pair("Siren (Sweep)", "android.resource://${context.packageName}/${R.raw.alarm_siren}"),
        Pair("Rapid Beep", "android.resource://${context.packageName}/${R.raw.alarm_beep}")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarm Sound") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Sentinel Built-in Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }
            item {
                RoundedCardContainer {
                    Column {
                        bundledAlarms.forEach { (name, uriStr) ->
                            val isSelected = currentUri == uriStr
                            RingtoneItem(
                                title = name,
                                icon = Icons.Default.Warning,
                                isSelected = isSelected,
                                onClick = {
                                    saveRingtone(uriStr)
                                    if (isPlaying) stopPreview() else playPreview(uriStr)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "System Alarms",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
                )
            }
            item {
                RoundedCardContainer {
                    RingtoneItem(
                        title = "Select Device Ringtone",
                        icon = Icons.Default.Notifications,
                        isSelected = currentUri.startsWith("content://media/") || currentUri.startsWith("content://settings/"),
                        onClick = {
                            stopPreview()
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )
                }
            }

            item {
                Text(
                    text = "Custom Audio File",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
                )
            }
            item {
                RoundedCardContainer {
                    RingtoneItem(
                        title = "Select File from Device",
                        icon = Icons.Default.Add,
                        isSelected = currentUri.startsWith("content://com.android.providers"),
                        onClick = {
                            stopPreview()
                            documentPickerLauncher.launch(arrayOf("audio/*"))
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.padding(16.dp))
                // Preview Controls
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (isPlaying) stopPreview() else playPreview(currentUri)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Clear else Icons.Default.PlayArrow,
                            contentDescription = "Preview",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text(
                            text = if (isPlaying) "Stop Preview" else "Preview Current Alarm",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RingtoneItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
