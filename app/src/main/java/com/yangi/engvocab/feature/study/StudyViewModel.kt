package com.yangi.engvocab.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.study.gradeTypedAnswer
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyUiState(
    val mode: StudyMode,
    val current: StudyWord?,
    val originalTotal: Int,
    val position: Int,
    val isRetry: Boolean = false,
    val answerRevealed: Boolean = false,
    val typedAnswer: String = "",
    val typedChecked: Boolean = false,
    val automaticResult: Boolean? = null,
    val finalResult: Boolean? = null,
    val wasOverridden: Boolean = false,
    val persisting: Boolean = false,
    val completed: Boolean = false,
    val summary: StudySummary? = null,
    val error: String? = null,
)

class StudyViewModel(
    private val repository: VocabularyRepository,
    private val mode: StudyMode,
    private val engine: StudySessionEngine,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(
        StudyUiState(
            mode = mode,
            current = if (engine.hasCurrent) engine.current else null,
            originalTotal = engine.originalTotal,
            position = if (engine.hasCurrent) engine.displayPosition else 0,
            completed = !engine.hasCurrent,
            summary = if (!engine.hasCurrent) engine.summary() else null,
        ),
    )
    val state: StateFlow<StudyUiState> = mutableState.asStateFlow()

    fun revealAnswer() = mutableState.update { it.copy(answerRevealed = true) }

    fun updateTypedAnswer(value: String) = mutableState.update {
        if (it.typedChecked) it else it.copy(typedAnswer = value, error = null)
    }

    fun submitTyped(answer: String = state.value.typedAnswer) {
        val expected = state.value.current?.word?.expression ?: return
        if (answer.isBlank()) {
            mutableState.update { it.copy(error = "답을 입력하세요.") }
            return
        }
        val automatic = gradeTypedAnswer(expected, answer)
        mutableState.update {
            it.copy(
                typedAnswer = answer,
                typedChecked = true,
                automaticResult = automatic,
                finalResult = automatic,
                wasOverridden = false,
                error = null,
            )
        }
    }

    fun overrideAsCorrect() {
        val current = state.value
        if (current.typedChecked && current.automaticResult == false) {
            mutableState.update { it.copy(finalResult = true, wasOverridden = true) }
        }
    }

    fun markCorrect() = mutableState.update { it.copy(finalResult = true) }
    fun markWrong() = mutableState.update { it.copy(finalResult = false) }

    fun advance() {
        val currentState = state.value
        val studyWord = currentState.current ?: return
        val result = currentState.finalResult ?: return
        if (currentState.persisting) return
        mutableState.update { it.copy(persisting = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val reviewedAt = clock.instant()
                repository.recordReview(
                    wordId = studyWord.word.id,
                    mode = mode,
                    submittedAnswer = currentState.typedAnswer.takeIf { mode == StudyMode.TYPED },
                    automaticResult = currentState.automaticResult,
                    finalResult = result,
                    wasOverridden = currentState.wasOverridden,
                    reviewedAt = reviewedAt,
                    localDate = LocalDate.ofInstant(reviewedAt, clock.zone),
                )
            }.onSuccess {
                engine.answerCurrent(result)
                engine.advance()
                if (engine.hasCurrent) {
                    mutableState.value = StudyUiState(
                        mode = mode,
                        current = engine.current,
                        originalTotal = engine.originalTotal,
                        position = engine.displayPosition,
                        isRetry = engine.isRetry,
                    )
                } else {
                    mutableState.value = StudyUiState(
                        mode = mode,
                        current = null,
                        originalTotal = engine.originalTotal,
                        position = engine.originalTotal,
                        completed = true,
                        summary = engine.summary(),
                    )
                }
            }.onFailure {
                mutableState.update { it.copy(persisting = false, error = "학습 결과를 저장하지 못했습니다.") }
            }
        }
    }

    class Factory(
        private val repository: VocabularyRepository,
        private val mode: StudyMode,
        private val words: List<StudyWord>,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            StudyViewModel(repository, mode, StudySessionEngine(words), clock) as T
    }
}
