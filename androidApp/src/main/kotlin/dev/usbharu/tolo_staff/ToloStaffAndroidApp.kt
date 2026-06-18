package dev.usbharu.tolo_staff

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetType
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetUiModel
import dev.usbharu.tolo_staff.feature.appshell.CurrentStaffUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.InstructionDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionProgressStatus
import dev.usbharu.tolo_staff.feature.appshell.InstructionSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.ReportFlowStep
import dev.usbharu.tolo_staff.feature.appshell.ReportTypeUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportsTabUiState
import dev.usbharu.tolo_staff.feature.appshell.ThreadMessageUiModel

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
        onInstructionThreadClosed = viewModel::onInstructionThreadClosed,
        onInstructionStatusUpdated = viewModel::onInstructionStatusUpdated,
        onReportTypeSelected = viewModel::onReportTypeSelected,
        onReportCommentChanged = viewModel::onReportCommentChanged,
        onReportUrgencySelected = viewModel::onReportUrgencySelected,
        onReportImageToggleChanged = viewModel::onReportImageToggleChanged,
        onReportLocationToggleChanged = viewModel::onReportLocationToggleChanged,
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
        onContactSendClicked = viewModel::onContactSendClicked
    )
}

@Composable
fun ToloStaffAndroidContent(
    state: AppShellUiState,
    onTabSelected: (AppTab) -> Unit = {},
    onHomeInstructionSelected: () -> Unit = {},
    onInstructionSelected: (String) -> Unit = {},
    onInstructionThreadOpened: () -> Unit = {},
    onInstructionDetailClosed: () -> Unit = {},
    onInstructionThreadClosed: () -> Unit = {},
    onInstructionStatusUpdated: (InstructionProgressStatus) -> Unit = {},
    onReportTypeSelected: (String) -> Unit = {},
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
) {
    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(appShellTitle(state.selectedTab)) },
                    navigationIcon = {
                        CurrentStaffHeaderIcon(
                            currentStaff = state.currentStaff,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                Column {
                    if (state.selectedTab != AppTab.CONTACTS) {
                        PlacementFooter(placementName = state.currentPlacementName)
                    }
                    NavigationBar {
                        appShellTabs().forEach { item ->
                            NavigationBarItem(
                                selected = state.selectedTab == item.tab,
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
                when (state.selectedTab) {
                    AppTab.HOME -> HomeOverviewContent(
                        overview = state.homeOverview,
                        errorMessage = state.errorMessage,
                        onOpenInstruction = onHomeInstructionSelected,
                        onOpenReport = { onTabSelected(AppTab.REPORTS) },
                        onOpenContacts = { onTabSelected(AppTab.CONTACTS) }
                    )
                    AppTab.INSTRUCTIONS -> InstructionsContent(
                        state = state.instructionsTab,
                        onInstructionSelected = onInstructionSelected,
                        onThreadOpened = onInstructionThreadOpened,
                        onDetailClosed = onInstructionDetailClosed,
                        onThreadClosed = onInstructionThreadClosed,
                        onStatusUpdated = onInstructionStatusUpdated
                    )
                    AppTab.REPORTS -> ReportsContent(
                        state = state.reportsTab,
                        onTypeSelected = onReportTypeSelected,
                        onCommentChanged = onReportCommentChanged,
                        onUrgencySelected = onReportUrgencySelected,
                        onImageToggleChanged = onReportImageToggleChanged,
                        onLocationToggleChanged = onReportLocationToggleChanged,
                        onContinueToPlaceSelection = onReportContinueToPlaceSelection,
                        onPlaceSelected = onReportPlaceSelected,
                        onSubmitted = onReportSubmitted,
                        onBack = onReportBack
                    )
                    AppTab.CONTACTS -> ContactsContent(
                        state = state.contactsTab,
                        onThreadSelected = onContactThreadSelected,
                        onBackToList = onContactBackToList,
                        onNewThreadStarted = onContactNewThreadStarted,
                        onTargetTypeSelected = onContactTargetTypeSelected,
                        onTargetSelected = onContactTargetSelected,
                        onDraftChanged = onContactDraftChanged,
                        onSendClicked = onContactSendClicked
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentStaffHeaderIcon(
    currentStaff: CurrentStaffUiModel,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = "current_mock_staff_${currentStaff.staffId}",
        modifier = modifier
            .size(28.dp)
            .semantics { contentDescription = "current_mock_staff_${currentStaff.staffId}" },
        tint = MaterialTheme.colorScheme.primary
    )
}

private fun appShellTitle(tab: AppTab): String =
    when (tab) {
        AppTab.HOME -> "ホーム"
        AppTab.INSTRUCTIONS -> "指示"
        AppTab.REPORTS -> "報告"
        AppTab.CONTACTS -> "連絡"
    }

@Composable
private fun HomeOverviewContent(
    overview: AppShellHomeOverview,
    errorMessage: String?,
    onOpenInstruction: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenContacts: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        errorMessage?.let {
            item {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { contentDescription = "app_shell_home_error" }
                )
            }
        }
        item {
            HomeOverviewCard("イベント", overview.eventName, overview.eventTime, "app_shell_home_event_card")
        }
        item {
            HomePlacementMapOverviewCard(overview, "app_shell_home_placement_map_card")
        }
        item {
            HomeOverviewCard("あなたへの指示", overview.currentInstruction, null, "app_shell_home_instruction_card")
        }
        item {
            QuickActionsCard(
                unreadContactCount = overview.unreadContactCount,
                pendingReportLabel = overview.pendingReportLabel,
                onOpenInstruction = onOpenInstruction,
                onOpenReport = onOpenReport,
                onOpenContacts = onOpenContacts
            )
        }
    }
}

@Composable
private fun InstructionsContent(
    state: InstructionsTabUiState,
    onInstructionSelected: (String) -> Unit,
    onThreadOpened: () -> Unit,
    onDetailClosed: () -> Unit,
    onThreadClosed: () -> Unit,
    onStatusUpdated: (InstructionProgressStatus) -> Unit,
) {
    when {
        state.selectedInstruction == null -> InstructionList(state.instructions, onInstructionSelected)
        state.isShowingThread -> InstructionThread(state.selectedInstruction, onThreadClosed)
        else -> InstructionDetail(state.selectedInstruction, onThreadOpened, onDetailClosed, onStatusUpdated)
    }
}

@Composable
private fun ReportsContent(
    state: ReportsTabUiState,
    onTypeSelected: (String) -> Unit,
    onCommentChanged: (String) -> Unit,
    onUrgencySelected: (String) -> Unit,
    onImageToggleChanged: (Boolean) -> Unit,
    onLocationToggleChanged: (Boolean) -> Unit,
    onContinueToPlaceSelection: () -> Unit,
    onPlaceSelected: (String) -> Unit,
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
) {
    when (state.step) {
        ReportFlowStep.TYPE_SELECTION -> ReportTypeSelection(state.reportTypes, onTypeSelected)
        ReportFlowStep.DRAFT_INPUT -> ReportDraftInput(
            state = state,
            onCommentChanged = onCommentChanged,
            onUrgencySelected = onUrgencySelected,
            onImageToggleChanged = onImageToggleChanged,
            onLocationToggleChanged = onLocationToggleChanged,
            onContinueToPlaceSelection = onContinueToPlaceSelection,
            onBack = onBack
        )
        ReportFlowStep.PLACE_SELECTION -> ReportPlaceSelection(state, onPlaceSelected, onSubmitted, onBack)
        ReportFlowStep.THREAD -> ReportThread(state, onBack)
    }
}

@Composable
private fun ContactsContent(
    state: ContactsTabUiState,
    onThreadSelected: (String) -> Unit,
    onBackToList: () -> Unit,
    onNewThreadStarted: () -> Unit,
    onTargetTypeSelected: (ContactTargetType) -> Unit,
    onTargetSelected: (String) -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
) {
    when {
        state.selectedThread != null -> ContactThreadDetail(state.selectedThread, onBackToList, onDraftChanged, onSendClicked)
        state.isChoosingTargetType -> ContactTargetSelection(state, onBackToList, onTargetTypeSelected, onTargetSelected)
        else -> ContactThreadList(state, onThreadSelected, onNewThreadStarted)
    }
}

@Composable
private fun InstructionList(
    instructions: List<InstructionSummaryUiModel>,
    onInstructionSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("指示", "現在受けている指示を確認します。") }
        items(instructions, key = { it.id }) { instruction ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInstructionSelected(instruction.id) }
                    .semantics { contentDescription = "instruction_row_${instruction.id}" },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row {
                        Text(instruction.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(instruction.priorityLabel, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(instruction.targetName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(instruction.preview, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(instruction.statusLabel)
                        if (instruction.unreadCount > 0) {
                            Text("未読 ${instruction.unreadCount}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InstructionDetail(
    instruction: InstructionDetailUiModel,
    onThreadOpened: () -> Unit,
    onDetailClosed: () -> Unit,
    onStatusUpdated: (InstructionProgressStatus) -> Unit,
) {
    BackHandler(onBack = onDetailClosed)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onDetailClosed) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("一覧へ戻る")
                }
                Spacer(Modifier.width(12.dp))
                Text("指示詳細", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Text(instruction.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(instruction.body)
            Spacer(Modifier.height(8.dp))
            Text("対象: ${instruction.target.displayName}")
            Text("優先度: ${instruction.priorityLabel} / 状態: ${instruction.statusLabel}")
            instruction.locationLabel?.let { Text("場所: $it") }
            instruction.attachmentSummary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            Text("担当者状態", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            instruction.participants.forEach { participant ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(participant.staffName)
                    Text(participant.statusLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        item {
            Text("状態を更新", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusButton("未確認") { onStatusUpdated(InstructionProgressStatus.UNCONFIRMED) }
                StatusButton("了解") { onStatusUpdated(InstructionProgressStatus.ACKNOWLEDGED) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusButton("対応中") { onStatusUpdated(InstructionProgressStatus.IN_PROGRESS) }
                StatusButton("完了") { onStatusUpdated(InstructionProgressStatus.COMPLETED) }
            }
        }
        item {
            Button(onClick = onThreadOpened) { Text("スレッドを見る") }
        }
    }
}

@Composable
private fun InstructionThread(
    instruction: InstructionDetailUiModel,
    onThreadClosed: () -> Unit,
) {
    BackHandler(onBack = onThreadClosed)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onThreadClosed) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("指示詳細へ戻る")
                }
            }
        }
        items(instruction.thread, key = { it.id }) { message ->
            ThreadMessageBubble(message)
        }
    }
}

@Composable
private fun ReportTypeSelection(
    reportTypes: List<ReportTypeUiModel>,
    onTypeSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("報告", "定型入力から本部への報告を開始します。") }
        items(reportTypes, key = { it.id }) { reportType ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onTypeSelected(reportType.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(reportType.title, fontWeight = FontWeight.SemiBold)
                    Text(reportType.detailText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ReportDraftInput(
    state: ReportsTabUiState,
    onCommentChanged: (String) -> Unit,
    onUrgencySelected: (String) -> Unit,
    onImageToggleChanged: (Boolean) -> Unit,
    onLocationToggleChanged: (Boolean) -> Unit,
    onContinueToPlaceSelection: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ScreenHeader("報告内容入力", state.draft.templateText) }
        item {
            LabeledTextField(
                label = "コメント",
                value = state.draft.comment,
                onValueChanged = onCommentChanged,
                placeholder = "現場の状況を入力"
            )
        }
        item {
            Text("緊急度", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("通常", "高", "緊急").forEach { label ->
                    FilterChip(
                        selected = state.draft.urgencyLabel == label,
                        onClick = { onUrgencySelected(label) },
                        label = { Text(label) }
                    )
                }
            }
        }
        item {
            ToggleRow("画像を添付", state.draft.includesImage, onImageToggleChanged)
            Spacer(Modifier.height(8.dp))
            ToggleRow("位置情報を添付", state.draft.includesLocation, onLocationToggleChanged)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("戻る") }
                Button(onClick = onContinueToPlaceSelection) { Text("対象場所を選ぶ") }
            }
        }
    }
}

@Composable
private fun ReportPlaceSelection(
    state: ReportsTabUiState,
    onPlaceSelected: (String) -> Unit,
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("対象場所を選択", "宛先は本部、対象場所だけを指定します。") }
        items(state.availablePlaces, key = { it.id }) { place ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onPlaceSelected(place.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (state.draft.selectedPlaceId == place.id) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(place.displayName, fontWeight = FontWeight.SemiBold)
                    place.subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack) { Text("戻る") }
                Button(onClick = onSubmitted, enabled = state.draft.selectedPlaceId != null) { Text("本部へ送信") }
            }
        }
    }
}

@Composable
private fun ReportThread(
    state: ReportsTabUiState,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(state.submittedThread?.title ?: "報告スレッド", state.submittedThread?.targetLabel ?: "")
        }
        item {
            Text(state.submittedThread?.lastSubmittedSummary ?: "")
        }
        items(state.submittedThread?.messages ?: emptyList(), key = { it.id }) { message ->
            ThreadMessageBubble(message)
        }
        item {
            OutlinedButton(onClick = onBack) { Text("報告内容へ戻る") }
        }
    }
}

@Composable
private fun ContactThreadList(
    state: ContactsTabUiState,
    onThreadSelected: (String) -> Unit,
    onNewThreadStarted: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("連絡", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Button(onClick = onNewThreadStarted) {
                    Icon(Icons.Default.AddComment, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("新規連絡")
                }
            }
        }
        item { Text("現在の連絡", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.threads.filter { !it.isFormerAssignment }, key = { it.id }) { thread ->
            ContactThreadRow(thread, onThreadSelected)
        }
        if (state.threads.any { it.isFormerAssignment }) {
            item { Text("旧担当", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(state.threads.filter { it.isFormerAssignment }, key = { it.id }) { thread ->
                ContactThreadRow(thread, onThreadSelected)
            }
        }
        items(state.formerAssignments, key = { it.id }) { assignment ->
            Text(assignment.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContactTargetSelection(
    state: ContactsTabUiState,
    onBackToList: () -> Unit,
    onTargetTypeSelected: (ContactTargetType) -> Unit,
    onTargetSelected: (String) -> Unit,
) {
    BackHandler(onBack = onBackToList)
    val selectedTargets = state.availableTargets.filter { it.type == state.selectedTargetType }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ScreenHeader("宛先を選択", "担当場所 / 担当ロール / 本部 / 個人から選びます。") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContactTypeChip("担当場所", state.selectedTargetType == ContactTargetType.PLACE) { onTargetTypeSelected(ContactTargetType.PLACE) }
                ContactTypeChip("担当ロール", state.selectedTargetType == ContactTargetType.ROLE) { onTargetTypeSelected(ContactTargetType.ROLE) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ContactTypeChip("本部", state.selectedTargetType == ContactTargetType.HEADQUARTERS) { onTargetTypeSelected(ContactTargetType.HEADQUARTERS) }
                ContactTypeChip("個人", state.selectedTargetType == ContactTargetType.USER) { onTargetTypeSelected(ContactTargetType.USER) }
            }
        }
        items(selectedTargets, key = { it.id }) { target ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onTargetSelected(target.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(target.displayName, fontWeight = FontWeight.SemiBold)
                    target.subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        item {
            OutlinedButton(onClick = onBackToList) { Text("連絡一覧へ戻る") }
        }
    }
}

@Composable
private fun ContactThreadDetail(
    thread: ContactThreadDetailUiModel,
    onBackToList: () -> Unit,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
) {
    BackHandler(onBack = onBackToList)
    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onBackToList) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("一覧へ戻る")
                    }
                }
            }
            item {
                Text(thread.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(thread.target.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(thread.messages, key = { it.id }) { message ->
                ThreadMessageBubble(message)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = thread.draftMessage,
                    onValueChange = onDraftChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (thread.draftMessage.isEmpty()) {
                            Text("メッセージ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        inner()
                    }
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onSendClicked,
                enabled = thread.canReply && thread.draftMessage.isNotBlank()
            ) { Text("送信") }
        }
    }
}

@Composable
private fun ContactThreadRow(
    thread: ContactThreadSummaryUiModel,
    onThreadSelected: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onThreadSelected(thread.id) }
            .semantics { contentDescription = "contact_thread_${thread.id}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(thread.title, fontWeight = FontWeight.SemiBold)
                Text(thread.lastMessagePreview, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (thread.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(thread.unreadCount.toString(), color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ThreadMessageBubble(message: ThreadMessageUiModel) {
    Row(Modifier.fillMaxWidth()) {
        if (message.isCurrentUser) Spacer(Modifier.weight(1f))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = message.senderRoleLabel?.let { "${message.senderName} ($it)" } ?: message.senderName,
                    color = if (message.isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = message.body,
                    color = if (message.isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                message.timeLabel?.let {
                    Text(
                        text = it,
                        color = if (message.isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        if (!message.isCurrentUser) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun HomePlacementMapOverviewCard(
    overview: AppShellHomeOverview,
    identifier: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = identifier },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("あなたの配置場所", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(overview.placementName, fontWeight = FontWeight.SemiBold)
            Text(overview.placementDetail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(overview.mapState.venueName)
            Text("${overview.mapState.latitude}, ${overview.mapState.longitude}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeOverviewCard(
    title: String,
    primaryText: String,
    secondaryText: String?,
    identifier: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = identifier },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(primaryText, fontWeight = FontWeight.SemiBold)
            secondaryText?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun QuickActionsCard(
    unreadContactCount: Int,
    pendingReportLabel: String,
    onOpenInstruction: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenContacts: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "app_shell_home_quick_actions" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("業務導線", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallActionButton("指示詳細", onOpenInstruction)
                SmallActionButton("報告する", onOpenReport)
                SmallActionButton(if (unreadContactCount > 0) "未読 $unreadContactCount" else "連絡を見る", onOpenContacts)
            }
            Text(pendingReportLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SmallActionButton(title: String, action: () -> Unit) {
    Button(
        onClick = action,
        modifier = Modifier.height(44.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    ) {
        Text(title, maxLines = 1)
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
            Text("あなたの配置場所", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                placementName,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { contentDescription = "app_shell_placement_name" }
            )
        }
    }
}

@Composable
private fun ScreenHeader(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChanged)
    }
}

@Composable
private fun StatusButton(title: String, action: () -> Unit) {
    Button(
        onClick = action,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun ContactTypeChip(title: String, selected: Boolean, action: () -> Unit) {
    FilterChip(selected = selected, onClick = action, label = { Text(title) })
}

private data class AppShellTabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
)

private fun appShellTabs(): List<AppShellTabItem> = listOf(
    AppShellTabItem(AppTab.HOME, "ホーム", Icons.Default.Home),
    AppShellTabItem(AppTab.INSTRUCTIONS, "指示", Icons.Default.Summarize),
    AppShellTabItem(AppTab.REPORTS, "報告", Icons.Default.Mail),
    AppShellTabItem(AppTab.CONTACTS, "連絡", Icons.Default.Message),
)

@Preview
@Composable
private fun PreviewToloStaffAndroidContent() {
    ToloStaffAndroidContent(state = AppShellUiState())
}
