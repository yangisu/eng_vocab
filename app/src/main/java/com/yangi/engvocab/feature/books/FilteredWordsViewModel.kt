package com.yangi.engvocab.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.repository.WordFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FilteredWordsUiState(
    val filter: WordFilter,
    val query: String = "",
    val words: List<WordEntry> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class FilteredWordsViewModel(
    private val repository: VocabularyRepository,
    filter: WordFilter,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val mutableState = MutableStateFlow(FilteredWordsUiState(filter = filter))
    val state: StateFlow<FilteredWordsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            query.flatMapLatest { repository.filteredWords(filter, it) }
                .collect { words -> mutableState.update { it.copy(words = words) } }
        }
    }

    fun updateQuery(value: String) {
        query.value = value
        mutableState.update { it.copy(query = value) }
    }

    fun toggleImportant(word: WordEntry) {
        viewModelScope.launch {
            repository.updateWord(word.copy(isImportant = !word.isImportant))
        }
    }

    class Factory(
        private val repository: VocabularyRepository,
        private val filter: WordFilter,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FilteredWordsViewModel(repository, filter) as T
    }
}
