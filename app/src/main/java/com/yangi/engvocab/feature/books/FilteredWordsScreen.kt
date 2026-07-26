package com.yangi.engvocab.feature.books

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

@Composable
fun FilteredWordsScreen(
    state: FilteredWordsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleImportant: (WordEntry) -> Unit,
    onStudy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val important = state.filter == WordFilter.IMPORTANT
    val title = if (important) "중요 단어" else "틀린 단어"
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(title = title, onBack = onBack) },
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
                    icon = if (important) Icons.Rounded.StarBorder else Icons.Rounded.WarningAmber,
                    title = if (important) "중요 표시한 단어가 없어요" else "틀린 단어가 없어요",
                    body = if (important) "별표를 누른 단어가 여기에 모입니다." else "학습 중 틀린 단어가 여기에 모입니다.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
                ) {
                    items(state.words, key = { it.id }) { word ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        word.expression,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        word.meaning.orEmpty(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconActionButton(
                                    icon = if (word.isImportant) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = if (word.isImportant) "중요 해제" else "중요 표시",
                                    onClick = { onToggleImportant(word) },
                                    tint = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
