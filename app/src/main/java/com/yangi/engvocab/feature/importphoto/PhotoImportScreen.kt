package com.yangi.engvocab.feature.importphoto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yangi.engvocab.core.image.TempImageStore

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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopAppBar(
                title = { Text("사진으로 단어장 만들기") },
                navigationIcon = { OutlinedButton(onClick = onCancel) { Text("닫기") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            when (state.phase) {
                ImportPhase.DESTINATION -> {
                    Text("저장할 단어장을 선택하세요.")
                    state.books.forEach { book ->
                        Button(onClick = { onSelectBook(book.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(book.name)
                        }
                    }
                    if (state.books.isEmpty()) Text("먼저 단어장을 만들어 주세요.")
                }

                ImportPhase.SOURCE -> {
                    Text("사진 가져오기")
                    Button(onClick = onOpenCamera, modifier = Modifier.fillMaxWidth()) { Text("카메라로 촬영") }
                    PhotoPickerButton(
                        tempImageStore = tempImageStore,
                        onSelected = onPhotoSelected,
                        onError = onPhotoError,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ImportPhase.PREVIEW -> {
                    state.imagePath?.let { path ->
                        AsyncImage(
                            model = path.toFile(),
                            contentDescription = "분석할 단어 사진",
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                    Button(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) { Text("AI로 분석") }
                }

                ImportPhase.ANALYZING, ImportPhase.SAVING -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Text(if (state.phase == ImportPhase.ANALYZING) "사진 분석 중…" else "단어 저장 중…")
                    }
                }

                ImportPhase.REVIEW -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${state.rows.size}개 항목 검토")
                        OutlinedButton(onClick = onAddRow) { Text("항목 추가") }
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
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("단어장에 저장") }
                }

                ImportPhase.COMPLETE -> {
                    Text("${state.savedWordIds.size}개 단어를 저장했습니다.")
                    Button(onClick = onStudySaved, enabled = state.savedWordIds.isNotEmpty()) { Text("지금 학습하기") }
                    OutlinedButton(onClick = onOpenBook) { Text("단어장으로 이동") }
                }
            }
        }
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
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (row.isLowConfidence) Text("확인 필요")
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
                Text("중복: ${row.duplicateExpression} / ${row.duplicateMeaning.orEmpty()}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onDuplicateAction(DuplicateAction.SKIP) }) {
                        Text(if (row.duplicateAction == DuplicateAction.SKIP) "✓ 건너뛰기" else "건너뛰기")
                    }
                    OutlinedButton(onClick = { onDuplicateAction(DuplicateAction.EDIT) }) {
                        Text(if (row.duplicateAction == DuplicateAction.EDIT) "✓ 수정" else "수정")
                    }
                }
            }
            row.error?.let { Text(it) }
            OutlinedButton(onClick = onDelete) { Text("항목 삭제") }
        }
    }
}
