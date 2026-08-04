package com.sentinelshield.antitheft

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class PhoneDataLayerListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)
        Log.d("PhoneDataLayer", "Message received from watch: ${messageEvent.path}")
        
        when (messageEvent.path) {
            "/sentinel/alarm_trigger" -> {
                Log.d("PhoneDataLayer", "Triggering alarm from watch!")
                SecurityAlertService.start(this, "Remote Alarm Triggered from Watch")
            }
        }
    }
}
