package dev.usbharu.tolo_staff

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.di.KoinHelper
import dev.usbharu.tolo_staff.feature.auth.AuthPhase
import dev.usbharu.tolo_staff.feature.auth.AuthViewModel

@Composable
internal fun AndroidAuthRoot(
    coordinator: AndroidOidcCoordinator,
    viewModel: AuthViewModel = remember { KoinHelper().getAuthViewModel() },
) {
    val state by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        coordinator.handleAuthorizationResult(it.data)
    }
    val logoutLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    LaunchedEffect(Unit) { coordinator.restore() }
    LaunchedEffect(coordinator) {
        coordinator.refreshRequests.collect {
            coordinator.refreshForFirebase()
        }
    }
    DisposableEffect(viewModel, coordinator) {
        onDispose {
            viewModel.clear()
            coordinator.close()
        }
    }

    MaterialTheme {
        if (state.phase == AuthPhase.AUTHENTICATED) {
            ToloStaffAndroidApp(onLogout = { coordinator.logout(logoutLauncher::launch) })
        } else {
            AndroidAuthContent(state.phase) { coordinator.beginAuthorization(launcher::launch) }
        }
    }
}

@Composable
internal fun AndroidAuthContent(phase: AuthPhase, onLogin: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.auth_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.auth_description), modifier = Modifier.padding(vertical = 16.dp))
        when (phase) {
            AuthPhase.RESTORING, AuthPhase.AUTHORIZING, AuthPhase.EXCHANGING ->
                CircularProgressIndicator(Modifier.testTag("auth_progress"))
            AuthPhase.ERROR -> {
                Text(stringResource(R.string.auth_error), color = MaterialTheme.colorScheme.error)
                Button(onClick = onLogin, modifier = Modifier.testTag("auth_retry_button")) {
                    Text(stringResource(R.string.auth_retry))
                }
            }
            else -> Button(onClick = onLogin, modifier = Modifier.testTag("auth_login_button")) {
                Text(stringResource(R.string.auth_login))
            }
        }
    }
}
