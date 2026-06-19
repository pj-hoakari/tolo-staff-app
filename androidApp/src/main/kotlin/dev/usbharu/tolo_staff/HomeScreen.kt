package dev.usbharu.tolo_staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.AppShellHomeOverview
import dev.usbharu.tolo_staff.feature.appshell.InstructionSummaryUiModel

@Composable
internal fun HomeOverviewContent(
    overview: AppShellHomeOverview,
    errorMessage: String?,
    isLoading: Boolean,
    onOpenInstruction: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenContacts: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                HeroShiftCard(overview = overview)
            }
            item {
                PlacementSummaryCard(overview = overview)
            }
            item {
                HomeInstructionOverviewCard(
                    overview = overview,
                    identifier = "app_shell_home_instruction_card",
                    onClick = onOpenInstruction,
                )
            }
            item {
                QuickActionsCard(
                    unreadContactCount = overview.unreadContactCount,
                    pendingReportLabel = overview.pendingReportLabel,
                    onOpenReport = onOpenReport,
                    onOpenContacts = onOpenContacts,
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun HeroShiftCard(overview: AppShellHomeOverview) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "app_shell_home_event_card" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null)
                Text("本日のイベント", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = overview.eventName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = overview.eventTime,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlacementSummaryCard(overview: AppShellHomeOverview) {
    SectionCard(contentDescription = "app_shell_home_placement_map_card") {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("配置場所", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(overview.placementName, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(overview.placementDetail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = overview.mapState.venueName,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "緯度 ${overview.mapState.latitude}, 経度 ${overview.mapState.longitude}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuickActionsCard(
    unreadContactCount: Int,
    pendingReportLabel: String,
    onOpenReport: () -> Unit,
    onOpenContacts: () -> Unit,
) {
    SectionCard(contentDescription = "app_shell_home_quick_actions") {
        Text("主要アクション", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton(
                title = "報告する",
                icon = Icons.Default.Description,
                onClick = onOpenReport,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                title = if (unreadContactCount > 0) "未読 $unreadContactCount" else "連絡を見る",
                icon = Icons.Default.Message,
                onClick = onOpenContacts,
                modifier = Modifier.weight(1f)
            )
        }
        Text(pendingReportLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp)
    ) {
        Icon(icon, contentDescription = null)
        Text(
            text = title,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun HomeInstructionOverviewCard(
    overview: AppShellHomeOverview,
    identifier: String,
    onClick: () -> Unit,
) {
    if (overview.currentInstruction.isBlank()) {
        EmptyInstructionCard(identifier = identifier)
    } else {
        FeaturedInstructionCard(
            instruction = overview.asFeaturedInstructionSummary(),
            identifier = identifier,
            onClick = onClick,
        )
    }
}

private fun AppShellHomeOverview.asFeaturedInstructionSummary(): InstructionSummaryUiModel {
    return InstructionSummaryUiModel(
        id = currentInstructionId ?: "home-current-instruction",
        title = currentInstructionTitle.orEmpty(),
        targetName = currentInstructionTargetName.orEmpty(),
        priorityLabel = currentInstructionPriorityLabel.orEmpty(),
        statusLabel = currentInstructionStatusLabel.orEmpty(),
        preview = currentInstruction,
        locationLabel = currentInstructionLocationLabel,
        attachmentSummary = currentInstructionAttachmentSummary,
        unreadCount = currentInstructionUnreadCount,
    )
}
