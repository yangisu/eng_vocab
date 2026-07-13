package com.yangi.engvocab.feature.books

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangi.engvocab.core.repository.WordFilter

@Composable
fun BookDetailRoute(
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
    onPhotoAdd: (Long) -> Unit,
    onStudy: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BookDetailScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::updateQuery,
        onFilter = viewModel::selectFilter,
        onToggleImportant = viewModel::toggleImportant,
        onEdit = viewModel::openEditEntry,
        onDelete = viewModel::deleteWord,
        onAdd = { viewModel.openNewEntry() },
        onPhotoAdd = { state.bookId?.let(onPhotoAdd) },
        onStudy = { state.bookId?.let(onStudy) },
        modifier = modifier,
    )
    if (state.editorOpen) {
        EntryEditorScreen(
            state = state,
            onExpressionChange = viewModel::updateExpression,
            onMeaningChange = viewModel::updateMeaning,
            onImportantChange = viewModel::updateImportant,
            onFillMeaning = viewModel::fillMeaningWithAi,
            onSave = viewModel::saveEntry,
            onDismiss = viewModel::closeEditor,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilter: (WordFilter) -> Unit,
    onToggleImportant: (com.yangi.engvocab.core.model.WordEntry) -> Unit,
    onEdit: (com.yangi.engvocab.core.model.WordEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit,
    onPhotoAdd: () -> Unit,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.book?.name ?: "단어장") },
                navigationIcon = { OutlinedButton(onClick = onBack) { Text("뒤로") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("검색") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.filter == WordFilter.ALL, onClick = { onFilter(WordFilter.ALL) }, label = { Text("전체") })
                FilterChip(selected = state.filter == WordFilter.IMPORTANT, onClick = { onFilter(WordFilter.IMPORTANT) }, label = { Text("중요") })
                FilterChip(selected = state.filter == WordFilter.WRONG, onClick = { onFilter(WordFilter.WRONG) }, label = { Text("틀린 단어") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAdd) { Text("직접 추가") }
                OutlinedButton(onClick = onPhotoAdd) { Text("사진 추가") }
                OutlinedButton(onClick = onStudy, enabled = state.words.isNotEmpty()) { Text("현재 목록 학습") }
            }
            if (state.words.isEmpty()) {
                Text("표시할 단어가 없습니다.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.words, key = { it.id }) { word ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(word.expression)
                                    Button(onClick = { onToggleImportant(word) }) {
                                        Text(if (word.isImportant) "★ 중요 해제" else "☆ 중요")
                                    }
                                }
                                Text(word.meaning.orEmpty())
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onEdit(word) }) { Text("수정") }
                                    OutlinedButton(onClick = { onDelete(word.id) }) { Text("삭제") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
