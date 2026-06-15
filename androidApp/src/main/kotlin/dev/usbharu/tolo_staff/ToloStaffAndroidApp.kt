package dev.usbharu.tolo_staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.appshell.AppTab

@Composable
fun ToloStaffAndroidApp(
    viewModel: AppShellViewModel = remember { AppShellViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }

    ToloStaffAndroidContent(
        state = state,
        onTabSelected = viewModel::onTabSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToloStaffAndroidContent(
    state: AppShellUiState,
    onTabSelected: (AppTab) -> Unit = {}
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.placement_header_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = state.currentPlacementName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.semantics {
                                    contentDescription = "app_shell_placement_name"
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    appShellTabs().forEach { item ->
                        NavigationBarItem(
                            selected = state.selectedTab == item.tab,
                            onClick = { onTabSelected(item.tab) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        ) { contentPadding ->
            AppShellTabContent(
                state = state,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun AppShellTabContent(
    state: AppShellUiState,
    contentPadding: PaddingValues
) {
    val tab = appShellTabs().first { it.tab == state.selectedTab }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(tab.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.placement_inline_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.currentPlacementName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = stringResource(tab.descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class AppShellTabItem(
    val tab: AppTab,
    val labelRes: Int,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

private fun appShellTabs(): List<AppShellTabItem> = listOf(
    AppShellTabItem(
        tab = AppTab.HOME,
        labelRes = R.string.tab_home,
        titleRes = R.string.home_title,
        descriptionRes = R.string.home_description,
        icon = Icons.Filled.Home
    ),
    AppShellTabItem(
        tab = AppTab.INSTRUCTIONS,
        labelRes = R.string.tab_instructions,
        titleRes = R.string.instructions_title,
        descriptionRes = R.string.instructions_description,
        icon = Icons.AutoMirrored.Filled.Assignment
    ),
    AppShellTabItem(
        tab = AppTab.REPORTS,
        labelRes = R.string.tab_reports,
        titleRes = R.string.reports_title,
        descriptionRes = R.string.reports_description,
        icon = Icons.Filled.Summarize
    ),
    AppShellTabItem(
        tab = AppTab.CONTACTS,
        labelRes = R.string.tab_contacts,
        titleRes = R.string.contacts_title,
        descriptionRes = R.string.contacts_description,
        icon = Icons.Filled.Mail
    )
)

@Preview
@Composable
private fun ToloStaffAndroidContentPreview() {
    ToloStaffAndroidContent(state = AppShellUiState())
}
