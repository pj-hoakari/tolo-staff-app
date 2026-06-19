package dev.usbharu.tolo_staff

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.CurrentStaffUiModel
import dev.usbharu.tolo_staff.feature.appshell.InstructionSummaryUiModel
import dev.usbharu.tolo_staff.feature.appshell.ThreadMessageUiModel

@Composable
internal fun CurrentStaffHeaderIcon(
    currentStaff: CurrentStaffUiModel,
    availableStaff: List<CurrentStaffUiModel>,
    onCurrentStaffSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(enabled = availableStaff.isNotEmpty()) { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .semantics { contentDescription = "current_staff_${currentStaff.staffId}" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = currentStaff.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                currentStaff.roleLabel?.takeIf { it.isNotBlank() }?.let { roleLabel ->
                    Text(
                        text = roleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableStaff.forEach { staff ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(staff.displayName)
                            staff.roleLabel?.takeIf { it.isNotBlank() }?.let { roleLabel ->
                                Text(
                                    text = roleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onCurrentStaffSelected(staff.staffId)
                    }
                )
            }
        }
    }
}

@Composable
internal fun PlacementFooter(placementName: String) {
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
            Text("現在の配置", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                placementName,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { contentDescription = "app_shell_placement_name" }
            )
        }
    }
}

@Composable
internal fun ScreenHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
internal fun InstructionHeroCard(
    titleLabel: String,
    title: String?,
    body: String,
    targetName: String?,
    priorityLabel: String?,
    statusLabel: String?,
    locationLabel: String?,
    attachmentSummary: String?,
    unreadCount: Int,
    identifier: String,
    onClick: () -> Unit,
) {
    SectionCard(
        onClick = onClick,
        contentDescription = identifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                titleLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                priorityLabel?.takeIf { it.isNotBlank() }?.let {
                    InstructionPill(it, instructionPriorityContainerColor(it), instructionPriorityContentColor(it))
                }
                statusLabel?.takeIf { it.isNotBlank() }?.let {
                    InstructionPill(it, instructionStatusContainerColor(it), instructionStatusContentColor(it))
                }
            }
        }

        title?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
        )

        InstructionMetaSummary(
            targetName = targetName,
            locationLabel = locationLabel,
            attachmentSummary = attachmentSummary,
            unreadCount = unreadCount,
        )
    }
}

@Composable
internal fun FeaturedInstructionCard(
    instruction: InstructionSummaryUiModel,
    identifier: String,
    onClick: () -> Unit,
) {
    InstructionHeroCard(
        titleLabel = "あなたへの指示",
        title = instruction.title,
        body = instruction.preview,
        targetName = instruction.targetName,
        priorityLabel = instruction.priorityLabel,
        statusLabel = instruction.statusLabel,
        locationLabel = instruction.locationLabel,
        attachmentSummary = instruction.attachmentSummary,
        unreadCount = instruction.unreadCount,
        identifier = identifier,
        onClick = onClick,
    )
}

@Composable
private fun InstructionMetaSummary(
    targetName: String?,
    locationLabel: String?,
    attachmentSummary: String?,
    unreadCount: Int,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        targetName?.takeIf { it.isNotBlank() }?.let {
            InstructionSecondaryPill(
                text = it,
                icon = Icons.Default.AccountCircle,
            )
        }
        locationLabel?.takeIf { it.isNotBlank() }?.let {
            InstructionSecondaryPill(
                text = it,
                icon = Icons.Default.LocationOn,
            )
        }
        attachmentSummary?.takeIf { it.isNotBlank() }?.let {
            InstructionSecondaryPill(
                text = it,
                icon = Icons.Default.AttachFile,
            )
        }
        if (unreadCount > 0) {
            InstructionSecondaryPill(
                text = "$unreadCount 件",
                icon = Icons.Default.MarkEmailUnread,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun InstructionPill(
    text: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun InstructionSecondaryPill(
    text: String,
    icon: ImageVector,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun instructionPriorityContainerColor(priorityLabel: String): Color =
    when (priorityLabel) {
        "高" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

@Composable
private fun instructionPriorityContentColor(priorityLabel: String): Color =
    when (priorityLabel) {
        "高" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
private fun instructionStatusContainerColor(statusLabel: String): Color =
    when (statusLabel) {
        "対応中" -> MaterialTheme.colorScheme.primaryContainer
        "完了" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

@Composable
private fun instructionStatusContentColor(statusLabel: String): Color =
    when (statusLabel) {
        "対応中" -> MaterialTheme.colorScheme.onPrimaryContainer
        "完了" -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
internal fun LabeledTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = 3,
        )
    }
}

@Composable
internal fun ToggleRow(
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
internal fun StatusButton(title: String, action: () -> Unit) {
    Button(
        onClick = action,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
internal fun ContactTypeChip(title: String, selected: Boolean, action: () -> Unit) {
    FilterChip(selected = selected, onClick = action, label = { Text(title) })
}

@Composable
internal fun ThreadMessageBubble(message: ThreadMessageUiModel) {
    Row(Modifier.fillMaxWidth()) {
        if (message.isCurrentUser) {
            Spacer(Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isSystemEvent -> MaterialTheme.colorScheme.surfaceVariant
                    message.isCurrentUser -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondaryContainer
                }
            ),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .semantics { contentDescription = "contact_chat_message_${message.id}" }
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message.senderRoleLabel?.let { "${message.senderName} ($it)" } ?: message.senderName,
                    color = if (message.isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = message.body,
                    color = if (message.isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                message.timeLabel?.let {
                    Text(
                        text = it,
                        color = if (message.isCurrentUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        if (!message.isCurrentUser) {
            Spacer(Modifier.weight(1f))
        }
    }
}
