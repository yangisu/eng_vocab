package com.yangi.engvocab.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.repository.WordFilter
import com.yangi.engvocab.ui.components.AppTopBar
import com.yangi.engvocab.ui.components.EmptyState
import com.yangi.engvocab.ui.components.IconActionButton

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

@Composable
fun BookDetailScreen(
    state: BookDetailUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onFilter: (WordFilter) -> Unit,
    onToggleImportant: (WordEntry) -> Unit,
    onEdit: (WordEntry) -> Unit,
    onDelete: (Long) -> Unit,
    onAdd: () -> Unit,
    onPhotoAdd: () -> Unit,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = state.book?.name ?: "단어장", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("단어 검색") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.filter == WordFilter.ALL,
                        onClick = { onFilter(WordFilter.ALL) },
                        label = { Text("전체") },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == WordFilter.IMPORTANT,
                        onClick = { onFilter(WordFilter.IMPORTANT) },
                        label = { Text("중요") },
                        leadingIcon = { Icon(Icons.Rounded.Star, contentDescription = null) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter == WordFilter.WRONG,
                        onClick = { onFilter(WordFilter.WRONG) },
                        label = { Text("틀린 단어") },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("직접 추가", Modifier.padding(start = 6.dp))
                }
                OutlinedButton(onClick = onPhotoAdd, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                    Text("사진 추가", Modifier.padding(start = 6.dp))
                }
            }
            Button(
                onClick = onStudy,
                enabled = state.words.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Text("현재 목록 학습 · ${state.words.size}개", Modifier.padding(start = 8.dp))
            }
            if (state.words.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "표시할 단어가 없어요",
                    body = "검색어나 필터를 바꾸거나 새 단어를 추가해 보세요.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
                ) {
                    items(state.words, key = { it.id }) { word ->
                        WordEntryCard(
                            word = word,
                            onToggleImportant = { onToggleImportant(word) },
                            onEdit = { onEdit(word) },
                            onDelete = { onDelete(word.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordEntryCard(
    word: WordEntry,
    onToggleImportant: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    word.expression,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconActionButton(
                    icon = if (word.isImportant) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (word.isImportant) "중요 해제" else "중요 표시",
                    onClick = onToggleImportant,
                    tint = MaterialTheme.colorScheme.secondary,
                )
                IconActionButton(Icons.Rounded.Edit, "단어 수정", onEdit)
                IconActionButton(
                    Icons.Rounded.DeleteOutline,
                    "단어 삭제",
                    onDelete,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                word.meaning.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
