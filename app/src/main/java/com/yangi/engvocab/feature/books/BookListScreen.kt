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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    state: BookListUiState,
    onOpenBook: (Long) -> Unit,
    onCreate: () -> Unit,
    onRename: (com.yangi.engvocab.core.model.VocabularyBook) -> Unit,
    onDelete: (Long) -> Unit,
    onNameChange: (String) -> Unit,
    onSaveDialog: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("내 단어장") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("새 단어장") }
            if (state.books.isEmpty()) {
                Text("단어장을 만들어 시작하세요.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.books, key = { it.id }) { book ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onOpenBook(book.id) },
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(book.name)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onRename(book) }) { Text("이름 변경") }
                                    OutlinedButton(onClick = { pendingDelete = book.id }) { Text("삭제") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.dialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissDialog,
            title = { Text(if (state.editingBookId == null) "새 단어장" else "단어장 이름 변경") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state.nameInput,
                        onValueChange = onNameChange,
                        label = { Text("단어장 이름") },
                        singleLine = true,
                    )
                    state.error?.let { Text(it) }
                }
            },
            confirmButton = { Button(onClick = onSaveDialog) { Text("저장") } },
            dismissButton = { TextButton(onClick = onDismissDialog) { Text("취소") } },
        )
    }

    pendingDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("단어장 삭제") },
            text = { Text("단어와 학습 기록이 모두 삭제됩니다.") },
            confirmButton = {
                Button(onClick = { onDelete(id); pendingDelete = null }) { Text("삭제") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("취소") } },
        )
    }
}
