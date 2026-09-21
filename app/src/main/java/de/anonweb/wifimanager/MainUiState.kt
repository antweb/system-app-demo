package de.anonweb.wifimanager

import androidx.annotation.StringRes

data class MainUiState(
    val loading: Boolean = true,
    val wifiEnabled: Boolean = false,
    val connected: Boolean = false,
    val locationEnabled: Boolean = true,
    val permissionGranted: Boolean = false,
    val permissionPermanentlyDenied: Boolean = false,
    val connection: ConnectionDetails? = null,
)

data class ConnectionDetails(
    val ssid: String?,
    val bssid: String?,
    val hiddenSsid: Boolean,
    @StringRes val securityRes: Int,
    val rssiDbm: Int,
    @StringRes val signalQualityRes: Int,
)
