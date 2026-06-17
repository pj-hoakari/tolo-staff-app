package dev.usbharu.tolo_staff

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.di.KoinHelper
import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.AppShellUiState
import dev.usbharu.tolo_staff.feature.appshell.AppShellViewModel
import dev.usbharu.tolo_staff.feature.appshell.AppTab
import dev.usbharu.tolo_staff.feature.contactchat.ChatMessage
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatUiState
import dev.usbharu.tolo_staff.feature.contactchat.ContactChatViewModel

@Composable
fun ToloStaffAndroidApp(
    viewModel: AppShellViewModel = remember { KoinHelper().getAppShellViewModel() },
    contactChatViewModel: ContactChatViewModel = remember { KoinHelper().getContactChatViewModel() }
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
        onRoomSelected = contactChatViewModel::onRoomSelected,
        onBackToRooms = contactChatViewModel::onBackToRooms,
        onDraftChanged = contactChatViewModel::onDraftChanged,
        onSendClicked = contactChatViewModel::onSendClicked
    )
}

@Composable
fun ToloStaffAndroidContent(
    state: AppShellUiState,
    contactChatState: ContactChatUiState = ContactChatUiState(),
    onTabSelected: (AppTab) -> Unit = {},
    onRoomSelected: (String) -> Unit = {},
    onBackToRooms: () -> Unit = {},
    onDraftChanged: (String) -> Unit = {},
    onSendClicked: () -> Unit = {}
) {
    MaterialTheme {
        val isChatDetailVisible = state.selectedTab == AppTab.CONTACTS &&
            contactChatState.selectedRoomId != null
        val shouldBackToHome = !isChatDetailVisible && state.selectedTab != AppTab.HOME

        BackHandler(enabled = shouldBackToHome) {
            onTabSelected(AppTab.HOME)
        }

        Scaffold(
            bottomBar = {
                if (!isChatDetailVisible) {
                    Column {
                        if (state.selectedTab != AppTab.CONTACTS) {
                            PlacementFooter(placementName = state.currentPlacementName)
                        }

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
                }
            }
        ) { contentPadding ->
            AppShellTabContent(
                state = state,
                contactChatState = contactChatState,
                contentPadding = contentPadding,
                onRoomSelected = onRoomSelected,
                onBackToRooms = onBackToRooms,
                onDraftChanged = onDraftChanged,
                onSendClicked = onSendClicked
            )
        }
    }
}

@Composable
private fun AppShellTabContent(
    state: AppShellUiState,
    contactChatState: ContactChatUiState,
    contentPadding: PaddingValues,
    onRoomSelected: (String) -> Unit,
    onBackToRooms: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    val tab = appShellTabs().first { it.tab == state.selectedTab }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        if (state.selectedTab == AppTab.CONTACTS) {
            ContactChatContent(
                state = contactChatState,
                onRoomSelected = onRoomSelected,
                onBackToRooms = onBackToRooms,
                onDraftChanged = onDraftChanged,
                onSendClicked = onSendClicked
            )
            return@Surface
        }

        if (state.selectedTab == AppTab.HOME) {
            HomeOverviewContent(overview = state.homeOverview)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
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
            }
        }
    }
}

@Composable
private fun HomeOverviewContent(overview: AppShellHomeOverview) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        item {
            HomeOverviewCard(
                title = stringResource(R.string.home_event_card_title),
                primaryText = overview.eventName,
                secondaryText = overview.eventTime,
                identifier = "app_shell_home_event_card"
            )
        }
        item {
            HomePlacementMapOverviewCard(
                overview = overview,
                identifier = "app_shell_home_placement_map_card"
            )
        }
        item {
            HomeOverviewCard(
                title = stringResource(R.string.home_instruction_card_title),
                primaryText = overview.currentInstruction,
                secondaryText = null,
                identifier = "app_shell_home_instruction_card"
            )
        }
    }
}

@Composable
private fun HomePlacementMapOverviewCard(
    overview: AppShellHomeOverview,
    identifier: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = identifier
            },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.home_placement_card_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = overview.placementName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = overview.placementDetail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = overview.mapState.venueName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${overview.mapState.latitude}, ${overview.mapState.longitude}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeOverviewCard(
    title: String,
    primaryText: String,
    secondaryText: String?,
    identifier: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = identifier
            },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlacementFooter(placementName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
    onRoomSelected: (String) -> Unit,
    onBackToRooms: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    BackHandler(enabled = state.selectedRoomId != null) {
        onBackToRooms()
    }

    AnimatedContent(
        targetState = state.selectedRoomId != null,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { -it / 3 }) + fadeOut()
            } else {
                slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() togetherWith
                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            }
        },
        label = "ContactChatNavigation"
    ) { isDetailVisible ->
        if (isDetailVisible) {
            ContactChatDetail(
                state = state,
                onBackToRooms = onBackToRooms,
                onDraftChanged = onDraftChanged,
                onSendClicked = onSendClicked
            )
        } else {
            ContactRoomList(
                state = state,
                onRoomSelected = onRoomSelected
            )
        }
    }
}

@Composable
private fun ContactRoomList(
    state: ContactChatUiState,
    onRoomSelected: (String) -> Unit
) {
    if (state.isLoading && state.rooms.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.contacts_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        state.errorMessage?.let { errorMessage ->
            item {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }

        if (state.rooms.isEmpty()) {
            item {
                Text(
                    text = "参加中のスレッドはありません",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                )
            }
        }

        items(state.rooms, key = { it.id }) { room ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRoomSelected(room.id) }
                    .padding(horizontal = 24.dp, vertical = 14.dp)
                    .semantics {
                        contentDescription = "contact_chat_room_${room.id}"
                    },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = room.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = room.lastMessage ?: "メッセージはまだありません",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (room.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = 24.dp))
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackToRooms,
                        modifier = Modifier.semantics {
                            contentDescription = "contact_chat_back_button"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = state.selectedRoomTitle ?: stringResource(R.string.contacts_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider()
            }
        },
        bottomBar = {
            ContactMessageInput(
                draftText = state.draftText,
                onDraftChanged = onDraftChanged,
                onSendClicked = onSendClicked,
                isSending = state.isSending
            )
        }
    ) { chatPadding ->
        if (state.isLoading && state.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(chatPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(chatPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.errorMessage?.let { errorMessage ->
                    item {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (state.messages.isEmpty()) {
                    item {
                        Text(
                            text = "このスレッドにはまだメッセージがありません",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "contact_chat_message_${message.id}"
            },
        horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (message.isSystemEvent) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else if (message.isFromCurrentUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                }
            ) {
                Text(
                    text = message.body,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isSystemEvent) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else if (message.isFromCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
            message.timeLabel?.let { timeLabel ->
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactMessageInput(
    draftText: String,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    isSending: Boolean
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "contact_chat_message_input"
                    },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (draftText.isEmpty()) {
                        Text(
                            text = stringResource(R.string.contact_chat_message_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    BasicTextField(
                        value = draftText,
                        onValueChange = onDraftChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            FilledIconButton(
                onClick = onSendClicked,
                enabled = draftText.trim().isNotEmpty() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "contact_chat_send_button"
                    }
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null
                    )
                }
            }
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
