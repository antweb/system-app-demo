package de.anonweb.wifimanager

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import androidx.annotation.StringRes

fun unquoteSsid(raw: String?): String? {
    if (raw == null || raw == WifiManager.UNKNOWN_SSID) return null
    return raw.removeSurrounding("\"").ifEmpty { null }
}

fun unredactedBssid(raw: String?): String? {
    // A redacted BSSID (WiFiInfo.DEFAULT_MAC_ADDRESS) is returned in some cases (e.g. missing permissions)
    val redactedBssid = "02:00:00:00:00:00"
    if (raw == null || raw == redactedBssid) return null
    return raw
}

@StringRes
fun securityLabelRes(securityType: Int): Int = when (securityType) {
    WifiInfo.SECURITY_TYPE_OPEN -> R.string.security_open
    WifiInfo.SECURITY_TYPE_WEP -> R.string.security_wep
    WifiInfo.SECURITY_TYPE_PSK -> R.string.security_wpa_psk
    WifiInfo.SECURITY_TYPE_EAP -> R.string.security_wpa_eap
    WifiInfo.SECURITY_TYPE_SAE -> R.string.security_wpa3_sae
    WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE_192_BIT -> R.string.security_wpa3_enterprise_192
    WifiInfo.SECURITY_TYPE_OWE -> R.string.security_owe
    WifiInfo.SECURITY_TYPE_WAPI_PSK -> R.string.security_wapi_psk
    WifiInfo.SECURITY_TYPE_WAPI_CERT -> R.string.security_wapi_cert
    WifiInfo.SECURITY_TYPE_EAP_WPA3_ENTERPRISE -> R.string.security_wpa3_enterprise
    WifiInfo.SECURITY_TYPE_OSEN -> R.string.security_osen
    WifiInfo.SECURITY_TYPE_PASSPOINT_R1_R2 -> R.string.security_passpoint_r1_r2
    WifiInfo.SECURITY_TYPE_PASSPOINT_R3 -> R.string.security_passpoint_r3
    WifiInfo.SECURITY_TYPE_DPP -> R.string.security_dpp
    else -> R.string.unknown
}

@StringRes
fun signalQualityLabelRes(level: Int, maxLevel: Int): Int {
    if (maxLevel <= 0) return R.string.unknown
    return when (level.toFloat() / maxLevel) {
        in 0.75f..1f -> R.string.signal_excellent
        in 0.5f..<0.75f -> R.string.signal_good
        in 0.25f..<0.5f -> R.string.signal_fair
        else -> R.string.signal_weak
    }
}

@StringRes
fun yesNoRes(value: Boolean): Int = if (value) R.string.value_yes else R.string.value_no
