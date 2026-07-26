package com.yangi.engvocab.feature.books

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.ui.components.AppTopBar
import com.yangi.engvocab.ui.components.EmptyState
import com.yangi.engvocab.ui.components.IconActionButton
import com.yangi.engvocab.ui.components.StatusBanner

@Composable
fun BookListRoute(
    viewModel: BookListViewModel,
    onOpenBook: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BookListScreen(
        state = state,
        onOpenBook = onOpenBook,
        onCreate = viewModel::openCreate,
        onRename = viewModel::openRename,
        onDelete = viewModel::deleteBook,
        onNameChange = viewModel::updateName,
        onSaveDialog = viewModel::saveBook,
        onDismissDialog = viewModel::dismissDialog,
        modifier = modifier,
    )
}

@Composable
fun BookListScreen(
    state: BookListUiState,
    onOpenBook: (Long) -> Unit,
    onCreate: () -> Unit,
    onRename: (VocabularyBook) -> Unit,
    onDelete: (Long) -> Unit,
    onNameChange: (String) -> Unit,
    onSaveDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = "내 단어장") },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("나만의 단어장을\n차곡차곡 모아보세요.", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("새 단어장", Modifier.padding(start = 8.dp))
                }
            }
            if (state.books.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.AutoStories,
                        title = "첫 단어장을 만들어 보세요",
                        body = "사진이나 직접 입력으로 단어를 채울 수 있어요.",
                    )
                }
            } else {
                items(state.books, key = { it.id }) { book ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenBook(book.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(book.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            IconActionButton(
                                icon = Icons.Rounded.Edit,
                                contentDescription = "단어장 이름 변경",
                                onClick = { onRename(book) },
                            )
                            IconActionButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = "단어장 삭제",
                                onClick = { pendingDelete = book.id },
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = "단어장 열기",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.dialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            icon = { Icon(if (state.editingBookId == null) Icons.Rounded.Add else Icons.Rounded.Edit, null) },
            title = { Text(if (state.editingBookId == null) "새 단어장" else "단어장 이름 변경") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.nameInput,
                        onValueChange = onNameChange,
                        label = { Text("단어장 이름") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                    )
                    state.error?.let { StatusBanner(it, isError = true) }
                }
            },
            confirmButton = { Button(onClick = onSaveDialog) { Text("저장") } },
            dismissButton = { TextButton(onClick = onDismissDialog) { Text("취소") } },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text("단어장 삭제") },
            text = { Text("단어와 학습 기록이 모두 삭제됩니다.") },
            confirmButton = {
                Button(
                    onClick = { onDelete(id); pendingDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }
}
