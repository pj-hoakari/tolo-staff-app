package dev.usbharu.tolo_staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.InstructionDetailUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionParticipantStatusUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionProgressStatus
import dev.usbharu.tolo_staff.feature.appshell.InstructionSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionsTabUiState

@Composable
internal fun InstructionsListScreen(
    state: InstructionsTabUiState,
    onInstructionSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.featuredInstruction?.let { instruction ->
            item {
                FeaturedInstructionCard(
                    instruction = instruction,
                    identifier = "featured_instruction_card",
                    onClick = { onInstructionSelected(instruction.id) },
                )
            }
        }
        if (state.featuredInstruction == null && state.otherInstructions.isEmpty()) {
            item {
                EmptyInstructionCard()
            }
        }
        if (state.otherInstructions.isNotEmpty()) {
            item {
                Text("その他の指示", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(state.otherInstructions, key = { it.id }) { instruction ->
                InstructionListRow(instruction = instruction, onInstructionSelected = onInstructionSelected)
            }
        }
    }
}

@Composable
private fun EmptyInstructionCard() {
    SectionCard(
        contentDescription = "instruction_empty_state",
    ) {
        Text(
            "あなたへの指示",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            "表示できる指示はまだありません",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "担当エリア向け、またはあなた宛ての指示が届くとここに表示されます。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun InstructionDetailScreen(
    instruction: InstructionDetailUiModel,
    onThreadOpened: () -> Unit,
    onStatusUpdated: (InstructionProgressStatus) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader("指示詳細", "内容確認、担当状況、進捗更新をこの画面で完結します。")
        }
        item {
            InstructionDetailHero(instruction)
        }
        item {
            SectionCard {
                Text("担当者状態", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                instruction.participants.forEach { participant ->
                    ParticipantStatusRow(participant)
                }
            }
        }
        item {
            SectionCard {
                Text("状態を更新", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusActionButton(
                        title = "未確認",
                        selected = instruction.statusLabel == "未確認",
                        modifier = Modifier.weight(1f)
                    ) { onStatusUpdated(InstructionProgressStatus.UNCONFIRMED) }
                    StatusActionButton(
                        title = "了解",
                        selected = instruction.statusLabel == "了解",
                        modifier = Modifier.weight(1f)
                    ) { onStatusUpdated(InstructionProgressStatus.ACKNOWLEDGED) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusActionButton(
                        title = "対応中",
                        selected = instruction.statusLabel == "対応中",
                        modifier = Modifier.weight(1f)
                    ) { onStatusUpdated(InstructionProgressStatus.IN_PROGRESS) }
                    StatusActionButton(
                        title = "完了",
                        selected = instruction.statusLabel == "完了",
                        modifier = Modifier.weight(1f)
                    ) { onStatusUpdated(InstructionProgressStatus.COMPLETED) }
                }
            }
        }
        item {
            Button(
                onClick = onThreadOpened,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("関連スレッドを開く")
            }
        }
    }
}

@Composable
private fun InstructionListRow(
    instruction: InstructionSummaryUiModel,
    onInstructionSelected: (String) -> Unit,
) {
    SectionCard(
        onClick = { onInstructionSelected(instruction.id) },
        contentDescription = "instruction_row_${instruction.id}",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Text(instruction.title, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    instruction.preview,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactInfoChip(instruction.targetName)
            CompactInfoChip(instruction.statusLabel)
            if (instruction.priorityLabel.isNotBlank()) {
                CompactInfoChip(instruction.priorityLabel)
            }
            if (instruction.unreadCount > 0) {
                CompactInfoChip("未読 ${instruction.unreadCount}", emphasized = true)
            }
        }
    }
}

@Composable
private fun InstructionDetailHero(instruction: InstructionDetailUiModel) {
    SectionCard {
        Text(instruction.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(instruction.body, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactInfoChip(instruction.target.displayName)
            CompactInfoChip(
                instruction.statusLabel,
                emphasized = instruction.statusLabel == "対応中" || instruction.statusLabel == "完了"
            )
            CompactInfoChip(instruction.priorityLabel.ifBlank { "通常" })
        }
        instruction.locationLabel?.let {
            DetailInfoRow(icon = Icons.Default.Place, label = "場所", value = it)
        }
        instruction.attachmentSummary?.let {
            DetailInfoRow(icon = Icons.Default.Groups, label = "添付", value = it)
        }
    }
}

@Composable
private fun ParticipantStatusRow(participant: InstructionParticipantStatusUiModel) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (participant.isCurrentStaff) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(participant.staffName, fontWeight = FontWeight.SemiBold)
                if (participant.isCurrentStaff) {
                    Text("あなた", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            CompactInfoChip(participant.statusLabel, emphasized = participant.isCurrentStaff)
        }
    }
}

@Composable
private fun StatusActionButton(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    ) {
        Text(title)
    }
}

@Composable
private fun MetricPill(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(value, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactInfoChip(
    text: String,
    emphasized: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (emphasized) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun DetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value)
        }
    }
}
