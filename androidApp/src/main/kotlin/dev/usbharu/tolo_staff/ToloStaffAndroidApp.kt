package dev.usbharu.tolo_staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.usbharu.tolo_staff.di.KoinHelper
import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel

@Composable
fun ToloStaffAndroidApp(
    viewModel: AppShellViewModel = remember { KoinHelper().getAppShellViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }

    ToloStaffAndroidContent(
        state = state,
        onTabSelected = viewModel::onTabSelected,
        onHomeInstructionSelected = viewModel::onHomeInstructionSelected,
        onInstructionSelected = viewModel::onInstructionSelected,
        onInstructionThreadOpened = viewModel::onInstructionThreadOpened,
        onInstructionDetailClosed = viewModel::onInstructionDetailClosed,
        onReportTypeSelected = viewModel::onReportTypeSelected,
        onReportSelected = viewModel::onReportSelected,
        onReportDetailClosed = viewModel::onReportDetailClosed,
        onReportThreadOpened = viewModel::onReportThreadOpened,
        onReportCommentChanged = viewModel::onReportCommentChanged,
        onReportUrgencySelected = viewModel::onReportUrgencySelected,
        onReportImageToggleChanged = viewModel::onReportImageToggleChanged,
        onReportLocationToggleChanged = viewModel::onReportLocationToggleChanged,
        onReportContinueToPlaceSelection = viewModel::onReportContinueToPlaceSelection,
        onReportPlaceSelected = viewModel::onReportPlaceSelected,
        onReportSubmitted = viewModel::onReportSubmitted,
        onReportBack = viewModel::onReportBack,
        onContactThreadSelected = viewModel::onContactThreadSelected,
        onContactReportMessageSelected = viewModel::onContactReportMessageSelected,
        onContactBackToList = viewModel::onContactBackToList,
        onContactNewThreadStarted = viewModel::onContactNewThreadStarted,
        onContactTargetTypeSelected = viewModel::onContactTargetTypeSelected,
        onContactTargetSelected = viewModel::onContactTargetSelected,
        onContactDraftChanged = viewModel::onContactDraftChanged,
        onContactSendClicked = viewModel::onContactSendClicked,
        onCurrentStaffSelected = viewModel::onCurrentStaffSelected,
    )
}
