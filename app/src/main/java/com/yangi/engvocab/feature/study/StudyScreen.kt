package com.yangi.engvocab.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangi.engvocab.core.model.StudyMode

@Composable
fun StudyRoute(
    viewModel: StudyViewModel,
    onRetryWrong: (List<Long>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.completed) {
        StudySummaryScreen(
            summary = requireNotNull(state.summary),
            onRetryWrong = onRetryWrong,
            onDone = onDone,
            modifier = modifier,
        )
    } else {
        StudyScreen(
            state = state,
            onReveal = viewModel::revealAnswer,
            onTypedChange = viewModel::updateTypedAnswer,
            onCheckTyped = viewModel::submitTyped,
            onOverride = viewModel::overrideAsCorrect,
            onCorrect = { viewModel.markCorrect(); viewModel.advance() },
            onWrong = { viewModel.markWrong(); viewModel.advance() },
            onNext = viewModel::advance,
            modifier = modifier,
        )
    }
}

@Composable
fun StudyModeSelectionScreen(
    onSelect: (StudyMode) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("학습 방식 선택")
        Button(onClick = { onSelect(StudyMode.SELF_GRADED) }, modifier = Modifier.fillMaxWidth()) {
            Text("영어 → 한국어 · 직접 채점")
        }
        Button(onClick = { onSelect(StudyMode.TYPED) }, modifier = Modifier.fillMaxWidth()) {
            Text("한국어 → 영어 · 타이핑")
        }
        OutlinedButton(onClick = onCancel) { Text("취소") }
    }
}

@Composable
fun StudyScreen(
    state: StudyUiState,
    onReveal: () -> Unit,
    onTypedChange: (String) -> Unit,
    onCheckTyped: () -> Unit,
    onOverride: () -> Unit,
    onCorrect: () -> Unit,
    onWrong: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val studyWord = requireNotNull(state.current)
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("${state.position} / ${state.originalTotal}${if (state.isRetry) " · 다시 확인" else ""}")
        Card(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.mode == StudyMode.SELF_GRADED) {
                    Text(studyWord.word.expression)
                    if (state.answerRevealed) Text(studyWord.word.meaning.orEmpty())
                } else {
                    Text(studyWord.word.meaning.orEmpty())
                    OutlinedTextField(
                        value = state.typedAnswer,
                        onValueChange = onTypedChange,
                        label = { Text("영어 답") },
                        singleLine = true,
                        enabled = !state.typedChecked,
                    )
                    if (state.typedChecked) {
                        Text("정답: ${studyWord.word.expression}")
                        Text(if (state.finalResult == true) "정답" else "오답")
                    }
                }
            }
        }
        state.error?.let { Text(it) }
        if (state.mode == StudyMode.SELF_GRADED) {
            if (!state.answerRevealed) {
                Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) { Text("정답 보기") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onCorrect, enabled = !state.persisting) { Text("알겠음") }
                    OutlinedButton(onClick = onWrong, enabled = !state.persisting) { Text("틀림") }
                }
            }
        } else if (!state.typedChecked) {
            Button(onClick = onCheckTyped, modifier = Modifier.fillMaxWidth()) { Text("채점") }
        } else {
            if (state.automaticResult == false && !state.wasOverridden) {
                OutlinedButton(onClick = onOverride) { Text("정답으로 변경") }
            }
            Button(onClick = onNext, enabled = !state.persisting, modifier = Modifier.fillMaxWidth()) { Text("다음") }
        }
    }
}
