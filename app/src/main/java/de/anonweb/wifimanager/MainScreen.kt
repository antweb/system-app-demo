package de.anonweb.wifimanager

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val canAskAgain = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == true
        viewModel.onPermissionResult(granted, canAskAgain)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.title_wifi)) }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Banner(
                    state = state,
                    onGrantPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            )
                        )
                    },
                    onOpenAppSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            )
                        )
                    },
                    onOpenLocationSettings = {
                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                )
            }

            val connection = state.connection
            if (connection != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        val yes = stringResource(R.string.value_yes)
                        Column {
                            InfoRow(stringResource(R.string.label_quality), stringResource(connection.signalQualityRes))
                            InfoRow(
                                stringResource(R.string.label_signal_strength),
                                stringResource(R.string.value_dbm, connection.rssiDbm),
                            )

                            InfoRow(stringResource(R.string.label_ssid), connection.ssid)
                            InfoRow(stringResource(R.string.label_bssid), connection.bssid)
                            InfoRow(stringResource(R.string.label_security), stringResource(connection.securityRes))
                            InfoRow(stringResource(R.string.label_hidden_network), if (connection.hiddenSsid) yes else null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Banner(
    state: MainUiState,
    onGrantPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val openSettings = stringResource(R.string.action_open_settings)
    val grant = stringResource(R.string.action_grant)
    val (message, actionLabel, onAction) = when {
        !state.permissionGranted && state.permissionPermanentlyDenied ->
            Triple(stringResource(R.string.banner_permission_permanently_denied), openSettings, onOpenAppSettings)
        !state.permissionGranted ->
            Triple(stringResource(R.string.banner_permission_needed), grant, onGrantPermission)
        !state.locationEnabled ->
            Triple(stringResource(R.string.banner_location_disabled), openSettings, onOpenLocationSettings)
        !state.wifiEnabled ->
            Triple(stringResource(R.string.banner_wifi_off), null, null)
        !state.connected ->
            Triple(stringResource(R.string.banner_not_connected), null, null)
        else -> Triple(null, null, null)
    }

    if (message == null) return

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value == null) return
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(value)
    }
}
