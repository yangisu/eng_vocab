package com.yangi.engvocab.feature.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.ui.components.ActionCard
import com.yangi.engvocab.ui.components.StatusBanner

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
        modifier = modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.School,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "어떻게 학습할까요?",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
        )
        Text(
            "지금 집중하기 좋은 방식을 선택하세요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        ActionCard(
            icon = Icons.Rounded.Flip,
            title = "카드 뒤집기",
            subtitle = "영어를 보고 뜻을 떠올린 뒤 직접 채점해요.",
            onClick = { onSelect(StudyMode.SELF_GRADED) },
            modifier = Modifier.fillMaxWidth(),
        )
        ActionCard(
            icon = Icons.Rounded.EditNote,
            title = "정답 입력",
            subtitle = "뜻을 보고 영어 표현을 직접 입력해요.",
            onClick = { onSelect(StudyMode.TYPED) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            accentColor = MaterialTheme.colorScheme.secondary,
        )
        OutlinedButton(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
            Text("취소")
        }
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
    val progress = if (state.originalTotal > 0) {
        state.position.toFloat() / state.originalTotal
    } else {
        0f
    }
    Column(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (state.isRetry) "다시 확인" else "오늘의 학습",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "${state.position} / ${state.originalTotal}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.mode == StudyMode.SELF_GRADED) {
                    Text(
                        studyWord.word.expression,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                    if (state.answerRevealed) {
                        Text(
                            studyWord.word.meaning.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 22.dp),
                        )
                    } else {
                        Text(
                            "뜻을 떠올려 보세요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 14.dp),
                        )
                    }
                } else {
                    Text(
                        studyWord.word.meaning.orEmpty(),
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedTextField(
                        value = state.typedAnswer,
                        onValueChange = onTypedChange,
                        label = { Text("영어 답") },
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        singleLine = true,
                        enabled = !state.typedChecked,
                    )
                    if (state.typedChecked) {
                        StatusBanner(
                            message = if (state.finalResult == true) {
                                "정답이에요 · ${studyWord.word.expression}"
                            } else {
                                "정답 · ${studyWord.word.expression}"
                            },
                            modifier = Modifier.padding(top = 16.dp),
                            isError = state.finalResult != true,
                        )
                    }
                }
            }
        }
        state.error?.let { StatusBanner(it, isError = true) }
        if (state.mode == StudyMode.SELF_GRADED) {
            if (!state.answerRevealed) {
                Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null)
                    Text("정답 보기", Modifier.padding(start = 8.dp))
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onWrong,
                        enabled = !state.persisting,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) { Text("틀림") }
                    Button(
                        onClick = onCorrect,
                        enabled = !state.persisting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Text("알겠음")
                    }
                }
            }
        } else if (!state.typedChecked) {
            Button(onClick = onCheckTyped, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Check, contentDescription = null)
                Text("채점", Modifier.padding(start = 8.dp))
            }
        } else {
            if (state.automaticResult == false && !state.wasOverridden) {
                OutlinedButton(onClick = onOverride, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                    Text("정답으로 변경", Modifier.padding(start = 8.dp))
                }
            }
            Button(
                onClick = onNext,
                enabled = !state.persisting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("다음") }
        }
    }
}
