package com.sentinelshield.antitheft.wear

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class DataLayerListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("WearDataLayer", "Message received: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/sentinel/alarm_status" -> {
                // If phone sends alarm status to watch
                val status = String(messageEvent.data)
                Log.d("WearDataLayer", "Phone alarm status: $status")
            }
        }
    }
}
