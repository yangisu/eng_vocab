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
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.repository.WordFilter

@Composable
fun FilteredWordsRoute(
    viewModel: FilteredWordsViewModel,
    onBack: () -> Unit,
    onStudy: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FilteredWordsScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::updateQuery,
        onToggleImportant = viewModel::toggleImportant,
        onStudy = { onStudy(state.words.map { it.id }) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredWordsScreen(
    state: FilteredWordsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleImportant: (WordEntry) -> Unit,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = if (state.filter == WordFilter.IMPORTANT) "중요 단어 모음" else "틀린 단어 모음"
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
            Button(
                onClick = onStudy,
                enabled = state.words.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("현재 목록 학습 · " + state.words.size + "개")
            }
            if (state.words.isEmpty()) {
                Text(if (state.filter == WordFilter.IMPORTANT) "중요 표시한 단어가 없습니다." else "틀린 단어가 없습니다.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.words, key = { it.id }) { word ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(word.expression)
                                    OutlinedButton(onClick = { onToggleImportant(word) }) {
                                        Text(if (word.isImportant) "★" else "☆")
                                    }
                                }
                                Text(word.meaning.orEmpty())
                            }
                        }
                    }
                }
            }
        }
    }
}
