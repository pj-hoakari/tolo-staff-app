package dev.usbharu.tolo_staff

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.ui.graphics.vector.ImageVector
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppTab
import dev.usbharu.tolo_staff.feature.appshell.ReportFlowStep

internal object AppShellRoutes {
    const val HOME = "home"
    const val INSTRUCTIONS_LIST = "instructions/list"
    const val INSTRUCTIONS_DETAIL = "instructions/detail"
    const val REPORTS_TYPE = "reports/type"
    const val REPORTS_DRAFT = "reports/draft"
    const val REPORTS_PLACE = "reports/place"
    const val CONTACTS_LIST = "contacts/list"
    const val CONTACTS_TARGET = "contacts/target"
    const val CONTACTS_DETAIL = "contacts/detail"
}

internal data class AppShellTabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
    val rootRoute: String,
)

internal fun appShellTabs(): List<AppShellTabItem> = listOf(
    AppShellTabItem(AppTab.HOME, "ホーム", Icons.Default.Home, AppShellRoutes.HOME),
    AppShellTabItem(AppTab.INSTRUCTIONS, "指示", Icons.Default.Summarize, AppShellRoutes.INSTRUCTIONS_LIST),
    AppShellTabItem(AppTab.REPORTS, "報告", Icons.Default.Mail, AppShellRoutes.REPORTS_TYPE),
    AppShellTabItem(AppTab.CONTACTS, "連絡", Icons.Default.Message, AppShellRoutes.CONTACTS_LIST),
)

internal fun AppShellUiState.navigationRoute(): String =
    when (selectedTab) {
        AppTab.HOME -> AppShellRoutes.HOME
        AppTab.INSTRUCTIONS -> if (instructionsTab.selectedInstruction == null) {
            AppShellRoutes.INSTRUCTIONS_LIST
        } else {
            AppShellRoutes.INSTRUCTIONS_DETAIL
        }
        AppTab.REPORTS -> when (reportsTab.step) {
            ReportFlowStep.TYPE_SELECTION -> AppShellRoutes.REPORTS_TYPE
            ReportFlowStep.DRAFT_INPUT -> AppShellRoutes.REPORTS_DRAFT
            ReportFlowStep.PLACE_SELECTION -> AppShellRoutes.REPORTS_PLACE
        }
        AppTab.CONTACTS -> when {
            contactsTab.selectedThread != null -> AppShellRoutes.CONTACTS_DETAIL
            contactsTab.isChoosingTargetType -> AppShellRoutes.CONTACTS_TARGET
            else -> AppShellRoutes.CONTACTS_LIST
        }
    }

internal fun routeToTab(route: String?): AppTab =
    when {
        route == null -> AppTab.HOME
        route.startsWith("instructions/") -> AppTab.INSTRUCTIONS
        route.startsWith("reports/") -> AppTab.REPORTS
        route.startsWith("contacts/") -> AppTab.CONTACTS
        else -> AppTab.HOME
    }

internal fun rootRouteForTab(tab: AppTab): String =
    when (tab) {
        AppTab.HOME -> AppShellRoutes.HOME
        AppTab.INSTRUCTIONS -> AppShellRoutes.INSTRUCTIONS_LIST
        AppTab.REPORTS -> AppShellRoutes.REPORTS_TYPE
        AppTab.CONTACTS -> AppShellRoutes.CONTACTS_LIST
    }
