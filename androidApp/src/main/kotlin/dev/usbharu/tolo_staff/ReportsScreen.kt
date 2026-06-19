package dev.usbharu.tolo_staff

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.usbharu.tolo_staff.feature.appshell.ReportFlowStep
import dev.usbharu.tolo_staff.feature.appshell.ReportTypeUiModel
import dev.usbharu.tolo_staff.feature.appshell.ReportsTabUiState

@Composable
internal fun ReportTypeSelectionScreen(
    state: ReportsTabUiState,
    onTypeSelected: (String) -> Unit,
    onReportSelected: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("報告", "本部へ送る報告種別を選択します。")
        }
        items(state.reportTypes, key = { it.id }) { reportType ->
            SectionCard(
                onClick = { onTypeSelected(reportType.id) },
                contentDescription = "report_type_${reportType.id}",
            ) {
                Text(reportType.title, fontWeight = FontWeight.SemiBold)
                Text(reportType.detailText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ScreenHeader("関連報告一覧", "自分が作成した報告と担当に関連する報告を表示します。")
        }
        when {
            state.isLoadingReports -> item {
                SectionCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("関連報告を読み込んでいます")
                    }
                }
            }

            state.reportsErrorMessage != null -> item {
                SectionCard {
                    val errorMessage = state.reportsErrorMessage.orEmpty()
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }

            state.relatedReports.isEmpty() -> item {
                SectionCard {
                    Text("関連する報告はまだありません")
                }
            }

            else -> items(state.relatedReports, key = { it.reportId }) { report ->
                SectionCard(
                    onClick = { onReportSelected(report.reportId) },
                    contentDescription = "related_report_${report.reportId}",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(report.title, fontWeight = FontWeight.SemiBold)
                            if (report.isAuthoredByCurrentStaff) {
                                Text("自分の報告", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (report.priorityLabel.isNotBlank()) {
                            Text(report.priorityLabel, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(report.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${report.targetLabel} / ${report.authorName}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        report.timeLabel?.let { timeLabel ->
                            Text(timeLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
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

internal fun reportTitleForStep(step: ReportFlowStep): String =
    when (step) {
        ReportFlowStep.TYPE_SELECTION -> "報告"
        ReportFlowStep.DRAFT_INPUT -> "報告内容入力"
        ReportFlowStep.PLACE_SELECTION -> "対象場所"
    }
