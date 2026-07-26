package com.yangi.engvocab.feature.importphoto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yangi.engvocab.core.image.TempImageStore
import com.yangi.engvocab.core.openai.Confidence
import com.yangi.engvocab.ui.components.AppTopBar
import com.yangi.engvocab.ui.components.EmptyState
import com.yangi.engvocab.ui.components.IconActionButton
import com.yangi.engvocab.ui.components.StatusBanner

@Composable
fun PhotoImportRoute(
    viewModel: PhotoImportViewModel,
    tempImageStore: TempImageStore,
    onBack: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onStudySaved: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCamera by remember { mutableStateOf(false) }
    if (showCamera) {
        CameraCaptureScreen(
            onCaptured = { viewModel.attach(it); showCamera = false },
            onCancel = { showCamera = false },
            onError = { message ->
                viewModel.reportError(message)
                showCamera = false
            },
            modifier = modifier,
        )
    } else {
        PhotoImportScreen(
            state = state,
            tempImageStore = tempImageStore,
            onSelectBook = viewModel::selectBook,
            onOpenCamera = { showCamera = true },
            onPhotoSelected = viewModel::attach,
            onPhotoError = viewModel::reportError,
            onAnalyze = viewModel::analyze,
            onUpdateRow = viewModel::updateRow,
            onDuplicateAction = viewModel::setDuplicateAction,
            onDeleteRow = viewModel::deleteRow,
            onAddRow = viewModel::addRow,
            onSave = viewModel::save,
            onCancel = { viewModel.cancel(); onBack() },
            onOpenBook = { state.selectedBookId?.let(onOpenBook) },
            onStudySaved = { onStudySaved(state.savedWordIds) },
            modifier = modifier,
        )
    }
}

@Composable
fun PhotoImportScreen(
    state: PhotoImportState,
    tempImageStore: TempImageStore,
    onSelectBook: (Long) -> Unit,
    onOpenCamera: () -> Unit,
    onPhotoSelected: (java.nio.file.Path) -> Unit,
    onPhotoError: (String) -> Unit,
    onAnalyze: () -> Unit,
    onUpdateRow: (String, String?, String?) -> Unit,
    onDuplicateAction: (String, DuplicateAction) -> Unit,
    onDeleteRow: (String) -> Unit,
    onAddRow: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onOpenBook: () -> Unit,
    onStudySaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "사진으로 단어장 만들기", onBack = onCancel) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ImportProgress(state.phase)
            state.error?.let { StatusBanner(it, isError = true) }
            when (state.phase) {
                ImportPhase.DESTINATION -> {
                    Text("저장할 단어장", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "사진에서 찾은 단어를 담을 곳을 선택하세요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.books.isEmpty()) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            title = "단어장이 필요해요",
                            body = "먼저 내 단어장에서 새 단어장을 만들어 주세요.",
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.books, key = { it.id }) { book ->
                                Card(
                                    onClick = { onSelectBook(book.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(book.name, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                ImportPhase.SOURCE -> {
                    Text("사진 가져오기", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "글자가 선명하고 수평인 사진일수록 정확해요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                        Text("카메라로 촬영", Modifier.padding(start = 8.dp))
                    }
                    PhotoPickerButton(
                        tempImageStore = tempImageStore,
                        onSelected = onPhotoSelected,
                        onError = onPhotoError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ImportPhase.PREVIEW -> {
                    Text("사진 미리보기", style = MaterialTheme.typography.headlineMedium)
                    state.imagePath?.let { path ->
                        AsyncImage(
                            model = path.toFile(),
                            contentDescription = "분석할 단어 사진",
                            modifier = Modifier.fillMaxWidth().weight(1f).clip(MaterialTheme.shapes.large),
                        )
                    }
                    Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                        Text("AI로 분석", Modifier.padding(start = 8.dp))
                    }
                }

                ImportPhase.ANALYZING, ImportPhase.SAVING -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Text(
                            if (state.phase == ImportPhase.ANALYZING) "사진에서 단어를 찾고 있어요" else "단어장에 저장하고 있어요",
                            modifier = Modifier.padding(top = 18.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "잠시만 기다려 주세요.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                ImportPhase.REVIEW -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text("분석 결과 검토", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                "${state.rows.size}개 항목",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = onAddRow) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Text("추가")
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.rows, key = { it.id }) { row ->
                            ImportRowCard(
                                row = row,
                                onExpressionChange = { onUpdateRow(row.id, it, null) },
                                onMeaningChange = { onUpdateRow(row.id, null, it) },
                                onDuplicateAction = { onDuplicateAction(row.id, it) },
                                onDelete = { onDeleteRow(row.id) },
                            )
                        }
                    }
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Text("단어장에 저장", Modifier.padding(start = 8.dp))
                    }
                }

                ImportPhase.COMPLETE -> {
                    EmptyState(
                        icon = Icons.Rounded.CheckCircle,
                        title = "${state.savedWordIds.size}개 단어를 저장했어요",
                        body = "바로 학습하거나 단어장에서 결과를 확인하세요.",
                    )
                    Button(
                        onClick = onStudySaved,
                        enabled = state.savedWordIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("지금 학습하기")
                    }
                    OutlinedButton(onClick = onOpenBook, modifier = Modifier.fillMaxWidth()) {
                        Text("단어장으로 이동")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportProgress(phase: ImportPhase) {
    val (label, progress) = when (phase) {
        ImportPhase.DESTINATION -> "단어장 선택" to 0.2f
        ImportPhase.SOURCE -> "사진 선택" to 0.4f
        ImportPhase.PREVIEW, ImportPhase.ANALYZING -> "AI 분석" to 0.6f
        ImportPhase.REVIEW, ImportPhase.SAVING -> "결과 검토" to 0.8f
        ImportPhase.COMPLETE -> "완료" to 1f
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ImportRowCard(
    row: ImportRow,
    onExpressionChange: (String) -> Unit,
    onMeaningChange: (String) -> Unit,
    onDuplicateAction: (DuplicateAction) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    row.isLowConfidence -> AssistChip(
                        onClick = {},
                        label = { Text("확인 필요") },
                        leadingIcon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null) },
                    )
                    row.confidence == Confidence.HIGH -> AssistChip(
                        onClick = {},
                        label = { Text("높은 신뢰도") },
                        leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                    )
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                IconActionButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "항목 삭제",
                    onClick = onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = row.expression,
                onValueChange = onExpressionChange,
                label = { Text("영어 표현") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = row.meaning,
                onValueChange = onMeaningChange,
                label = { Text("뜻") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (row.duplicateWordId != null) {
                StatusBanner("중복 · ${row.duplicateExpression} / ${row.duplicateMeaning.orEmpty()}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDuplicateAction(DuplicateAction.SKIP) }) {
                        Text(if (row.duplicateAction == DuplicateAction.SKIP) "✓ 건너뛰기" else "건너뛰기")
                    }
                    OutlinedButton(onClick = { onDuplicateAction(DuplicateAction.EDIT) }) {
                        Text(if (row.duplicateAction == DuplicateAction.EDIT) "✓ 수정" else "수정")
                    }
                }
            }
            row.error?.let { StatusBanner(it, isError = true) }
        }
    }
}
