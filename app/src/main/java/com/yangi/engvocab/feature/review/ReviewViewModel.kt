package com.yangi.engvocab.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.repository.VocabularyRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DueBookGroup(
    val bookId: Long,
    val bookName: String,
    val count: Int,
)

data class ReviewUiState(
    val loading: Boolean = true,
    val dueWords: List<StudyWord> = emptyList(),
    val groups: List<DueBookGroup> = emptyList(),
    val message: String? = null,
) {
    val total: Int get() = dueWords.size
}

class ReviewViewModel(
    repository: VocabularyRepository,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<ReviewUiState> = combine(
        repository.dueWords(LocalDate.now(clock)),
        repository.books(),
    ) { due, books ->
        val names = books.associate { it.id to it.name }
        val groups = due.groupBy { it.word.bookId }.map { (bookId, words) ->
            DueBookGroup(bookId, names[bookId] ?: "단어장", words.size)
        }.sortedBy { it.bookName }
        ReviewUiState(
            loading = false,
            dueWords = due,
            groups = groups,
            message = if (due.isEmpty()) "오늘 복습할 단어가 없습니다." else null,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        ReviewUiState(),
    )

    class Factory(
        private val repository: VocabularyRepository,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ReviewViewModel(repository, clock) as T
    }
}
