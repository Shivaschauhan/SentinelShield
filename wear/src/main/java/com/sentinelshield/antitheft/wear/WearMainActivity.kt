package com.sentinelshield.antitheft.wear

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                WearApp(onTriggerAlarm = { triggerAlarmOnPhone() })
            }
        }
    }

    private fun triggerAlarmOnPhone() {
        val messageClient = Wearable.getMessageClient(this)
        val nodeClient = Wearable.getNodeClient(this)
        
        nodeClient.connectedNodes.addOnSuccessListener { nodes ->
            for (node in nodes) {
                messageClient.sendMessage(node.id, "/sentinel/alarm_trigger", ByteArray(0))
                    .addOnSuccessListener {
                        Log.d("WearMainActivity", "Message sent to phone successfully.")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WearMainActivity", "Failed to send message", e)
                    }
            }
        }
    }
}

@Composable
fun WearApp(onTriggerAlarm: () -> Unit) {
    var showConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showConfirmation) {
            Text(
                text = "Alarm Triggered on Phone!",
                color = Color.Green,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                showConfirmation = false
            }
        } else {
            Text(
                text = "Sentinel Shield",
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    onTriggerAlarm()
                    showConfirmation = true
                },
                colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color.Red),
                modifier = Modifier.size(72.dp)
            ) {
                Text(
                    text = "SOS",
                    color = Color.White,
                    style = MaterialTheme.typography.title3
                )
            }
        }
    }
}
