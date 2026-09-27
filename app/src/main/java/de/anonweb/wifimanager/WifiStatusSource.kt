package de.anonweb.wifimanager

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

data class WifiStatus(
    val wifiEnabled: Boolean,
    val locationEnabled: Boolean,
    val wifiInfo: WifiInfo?,
    val signalLevel: Int,
    val visibleNetworkCount: Int?,
    val maxSignalLevel: Int,
    val hotspotSupported: Boolean,
    val wifiScannerSupported: Boolean,
    val macRandomizationSupported: Boolean,
    val countryCode: String?,
)

class WifiStatusSource(private val context: Context) {

    private val TAG = "WifiStatusSource"

    fun statusFlow(): Flow<WifiStatus> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val wifiManager = context.getSystemService(WifiManager::class.java)
        val locationManager = context.getSystemService(LocationManager::class.java)
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val visibleNetworkCount = try {
            wifiManager.scanResults.size
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to call getScanResults: ${e.message}")
            null
        }

        val hotspotSupported = try {
            wifiManager.isPortableHotspotSupported
        } catch (e: Throwable) {
            Log.e("WifiStatusSource", "Failed to call isPortableHotspotSupported ${e.message}")
            false
        }

        val wifiScannerSupported = try {
            wifiManager.isWifiScannerSupported
        } catch (e: Throwable) {
            Log.e("WifiStatusSource", "Failed to call isWifiScannerSupported: ${e.message}")
            false
        }

        val macRandomizationSupported = try {
            wifiManager.isConnectedMacRandomizationSupported
        } catch (e: Throwable) {
            Log.e("WifiStatusSource", "Failed to call isConnectedMacRandomizationSupported: ${e.message}")
            false
        }

        val countryCode = try {
            wifiManager.countryCode
        } catch (e: Throwable) {
            Log.e("WifiStatusSource", "Failed to call isConnectedMacRandomizationSupported: ${e.message}")
            null
        }

        val snapshot = Snapshot()

        fun push() {
            val info = snapshot.info
            trySend(
                WifiStatus(
                    wifiEnabled = wifiManager.isWifiEnabled,
                    locationEnabled = locationManager.isLocationEnabled,
                    wifiInfo = info,
                    signalLevel = info?.let { wifiManager.calculateSignalLevel(it.rssi) } ?: 0,
                    visibleNetworkCount = visibleNetworkCount,
                    maxSignalLevel = wifiManager.maxSignalLevel,
                    hotspotSupported = hotspotSupported,
                    wifiScannerSupported = wifiScannerSupported,
                    macRandomizationSupported = macRandomizationSupported,
                    countryCode = countryCode
                )
            )
        }

        val callback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                snapshot.info = caps.transportInfo as? WifiInfo
                push()
            }

            override fun onLost(network: Network) {
                snapshot.info = null
                push()
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) = push()
        }
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(LocationManager.MODE_CHANGED_ACTION)
            },
            Context.RECEIVER_NOT_EXPORTED,
        )

        connectivityManager.registerNetworkCallback(request, callback)
        push()

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
            context.unregisterReceiver(receiver)
        }
    }.conflate()

    private class Snapshot {
        @Volatile var info: WifiInfo? = null
    }
}
