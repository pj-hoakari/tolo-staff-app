package dev.usbharu.tolo_staff

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.appshell.AppTab
import dev.usbharu.tolo_staff.feature.contactchat.ChatMessage
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatUiState
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel

@Composable
fun ToloStaffAndroidApp(
    viewModel: AppShellViewModel = remember { AppShellViewModel() },
    contactChatViewModel: ContactChatViewModel = remember { ContactChatViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    val contactChatState by contactChatViewModel.uiState.collectAsState()

    DisposableEffect(viewModel, contactChatViewModel) {
        onDispose {
            viewModel.clear()
            contactChatViewModel.clear()
        }
    }

    ToloStaffAndroidContent(
        state = state,
        contactChatState = contactChatState,
        onTabSelected = viewModel::onTabSelected,
        onContactRoomSelected = contactChatViewModel::onRoomSelected,
        onContactBackToRooms = contactChatViewModel::onBackToRooms,
        onContactDraftChanged = contactChatViewModel::onDraftChanged,
        onContactSendClicked = contactChatViewModel::onSendClicked
    )
}

@Composable
fun ToloStaffAndroidContent(
    state: AppShellUiState,
    contactChatState: ContactChatUiState = ContactChatUiState(),
    onTabSelected: (AppTab) -> Unit = {},
    onContactRoomSelected: (String) -> Unit = {},
    onContactBackToRooms: () -> Unit = {},
    onContactDraftChanged: (String) -> Unit = {},
    onContactSendClicked: () -> Unit = {}
) {
    MaterialTheme {
        Scaffold(
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
            if (state.selectedTab == AppTab.CONTACTS) {
                ContactChatContent(
                    state = contactChatState,
                    contentPadding = contentPadding,
                    onRoomSelected = onContactRoomSelected,
                    onBackToRooms = onContactBackToRooms,
                    onDraftChanged = onContactDraftChanged,
                    onSendClicked = onContactSendClicked
                )
            } else {
                AppShellTabContent(
                    state = state,
                    contentPadding = contentPadding
                )
            }
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
            Text(
                text = stringResource(tab.descriptionRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            PlacementFooter(placementName = state.currentPlacementName)
        }
    }
}

@Composable
private fun PlacementFooter(placementName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.placement_header_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = placementName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics {
                    contentDescription = "app_shell_placement_name"
                }
            )
        }
    }
}

@Composable
private fun ContactChatContent(
    state: ContactChatUiState,
    contentPadding: PaddingValues,
    onRoomSelected: (String) -> Unit,
    onBackToRooms: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state.selectedRoomId == null) {
            ContactRoomList(
                state = state,
                onRoomSelected = onRoomSelected
            )
        } else {
            ContactChatDetail(
                state = state,
                onBackToRooms = onBackToRooms,
                onDraftChanged = onDraftChanged,
                onSendClicked = onSendClicked
            )
        }
    }
}

@Composable
private fun ContactRoomList(
    state: ContactChatUiState,
    onRoomSelected: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.contact_chat_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        items(state.rooms, key = { it.id }) { room ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRoomSelected(room.id) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = room.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = room.lastMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (room.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = room.unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactChatDetail(
    state: ContactChatUiState,
    onBackToRooms: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToRooms) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.contact_chat_back)
                )
            }
            Text(
                text = state.selectedRoomTitle ?: stringResource(R.string.contact_chat_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = state.draftText,
                onValueChange = onDraftChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(R.string.contact_chat_message_placeholder)) },
                singleLine = true
            )
            Button(
                onClick = onSendClicked,
                enabled = state.draftText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = stringResource(R.string.contact_chat_send)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (message.isFromCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                }
            ) {
                Text(
                    text = message.body,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            Text(
                text = message.timeLabel,
                style = MaterialTheme.typography.labelSmall,
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
