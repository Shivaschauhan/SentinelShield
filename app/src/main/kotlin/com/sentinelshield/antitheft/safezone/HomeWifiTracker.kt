package com.sentinelshield.antitheft.safezone

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.sentinelshield.antitheft.utils.DebugLogger

/**
 * Watches the Wi-Fi connection so leaving the router's range is noticed in seconds instead of
 * waiting for the geofence. Hosted by SecurityMonitorService; callbacks run on the main thread.
 *
 * Only the access point's BSSID is trusted (see [WifiMatcher]). When Android hides it, the state is
 * recorded as unknown rather than "disconnected", so a missing permission can never cause a false AWAY.
 */
class HomeWifiTracker(
    context: Context,
    private val onChanged: () -> Unit,
) {
    private val app = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val connectivity = app.getSystemService(ConnectivityManager::class.java)
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var currentNetwork: Network? = null

    val isRunning: Boolean get() = callback != null

    fun start() {
        if (callback != null || connectivity == null) return
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val cb = createCallback()
        try {
            connectivity.registerNetworkCallback(request, cb, handler)
            callback = cb
        } catch (e: Exception) {
            DebugLogger.log(app, TAG, "Could not watch Wi-Fi: ${e.message}", force = true)
            return
        }
        // The callback only fires on changes; record the starting state.
        if (connectivity.allNetworks.none { isWifi(it) }) {
            recordDisconnected()
            onChanged()
        }
    }

    fun stop() {
        callback?.let { runCatching { connectivity?.unregisterNetworkCallback(it) } }
        callback = null
        currentNetwork = null
    }

    private fun isWifi(network: Network): Boolean =
        connectivity?.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    private fun createCallback(): ConnectivityManager.NetworkCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    val info = caps.transportInfo as? WifiInfo
                    handleConnected(network, info?.ssid, info?.bssid)
                }

                override fun onLost(network: Network) = handleLost(network)
            }
        } else {
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    @Suppress("DEPRECATION")
                    val info = app.getSystemService(WifiManager::class.java)?.connectionInfo
                    handleConnected(network, info?.ssid, info?.bssid)
                }

                override fun onLost(network: Network) = handleLost(network)
            }
        }

    private fun handleConnected(network: Network, ssid: String?, bssid: String?) {
        currentNetwork = network
        val zones = SafeZoneStore.zones(app).map { WifiZoneRef(it.id, it.ssid, it.bssids) }
        when (val verdict = WifiMatcher.evaluate(zones, ssid, bssid, SafeZoneStore.geoInside(app))) {
            is WifiVerdict.Home -> {
                verdict.learnBssid?.let {
                    if (SafeZoneStore.learnBssid(app, verdict.zoneId, it)) {
                        DebugLogger.log(app, TAG, "Learned another home access point ($it).", force = true)
                    }
                }
                SafeZoneStore.setWifiConnected(app, true)
                SafeZoneStore.setWifiLostAt(app, null)
            }
            WifiVerdict.NotHome -> recordDisconnected()
            WifiVerdict.Unreadable -> SafeZoneStore.setWifiConnected(app, null)
        }
        onChanged()
    }

    private fun handleLost(network: Network) {
        if (network != currentNetwork) return
        currentNetwork = null
        // Another Wi-Fi network may still be up; its own callback will correct this if so.
        recordDisconnected()
        onChanged()
    }

    private fun recordDisconnected() {
        SafeZoneStore.setWifiConnected(app, false)
        if (SafeZoneStore.wifiLostAt(app) == null) SafeZoneStore.setWifiLostAt(app, System.currentTimeMillis())
    }

    private companion object {
        const val TAG = "HomeWifiTracker"
    }
}
