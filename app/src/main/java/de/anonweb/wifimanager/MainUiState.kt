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
    val adapter: AdapterInfo? = null,
)

data class ConnectionDetails(
    val ssid: String?,
    val bssid: String?,
    val hiddenSsid: Boolean,
    @StringRes val securityRes: Int,
    val rssiDbm: Int,
    val visibleNetworkCount: Int?,
    @StringRes val signalQualityRes: Int,
)

data class AdapterInfo(
    @StringRes val hotspotSupportedRes: Int,
    @StringRes val wifiScannerSupportedRes: Int,
    @StringRes val macRandomizationSupportedRes: Int,
    val countryCode: String?,
)
