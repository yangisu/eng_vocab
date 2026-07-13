package com.yangi.engvocab.feature.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun ReviewRoute(
    viewModel: ReviewViewModel,
    onStartAll: () -> Unit,
    onStartBook: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ReviewScreen(state, onStartAll, onStartBook, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    state: ReviewUiState,
    onStartAll: () -> Unit,
    onStartBook: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, topBar = { TopAppBar(title = { Text("복습") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("오늘 복습 ${state.total}개")
            Button(
                onClick = onStartAll,
                enabled = state.total > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("전체 복습 시작") }
            state.message?.let { Text(it) }
            state.groups.forEach { group ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${group.bookName} · ${group.count}개")
                        OutlinedButton(onClick = { onStartBook(group.bookId) }) { Text("이 단어장 복습") }
                    }
                }
            }
        }
    }
}
