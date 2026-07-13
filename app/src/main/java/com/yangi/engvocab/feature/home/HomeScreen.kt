package com.yangi.engvocab.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onStartDue: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onPhotoImport: () -> Unit,
    onBooks: () -> Unit,
    onImportant: () -> Unit,
    onWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state,
        onStartDue,
        onOpenBook,
        onPhotoImport,
        onBooks,
        onImportant,
        onWrong,
        modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onStartDue: () -> Unit,
    onOpenBook: (Long) -> Unit,
    onPhotoImport: () -> Unit,
    onBooks: () -> Unit,
    onImportant: () -> Unit,
    onWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("오늘의 학습") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("오늘 복습")
                        Text("${state.dueCount}개")
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("미학습")
                        Text("${state.unstudiedCount}개")
                    }
                }
            }
            Button(
                onClick = onStartDue,
                enabled = state.dueCount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("오늘 복습 시작") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPhotoImport) { Text("사진으로 만들기") }
                OutlinedButton(onClick = onBooks) { Text("직접 추가") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onImportant) { Text("중요 단어 모음") }
                OutlinedButton(onClick = onWrong) { Text("틀린 단어 모음") }
            }
            Text("최근 단어장")
            if (state.recentBooks.isEmpty()) {
                Text("아직 단어장이 없습니다.")
            } else {
                state.recentBooks.forEach { book ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenBook(book.id) }) {
                        Text(book.name, Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}
