package com.yangi.engvocab.testing

import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.repository.DuplicateExpressionException
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.repository.WordFilter
import com.yangi.engvocab.core.text.normalizeExpression
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

data class RecordedReview(
    val wordId: Long,
    val mode: StudyMode,
    val answer: String?,
    val finalResult: Boolean,
    val automaticResult: Boolean?,
    val overridden: Boolean,
)

class FakeVocabularyRepository : VocabularyRepository {
    private val booksFlow = MutableStateFlow<List<VocabularyBook>>(emptyList())
    private val wordsFlow = MutableStateFlow<List<WordEntry>>(emptyList())
    private var nextBookId = 1L
    private var nextWordId = 1L

    val savedWords = mutableListOf<NewWord>()
    val imported = mutableListOf<NewWord>()
    val recordedReviews = mutableListOf<RecordedReview>()

    init {
        addBookDirect(7, "테스트")
    }

    override fun books(): Flow<List<VocabularyBook>> = booksFlow
    override suspend fun book(id: Long): VocabularyBook? = booksFlow.value.find { it.id == id }

    override suspend fun createBook(name: String): Long {
        val id = nextBookId++
        addBookDirect(id, name)
        return id
    }

    override suspend fun renameBook(id: Long, name: String) {
        booksFlow.value = booksFlow.value.map { if (it.id == id) it.copy(name = name) else it }
    }

    override suspend fun deleteBook(id: Long) {
        booksFlow.value = booksFlow.value.filterNot { it.id == id }
        wordsFlow.value = wordsFlow.value.filterNot { it.bookId == id }
    }

    override fun words(bookId: Long, filter: WordFilter, query: String): Flow<List<WordEntry>> =
        wordsFlow.map { all ->
            all.filter { it.bookId == bookId }
                .filter { query.isBlank() || it.expression.contains(query, true) || it.meaning.orEmpty().contains(query, true) }
                .filter { filter != WordFilter.IMPORTANT || it.isImportant }
                .filter { filter != WordFilter.WRONG }
        }

    override suspend fun word(id: Long): WordEntry? = wordsFlow.value.find { it.id == id }

    override suspend fun addWord(bookId: Long, word: NewWord): Long {
        ensureUnique(bookId, word.expression)
        val id = nextWordId++
        val now = Instant.EPOCH
        wordsFlow.value += WordEntry(
            id, bookId, word.expression.trim(), word.meaning, word.isImportant, word.sourceType, now, now,
        )
        savedWords += word
        return id
    }

    override suspend fun updateWord(word: WordEntry) {
        ensureUnique(word.bookId, word.expression, word.id)
        wordsFlow.value = wordsFlow.value.map { if (it.id == word.id) word else it }
    }

    override suspend fun deleteWord(id: Long) {
        wordsFlow.value = wordsFlow.value.filterNot { it.id == id }
    }

    override suspend fun importWords(bookId: Long, words: List<NewWord>): List<Long> {
        val ids = words.map { addWord(bookId, it) }
        imported += words
        return ids
    }

    override fun dueWords(today: LocalDate, bookId: Long?): Flow<List<StudyWord>> = MutableStateFlow(emptyList())
    override fun unstudiedCount(): Flow<Int> = MutableStateFlow(wordsFlow.value.size)

    override suspend fun recordReview(
        wordId: Long,
        mode: StudyMode,
        submittedAnswer: String?,
        automaticResult: Boolean?,
        finalResult: Boolean,
        wasOverridden: Boolean,
        reviewedAt: Instant,
        localDate: LocalDate,
    ) {
        recordedReviews += RecordedReview(wordId, mode, submittedAnswer, finalResult, automaticResult, wasOverridden)
    }

    fun existingWords(): List<WordEntry> = wordsFlow.value

    fun addBookDirect(id: Long, name: String) {
        val now = Instant.EPOCH
        booksFlow.value += VocabularyBook(id, name, now, now)
        nextBookId = maxOf(nextBookId, id + 1)
    }

    private fun ensureUnique(bookId: Long, expression: String, exceptId: Long = 0) {
        val normalized = normalizeExpression(expression)
        if (wordsFlow.value.any { it.bookId == bookId && it.id != exceptId && normalizeExpression(it.expression) == normalized }) {
            throw DuplicateExpressionException(expression)
        }
    }
}
