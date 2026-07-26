package com.yangi.engvocab.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yangi.engvocab.ui.components.MetricCard
import com.yangi.engvocab.ui.components.SectionHeader

@Composable
fun StudySummaryScreen(
    summary: StudySummary,
    onRetryWrong: (List<Long>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Icon(
                Icons.Rounded.SentimentSatisfied,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "오늘 학습 완료!",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "꾸준한 한 번이 실력을 만들어요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetricCard(
                    icon = Icons.Rounded.CheckCircle,
                    label = "정답",
                    value = "${summary.correctCount}개",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                MetricCard(
                    icon = Icons.Rounded.WarningAmber,
                    label = "오답",
                    value = "${summary.wrongCount}개",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
        if (summary.wrongWords.isNotEmpty()) {
            item { SectionHeader("다시 볼 단어") }
            items(summary.wrongWords, key = { it.word.id }) { studyWord ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            studyWord.word.expression,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            studyWord.word.meaning.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { onRetryWrong(summary.wrongWords.map { it.word.id }) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("틀린 단어 다시 학습", Modifier.padding(start = 8.dp))
                }
            }
        }
        item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("완료")
            }
        }
    }
}
