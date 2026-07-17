package dev.usbharu.tolo_staff

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import dev.usbharu.tolo_staff.di.KoinHelper
import dev.usbharu.tolo_staff.error.AppErrorPresentation
import dev.usbharu.tolo_staff.error.AppErrorSource
import dev.usbharu.tolo_staff.feature.appshell.AppShellEffect
import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel

@Composable
fun ToloStaffAndroidApp(
    viewModel: AppShellViewModel = remember { KoinHelper().getAppShellViewModel() },
    onLogout: (() -> Unit)? = null,
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val grpcConnectionError = stringResource(R.string.error_grpc_connection)
    val firebaseConnectionError = stringResource(R.string.error_firebase_connection)
    var fatalError by remember { mutableStateOf<AppShellEffect?>(null) }

    fun dismissFatalError() {
        val dismissed = fatalError ?: return
        viewModel.acknowledgeError(dismissed.eventId)
        fatalError = null
    }

    LaunchedEffect(viewModel) {
        viewModel.effectFlow.collect { effect ->
            if (effect.presentation == AppErrorPresentation.TRANSIENT) {
                val message = when (effect.source) {
                    AppErrorSource.FIREBASE -> firebaseConnectionError
                    else -> grpcConnectionError
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                viewModel.acknowledgeError(effect.eventId)
            } else {
                fatalError = effect
            }
        }
    }

    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }

    ToloStaffAndroidContent(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onHomeInstructionSelected = viewModel::onHomeInstructionSelected,
        onInstructionSelected = viewModel::onInstructionSelected,
        onInstructionDetailClosed = viewModel::onInstructionDetailClosed,
        onReportTypeSelected = viewModel::onReportTypeSelected,
        onReportSelected = viewModel::onReportSelected,
        onReportDetailClosed = viewModel::onReportDetailClosed,
        onReportCommentChanged = viewModel::onReportCommentChanged,
        onReportUrgencySelected = viewModel::onReportUrgencySelected,
        onReportContinueToPlaceSelection = viewModel::onReportContinueToPlaceSelection,
        onReportPlaceSelected = viewModel::onReportPlaceSelected,
        onReportSubmitted = viewModel::onReportSubmitted,
        onReportBack = viewModel::onReportBack,
        onContactThreadSelected = viewModel::onContactThreadSelected,
        onContactBackToList = viewModel::onContactBackToList,
        onContactNewThreadStarted = viewModel::onContactNewThreadStarted,
        onContactTargetTypeSelected = viewModel::onContactTargetTypeSelected,
        onContactTargetSelected = viewModel::onContactTargetSelected,
        onContactDraftChanged = viewModel::onContactDraftChanged,
        onContactSendClicked = viewModel::onContactSendClicked,
        onPlacementArrivalConfirmed = viewModel::onPlacementArrivalConfirmed,
        onLogout = onLogout,
    )

    if (fatalError != null) {
        AlertDialog(
            modifier = Modifier.testTag("fatal_error_dialog"),
            onDismissRequest = ::dismissFatalError,
            title = { Text(stringResource(R.string.error_fatal_title)) },
            text = { Text(stringResource(R.string.error_fatal_message)) },
            confirmButton = {
                TextButton(
                    modifier = Modifier.testTag("fatal_error_dismiss_button"),
                    onClick = ::dismissFatalError,
                ) {
                    Text(stringResource(R.string.error_confirm))
                }
            },
        )
    }
}
