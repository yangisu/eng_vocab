package com.yangi.engvocab.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.repository.VocabularyRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val loading: Boolean = true,
    val dueCount: Int = 0,
    val unstudiedCount: Int = 0,
    val recentBooks: List<VocabularyBook> = emptyList(),
)

class HomeViewModel(
    repository: VocabularyRepository,
    clock: Clock,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        repository.dueWords(LocalDate.now(clock)),
        repository.unstudiedCount(),
        repository.books(),
    ) { due, unstudied, books ->
        HomeUiState(
            loading = false,
            dueCount = due.size,
            unstudiedCount = unstudied,
            recentBooks = books.sortedByDescending { it.updatedAt }.take(5),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeUiState(),
    )

    class Factory(
        private val repository: VocabularyRepository,
        private val clock: Clock,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repository, clock) as T
    }
}
