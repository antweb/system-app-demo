package de.anonweb.wifimanager

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val source = WifiStatusSource(app)
    private val restart = MutableStateFlow(0)
    private var permanentlyDenied = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MainUiState> =
        restart
            .flatMapLatest { source.statusFlow() }
            .map { it.toUiState(fineLocationGranted(), permanentlyDenied) }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MainUiState())

    fun refresh() {
        restart.value++
    }

    fun onPermissionResult(granted: Boolean, canAskAgain: Boolean) {
        permanentlyDenied = !granted && !canAskAgain
        refresh()
    }

    private fun fineLocationGranted(): Boolean =
        getApplication<Application>().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}

private fun WifiStatus.toUiState(
    permissionGranted: Boolean,
    permissionPermanentlyDenied: Boolean,
): MainUiState {
    val info = wifiInfo
    val connection = if (info != null) {
        ConnectionDetails(
            ssid = unquoteSsid(info.ssid),
            bssid = unredactedBssid(info.bssid),
            hiddenSsid = info.hiddenSSID,
            securityRes = securityLabelRes(info.currentSecurityType),
            rssiDbm = info.rssi,
            visibleNetworkCount = visibleNetworkCount,
            signalQualityRes = signalQualityLabelRes(signalLevel, maxSignalLevel),
        )
    } else {
        null
    }

    return MainUiState(
        loading = false,
        wifiEnabled = wifiEnabled,
        connected = info != null,
        locationEnabled = locationEnabled,
        permissionGranted = permissionGranted,
        permissionPermanentlyDenied = permissionPermanentlyDenied,
        connection = connection,
        adapter = AdapterInfo(
            hotspotSupportedRes = yesNoRes(hotspotSupported),
            wifiScannerSupportedRes = yesNoRes(wifiScannerSupported),
            macRandomizationSupportedRes = yesNoRes(macRandomizationSupported),
            countryCode = countryCode,
        ),
        configuredNetworks = configuredNetworks.map { config ->
            ConfiguredNetwork(
                ssid = unquoteSsid(config.SSID),
                securityRes = configuredNetworkSecurityLabelRes(config),
                passphrase = config.preSharedKey?.removeSurrounding("\"")?.ifEmpty { null },
            )
        },
    )
}
