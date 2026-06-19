package dev.usbharu.tolo_staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.ReportFlowStep
import dev.usbharu.tolo_staff.feature.appshell.ReportTypeUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportsTabUiState

@Composable
internal fun ReportTypeSelectionScreen(
    reportTypes: List<ReportTypeUiModel>,
    onTypeSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("報告", "本部へ送る報告種別を選択します。")
        }
        items(reportTypes, key = { it.id }) { reportType ->
            SectionCard(
                onClick = { onTypeSelected(reportType.id) },
                contentDescription = "report_type_${reportType.id}",
            ) {
                Text(reportType.title, fontWeight = FontWeight.SemiBold)
                Text(reportType.detailText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ReportDraftInputScreen(
    state: ReportsTabUiState,
    onCommentChanged: (String) -> Unit,
    onUrgencySelected: (String) -> Unit,
    onImageToggleChanged: (Boolean) -> Unit,
    onLocationToggleChanged: (Boolean) -> Unit,
    onContinueToPlaceSelection: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader("報告内容入力", state.draft.templateText.ifBlank { "定型文を元に報告内容を整えます。" })
        }
        item {
            LabeledTextField(
                label = "コメント",
                value = state.draft.comment,
                onValueChanged = onCommentChanged,
                placeholder = "現場の状況を入力"
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("緊急度", fontWeight = FontWeight.SemiBold)
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
        }
        item {
            SectionCard {
                ToggleRow("画像を添付", state.draft.includesImage, onImageToggleChanged)
                ToggleRow("位置情報を添付", state.draft.includesLocation, onLocationToggleChanged)
            }
        }
        item {
            Button(onClick = onContinueToPlaceSelection) {
                Text("対象場所を選ぶ")
            }
        }
    }
}

@Composable
internal fun ReportPlaceSelectionScreen(
    state: ReportsTabUiState,
    onPlaceSelected: (String) -> Unit,
    onSubmitted: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("対象場所", "報告対象となる配置場所を選択します。")
        }
        items(state.availablePlaces, key = { it.id }) { place ->
            SectionCard(
                onClick = { onPlaceSelected(place.id) }
            ) {
                Text(place.displayName, fontWeight = FontWeight.SemiBold)
                place.subtitle?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (state.draft.selectedPlaceId == place.id) {
                    Text("選択中", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Button(
                onClick = onSubmitted,
                enabled = state.draft.selectedPlaceId != null,
            ) {
                Text("本部へ送信")
            }
        }
    }
}

@Composable
internal fun ReportThreadScreen(
    state: ReportsTabUiState,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = AppShellRoutes.REPORTS_THREAD },
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader(
                title = state.submittedThread?.title ?: "報告スレッド",
                description = state.submittedThread?.targetLabel ?: "送信済みの報告内容"
            )
        }
        item {
            SectionCard {
                Text(state.submittedThread?.lastSubmittedSummary ?: "")
            }
        }
        items(state.submittedThread?.messages.orEmpty(), key = { it.id }) { message ->
            ThreadMessageBubble(message)
        }
    }
}

internal fun reportTitleForStep(step: ReportFlowStep): String =
    when (step) {
        ReportFlowStep.TYPE_SELECTION -> "報告"
        ReportFlowStep.DRAFT_INPUT -> "報告内容入力"
        ReportFlowStep.PLACE_SELECTION -> "対象場所"
        ReportFlowStep.THREAD -> "報告スレッド"
    }
