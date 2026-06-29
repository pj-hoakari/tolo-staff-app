package dev.usbharu.tolo_staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.TaskAlt
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
                EmptyInstructionCard(identifier = "instruction_empty_state")
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
internal fun InstructionDetailScreen(
    instruction: InstructionDetailUiModel,
    onThreadOpened: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader("指示詳細", "概要を確認して、必要なら関連スレッドに移動できます。")
        }
        item {
            InstructionDetailHero(instruction = instruction)
        }
        item {
            Button(
                onClick = onThreadOpened,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "instruction_detail_open_thread_button" }
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
@OptIn(ExperimentalLayoutApi::class)
private fun InstructionDetailHero(instruction: InstructionDetailUiModel) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "優先対応",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    instruction.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            CompactInfoChip(
                instruction.statusLabel,
                emphasized = instruction.statusLabel == "対応中" || instruction.statusLabel == "完了"
            )
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactInfoChip(instruction.target.displayName)
            if (instruction.priorityLabel.isNotBlank()) {
                CompactInfoChip(instruction.priorityLabel)
            }
            instruction.locationLabel?.let { locationLabel ->
                CompactInfoChip(locationLabel)
            }
        }

        Text(
            instruction.body,
            style = MaterialTheme.typography.bodyLarge
        )

        instruction.locationLabel?.let {
            DetailInfoRow(icon = Icons.Default.Place, label = "場所", value = it)
        }
        instruction.attachmentSummary?.let {
            DetailInfoRow(icon = Icons.Default.TaskAlt, label = "共有物", value = it)
        }
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
