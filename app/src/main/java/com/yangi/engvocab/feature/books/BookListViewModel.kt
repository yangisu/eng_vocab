package com.yangi.engvocab.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.repository.VocabularyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookListUiState(
    val books: List<VocabularyBook> = emptyList(),
    val nameInput: String = "",
    val editingBookId: Long? = null,
    val dialogOpen: Boolean = false,
    val error: String? = null,
)

class BookListViewModel(private val repository: VocabularyRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(BookListUiState())
    val state: StateFlow<BookListUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.books().collect { books -> mutableState.update { it.copy(books = books) } }
        }
    }

    fun openCreate() = mutableState.update { it.copy(dialogOpen = true, editingBookId = null, nameInput = "", error = null) }

    fun openRename(book: VocabularyBook) = mutableState.update {
        it.copy(dialogOpen = true, editingBookId = book.id, nameInput = book.name, error = null)
    }

    fun updateName(value: String) = mutableState.update { it.copy(nameInput = value, error = null) }

    fun dismissDialog() = mutableState.update { it.copy(dialogOpen = false, error = null) }

    fun saveBook() {
        val name = state.value.nameInput.trim()
        if (name.isEmpty()) {
            mutableState.update { it.copy(error = "단어장 이름을 입력하세요.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                state.value.editingBookId?.let { repository.renameBook(it, name) } ?: repository.createBook(name)
            }.onSuccess { mutableState.update { it.copy(dialogOpen = false, error = null) } }
                .onFailure { mutableState.update { it.copy(error = "단어장을 저장하지 못했습니다.") } }
        }
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch { repository.deleteBook(id) }
    }

    class Factory(private val repository: VocabularyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = BookListViewModel(repository) as T
    }
}
