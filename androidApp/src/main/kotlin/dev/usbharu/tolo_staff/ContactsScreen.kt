package dev.usbharu.tolo_staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.ContactTargetType
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactThreadSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.ContactsTabUiState

@Composable
internal fun ContactThreadListScreen(
    state: ContactsTabUiState,
    onThreadSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                title = "連絡",
                description = "現在のスレッドと新規連絡の開始",
            )
        }
        item { Text("現在の連絡", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(state.threads, key = { it.id }) { thread ->
            ContactThreadRow(thread, onThreadSelected)
        }
    }
}

@Composable
internal fun ContactTargetSelectionScreen(
    state: ContactsTabUiState,
    onTargetTypeSelected: (ContactTargetType) -> Unit,
    onTargetSelected: (String) -> Unit,
) {
    val selectedTargetType = state.selectedTargetType ?: ContactTargetType.PLACE
    val selectedTargets = state.availableTargets.filter { it.type == selectedTargetType }
    val targetTypes = listOf(
        ContactTargetType.PLACE to "担当場所",
        ContactTargetType.USER to "個人",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "contact_target_selection" },
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("宛先を選択", "対象の種別を選んでから候補を選択します。")
        }
        item {
            SectionCard {
                Text("宛先", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("contact_target_tabs")
                ) {
                    targetTypes.forEachIndexed { index, (type, label) ->
                        SegmentedButton(
                            selected = selectedTargetType == type,
                            onClick = { onTargetTypeSelected(type) },
                            shape = SegmentedButtonDefaults.itemShape(index, targetTypes.size),
                            modifier = Modifier.testTag("contact_target_tab_${type.name.lowercase()}"),
                            label = { Text(label) },
                        )
                    }
                }

                HorizontalDivider()
                if (selectedTargets.isEmpty()) {
                    Text(
                        text = "選択できる宛先がありません",
                        modifier = Modifier.testTag("contact_target_empty"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    selectedTargets.forEach { target ->
                        TextButton(
                            onClick = { onTargetSelected(target.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("contact_target_${target.id}"),
                        ) {
                            Text(
                                text = target.displayName,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ContactThreadDetailScreen(
    thread: ContactThreadDetailUiModel,
    onDraftChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "contact_thread_detail" }
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionCard {
                    Text(thread.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(thread.target.displayName, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(thread.messages, key = { it.id }) { message ->
                ThreadMessageBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = thread.draftMessage,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "contact_chat_message_input" },
                placeholder = { Text("メッセージ") },
                enabled = thread.canReply,
                maxLines = 4,
            )
            Button(
                onClick = onSendClicked,
                enabled = thread.canReply && thread.draftMessage.isNotBlank(),
                modifier = Modifier.semantics { contentDescription = "contact_chat_send_button" }
            ) {
                Text("送信")
            }
        }
    }
}

@Composable
private fun ContactThreadRow(
    thread: ContactThreadSummaryUiModel,
    onThreadSelected: (String) -> Unit,
) {
    SectionCard(
        onClick = { onThreadSelected(thread.id) },
        contentDescription = "contact_thread_${thread.id}",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(thread.title, fontWeight = FontWeight.SemiBold)
                Text(
                    thread.lastMessagePreview,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (thread.unreadCount > 0) {
                Text(
                    text = thread.unreadCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
