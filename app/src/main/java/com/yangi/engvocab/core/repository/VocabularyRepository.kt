package com.yangi.engvocab.core.repository

import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.model.WordEntry
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

enum class WordFilter { ALL, IMPORTANT, WRONG }

class DuplicateExpressionException(expression: String) :
    IllegalArgumentException("이미 같은 단어장에 있는 표현입니다: $expression")

interface VocabularyRepository {
    fun books(): Flow<List<VocabularyBook>>
    suspend fun book(id: Long): VocabularyBook?
    suspend fun createBook(name: String): Long
    suspend fun renameBook(id: Long, name: String)
    suspend fun deleteBook(id: Long)

    fun words(bookId: Long, filter: WordFilter, query: String = ""): Flow<List<WordEntry>>
    fun filteredWords(filter: WordFilter, query: String = ""): Flow<List<WordEntry>>
    suspend fun word(id: Long): WordEntry?
    suspend fun addWord(bookId: Long, word: NewWord): Long
    suspend fun updateWord(word: WordEntry)
    suspend fun deleteWord(id: Long)
    suspend fun importWords(bookId: Long, words: List<NewWord>): List<Long>

    fun dueWords(today: LocalDate, bookId: Long? = null): Flow<List<StudyWord>>
    fun unstudiedCount(): Flow<Int>

    suspend fun recordReview(
        wordId: Long,
        mode: StudyMode,
        submittedAnswer: String?,
        automaticResult: Boolean?,
        finalResult: Boolean,
        wasOverridden: Boolean,
        reviewedAt: Instant,
        localDate: LocalDate,
    )
}
