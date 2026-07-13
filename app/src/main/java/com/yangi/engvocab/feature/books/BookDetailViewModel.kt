package com.yangi.engvocab.feature.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.openai.VocabularyAiService
import com.yangi.engvocab.core.repository.DuplicateExpressionException
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.repository.WordFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val bookId: Long? = null,
    val book: VocabularyBook? = null,
    val words: List<WordEntry> = emptyList(),
    val query: String = "",
    val filter: WordFilter = WordFilter.ALL,
    val editorOpen: Boolean = false,
    val editingWordId: Long? = null,
    val editorExpression: String = "",
    val editorMeaning: String = "",
    val editorImportant: Boolean = false,
    val editorLoadingMeaning: Boolean = false,
    val editorError: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModel(
    private val repository: VocabularyRepository,
    private val aiService: VocabularyAiService,
    initialBookId: Long? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(BookDetailUiState(bookId = initialBookId))
    val state: StateFlow<BookDetailUiState> = mutableState.asStateFlow()
    private val selectedBookId = MutableStateFlow(initialBookId)
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(WordFilter.ALL)

    init {
        viewModelScope.launch {
            combine(selectedBookId.filterNotNull(), query, filter) { bookId, text, wordFilter ->
                Triple(bookId, text, wordFilter)
            }.flatMapLatest { (bookId, text, wordFilter) ->
                repository.words(bookId, wordFilter, text)
            }.collect { words -> mutableState.update { it.copy(words = words) } }
        }
        initialBookId?.let(::selectBook)
    }

    fun selectBook(bookId: Long) {
        selectedBookId.value = bookId
        mutableState.update { it.copy(bookId = bookId) }
        viewModelScope.launch {
            mutableState.update { it.copy(book = repository.book(bookId)) }
        }
    }

    fun updateQuery(value: String) {
        query.value = value
        mutableState.update { it.copy(query = value) }
    }

    fun selectFilter(value: WordFilter) {
        filter.value = value
        mutableState.update { it.copy(filter = value) }
    }

    fun openNewEntry(bookId: Long = requireNotNull(state.value.bookId)) {
        selectBook(bookId)
        mutableState.update {
            it.copy(
                editorOpen = true,
                editingWordId = null,
                editorExpression = "",
                editorMeaning = "",
                editorImportant = false,
                editorError = null,
            )
        }
    }

    fun openEditEntry(word: WordEntry) {
        mutableState.update {
            it.copy(
                editorOpen = true,
                editingWordId = word.id,
                editorExpression = word.expression,
                editorMeaning = word.meaning.orEmpty(),
                editorImportant = word.isImportant,
                editorError = null,
            )
        }
    }

    fun closeEditor() = mutableState.update { it.copy(editorOpen = false, editorError = null) }
    fun updateExpression(value: String) = mutableState.update { it.copy(editorExpression = value, editorError = null) }
    fun updateMeaning(value: String) = mutableState.update { it.copy(editorMeaning = value, editorError = null) }
    fun updateImportant(value: Boolean) = mutableState.update { it.copy(editorImportant = value) }

    fun fillMeaningWithAi() {
        val expression = state.value.editorExpression.trim()
        if (!LATIN.containsMatchIn(expression)) {
            mutableState.update { it.copy(editorError = "영어 표현을 입력하세요.") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(editorLoadingMeaning = true, editorError = null) }
            runCatching { aiService.suggestMeaning(expression) }
                .onSuccess { meaning ->
                    mutableState.update { it.copy(editorMeaning = meaning, editorLoadingMeaning = false) }
                }
                .onFailure {
                    mutableState.update { it.copy(editorLoadingMeaning = false, editorError = "AI 뜻을 불러오지 못했습니다.") }
                }
        }
    }

    fun saveEntry() {
        val current = state.value
        val expression = current.editorExpression.trim()
        val meaning = current.editorMeaning.trim()
        if (!LATIN.containsMatchIn(expression)) {
            mutableState.update { it.copy(editorError = "영어 표현을 입력하세요.") }
            return
        }
        if (meaning.isEmpty()) {
            mutableState.update { it.copy(editorError = "뜻을 입력하거나 AI로 채우세요.") }
            return
        }
        val bookId = requireNotNull(current.bookId)
        viewModelScope.launch {
            runCatching {
                val editing = current.editingWordId?.let { repository.word(it) }
                if (editing == null) {
                    repository.addWord(
                        bookId,
                        NewWord(expression, meaning, current.editorImportant, SourceType.MANUAL),
                    )
                } else {
                    repository.updateWord(
                        editing.copy(expression = expression, meaning = meaning, isImportant = current.editorImportant),
                    )
                }
            }.onSuccess { mutableState.update { it.copy(editorOpen = false, editorError = null) } }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            editorError = if (error is DuplicateExpressionException) {
                                "같은 단어장에 이미 있는 표현입니다."
                            } else {
                                "단어를 저장하지 못했습니다."
                            },
                        )
                    }
                }
        }
    }

    fun toggleImportant(word: WordEntry) {
        viewModelScope.launch { repository.updateWord(word.copy(isImportant = !word.isImportant)) }
    }

    fun deleteWord(id: Long) {
        viewModelScope.launch { repository.deleteWord(id) }
    }

    class Factory(
        private val repository: VocabularyRepository,
        private val aiService: VocabularyAiService,
        private val bookId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            BookDetailViewModel(repository, aiService, bookId) as T
    }

    private companion object {
        val LATIN = Regex("[A-Za-z]")
    }
}
