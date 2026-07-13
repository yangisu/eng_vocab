package com.yangi.engvocab.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudySummaryScreen(
    summary: StudySummary,
    onRetryWrong: (List<Long>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("학습 완료")
        Text("정답 ${summary.correctCount}개 · 오답 ${summary.wrongCount}개")
        if (summary.wrongWords.isNotEmpty()) {
            Text("틀린 단어")
            summary.wrongWords.forEach { Text("${it.word.expression} · ${it.word.meaning.orEmpty()}") }
            OutlinedButton(
                onClick = { onRetryWrong(summary.wrongWords.map { it.word.id }) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("틀린 단어 다시 학습") }
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("완료") }
    }
}
