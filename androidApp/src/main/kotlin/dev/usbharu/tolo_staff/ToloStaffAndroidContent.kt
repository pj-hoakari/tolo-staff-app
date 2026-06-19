package dev.usbharu.tolo_staff

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppTab
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetType
import dev.usbharu.tolo_staff.feature.appshell.InstructionProgressStatus

@Composable
fun ToloStaffAndroidContent(
    state: AppShellUiState,
    onTabSelected: (AppTab) -> Unit = {},
    onHomeInstructionSelected: () -> Unit = {},
    onInstructionSelected: (String) -> Unit = {},
    onInstructionThreadOpened: () -> Unit = {},
    onInstructionDetailClosed: () -> Unit = {},
    onInstructionStatusUpdated: (InstructionProgressStatus) -> Unit = {},
    onReportTypeSelected: (String) -> Unit = {},
    onReportSelected: (String) -> Unit = {},
    onReportCommentChanged: (String) -> Unit = {},
    onReportUrgencySelected: (String) -> Unit = {},
    onReportImageToggleChanged: (Boolean) -> Unit = {},
    onReportLocationToggleChanged: (Boolean) -> Unit = {},
    onReportContinueToPlaceSelection: () -> Unit = {},
    onReportPlaceSelected: (String) -> Unit = {},
    onReportSubmitted: () -> Unit = {},
    onReportBack: () -> Unit = {},
    onContactThreadSelected: (String) -> Unit = {},
    onContactBackToList: () -> Unit = {},
    onContactNewThreadStarted: () -> Unit = {},
    onContactTargetTypeSelected: (ContactTargetType) -> Unit = {},
    onContactTargetSelected: (String) -> Unit = {},
    onContactDraftChanged: (String) -> Unit = {},
    onContactSendClicked: () -> Unit = {},
    onCurrentStaffSelected: (String) -> Unit = {},
) {
    MaterialTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val currentTab = routeToTab(currentRoute)
        val targetRoute = state.navigationRoute()
        var previousRoute by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(targetRoute, currentRoute) {
            if (currentRoute != null && currentRoute == targetRoute) {
                return@LaunchedEffect
            }
            val targetTab = routeToTab(targetRoute)
            if (currentTab == targetTab) {
                navController.navigate(targetRoute) {
                    launchSingleTop = true
                }
            } else {
                val rootRoute = rootRouteForTab(targetTab)
                navController.navigate(rootRoute) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                }
                if (rootRoute != targetRoute) {
                    navController.navigate(targetRoute) {
                        launchSingleTop = true
                    }
                }
            }
        }

        LaunchedEffect(currentRoute) {
            val lastRoute = previousRoute
            if (lastRoute != null && currentRoute != null) {
                when {
                    lastRoute == AppShellRoutes.INSTRUCTIONS_DETAIL && currentRoute == AppShellRoutes.INSTRUCTIONS_LIST -> {
                        onInstructionDetailClosed()
                    }
                    lastRoute == AppShellRoutes.REPORTS_DRAFT && currentRoute == AppShellRoutes.REPORTS_TYPE -> {
                        onReportBack()
                    }
                    lastRoute == AppShellRoutes.REPORTS_PLACE && currentRoute == AppShellRoutes.REPORTS_DRAFT -> {
                        onReportBack()
                    }
                    lastRoute == AppShellRoutes.CONTACTS_DETAIL && currentRoute == AppShellRoutes.CONTACTS_LIST -> {
                        onContactBackToList()
                    }
                    lastRoute == AppShellRoutes.CONTACTS_TARGET && currentRoute == AppShellRoutes.CONTACTS_LIST -> {
                        onContactBackToList()
                    }
                }
            }
            previousRoute = currentRoute
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            appBarTitle(
                                route = currentRoute,
                                state = state,
                            )
                        )
                    },
                    navigationIcon = {
                        if (isRootRoute(currentRoute)) {
                            CurrentStaffHeaderIcon(
                                currentStaff = state.currentStaff,
                                availableStaff = state.availableStaff,
                                onCurrentStaffSelected = onCurrentStaffSelected,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        } else {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                if (currentRoute == AppShellRoutes.CONTACTS_LIST) {
                    FloatingActionButton(onClick = onContactNewThreadStarted) {
                        Icon(
                            Icons.Default.AddComment,
                            contentDescription = "contact_new_thread_button"
                        )
                    }
                }
            },
            bottomBar = {
                Column {
                    if (currentTab != AppTab.CONTACTS) {
                        PlacementFooter(placementName = state.currentPlacementName)
                    }
                    NavigationBar {
                        appShellTabs().forEach { item ->
                            NavigationBarItem(
                                selected = currentTab == item.tab,
                                onClick = { onTabSelected(item.tab) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                color = MaterialTheme.colorScheme.background
            ) {
                NavHost(
                    navController = navController,
                    startDestination = AppShellRoutes.HOME,
                ) {
                    composable(AppShellRoutes.HOME) {
                        HomeOverviewContent(
                            overview = state.homeOverview,
                            errorMessage = state.errorMessage,
                            isLoading = state.isLoading,
                            onOpenInstruction = onHomeInstructionSelected,
                            onOpenReport = { onTabSelected(AppTab.REPORTS) },
                            onOpenContacts = { onTabSelected(AppTab.CONTACTS) },
                        )
                    }
                    composable(AppShellRoutes.INSTRUCTIONS_LIST) {
                        InstructionsListScreen(
                            state = state.instructionsTab,
                            onInstructionSelected = onInstructionSelected,
                        )
                    }
                    composable(AppShellRoutes.INSTRUCTIONS_DETAIL) {
                        state.instructionsTab.selectedInstruction?.let { instruction ->
                            InstructionDetailScreen(
                                instruction = instruction,
                                onThreadOpened = onInstructionThreadOpened,
                                onStatusUpdated = onInstructionStatusUpdated,
                            )
                        }
                    }
                    composable(AppShellRoutes.REPORTS_TYPE) {
                        ReportTypeSelectionScreen(
                            state = state.reportsTab,
                            onTypeSelected = onReportTypeSelected,
                            onReportSelected = onReportSelected,
                        )
                    }
                    composable(AppShellRoutes.REPORTS_DRAFT) {
                        ReportDraftInputScreen(
                            state = state.reportsTab,
                            onCommentChanged = onReportCommentChanged,
                            onUrgencySelected = onReportUrgencySelected,
                            onImageToggleChanged = onReportImageToggleChanged,
                            onLocationToggleChanged = onReportLocationToggleChanged,
                            onContinueToPlaceSelection = onReportContinueToPlaceSelection,
                        )
                    }
                    composable(AppShellRoutes.REPORTS_PLACE) {
                        ReportPlaceSelectionScreen(
                            state = state.reportsTab,
                            onPlaceSelected = onReportPlaceSelected,
                            onSubmitted = onReportSubmitted,
                        )
                    }
                    composable(AppShellRoutes.CONTACTS_LIST) {
                        ContactThreadListScreen(
                            state = state.contactsTab,
                            onThreadSelected = onContactThreadSelected,
                        )
                    }
                    composable(AppShellRoutes.CONTACTS_TARGET) {
                        ContactTargetSelectionScreen(
                            state = state.contactsTab,
                            onTargetTypeSelected = onContactTargetTypeSelected,
                            onTargetSelected = onContactTargetSelected,
                        )
                    }
                    composable(AppShellRoutes.CONTACTS_DETAIL) {
                        state.contactsTab.selectedThread?.let { thread ->
                            ContactThreadDetailScreen(
                                thread = thread,
                                onDraftChanged = onContactDraftChanged,
                                onSendClicked = onContactSendClicked,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isRootRoute(route: String?): Boolean =
    route == AppShellRoutes.HOME ||
        route == AppShellRoutes.INSTRUCTIONS_LIST ||
        route == AppShellRoutes.REPORTS_TYPE ||
        route == AppShellRoutes.CONTACTS_LIST

private fun appBarTitle(
    route: String?,
    state: AppShellUiState,
): String =
    when (route) {
        AppShellRoutes.HOME -> "ホーム"
        AppShellRoutes.INSTRUCTIONS_LIST -> "指示"
        AppShellRoutes.INSTRUCTIONS_DETAIL -> "指示詳細"
        AppShellRoutes.REPORTS_TYPE -> "報告"
        AppShellRoutes.REPORTS_DRAFT -> reportTitleForStep(state.reportsTab.step)
        AppShellRoutes.REPORTS_PLACE -> "対象場所"
        AppShellRoutes.CONTACTS_LIST -> "連絡"
        AppShellRoutes.CONTACTS_TARGET -> "宛先を選択"
        AppShellRoutes.CONTACTS_DETAIL -> state.contactsTab.selectedThread?.title ?: "連絡"
        else -> "ToloStaff"
    }

@Preview
@Composable
private fun PreviewToloStaffAndroidContent() {
    ToloStaffAndroidContent(state = AppShellUiState())
}
