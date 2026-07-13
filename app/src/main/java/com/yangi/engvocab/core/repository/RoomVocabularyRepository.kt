package com.yangi.engvocab.core.repository

import androidx.room.withTransaction
import com.yangi.engvocab.core.database.AppDatabase
import com.yangi.engvocab.core.database.entity.ReviewLogEntity
import com.yangi.engvocab.core.database.entity.ReviewStateEntity
import com.yangi.engvocab.core.database.entity.VocabularyBookEntity
import com.yangi.engvocab.core.database.entity.WordEntryEntity
import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.ReviewResult
import com.yangi.engvocab.core.model.ReviewSnapshot
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.review.ReviewScheduler
import com.yangi.engvocab.core.text.normalizeExpression
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomVocabularyRepository(
    private val db: AppDatabase,
) : VocabularyRepository {
    private val vocabularyDao = db.vocabularyDao()
    private val reviewDao = db.reviewDao()

    override fun books(): Flow<List<VocabularyBook>> =
        vocabularyDao.observeBooks().map { books -> books.map(VocabularyBookEntity::toDomain) }

    override suspend fun book(id: Long): VocabularyBook? = vocabularyDao.book(id)?.toDomain()

    override suspend fun createBook(name: String): Long {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "단어장 이름을 입력하세요." }
        val now = Instant.now()
        return vocabularyDao.insertBook(VocabularyBookEntity(name = cleanName, createdAt = now, updatedAt = now))
    }

    override suspend fun renameBook(id: Long, name: String) {
        val cleanName = name.trim()
        require(cleanName.isNotEmpty()) { "단어장 이름을 입력하세요." }
        vocabularyDao.renameBook(id, cleanName, Instant.now())
    }

    override suspend fun deleteBook(id: Long) {
        vocabularyDao.book(id)?.let { vocabularyDao.deleteBook(it) }
    }

    override fun words(bookId: Long, filter: WordFilter, query: String): Flow<List<WordEntry>> {
        val cleanQuery = normalizeExpression(query)
        val entities = when (filter) {
            WordFilter.ALL -> vocabularyDao.observeWords(bookId, cleanQuery)
            WordFilter.IMPORTANT -> vocabularyDao.observeImportantWords(bookId, cleanQuery)
            WordFilter.WRONG -> vocabularyDao.observeWrongWords(bookId, cleanQuery)
        }
        return entities.map { values -> values.map(WordEntryEntity::toDomain) }
    }

    override suspend fun word(id: Long): WordEntry? = vocabularyDao.word(id)?.toDomain()
    override fun filteredWords(filter: WordFilter, query: String): Flow<List<WordEntry>> {
        val cleanQuery = normalizeExpression(query)
        val entities = when (filter) {
            WordFilter.ALL -> vocabularyDao.observeAllWords(cleanQuery)
            WordFilter.IMPORTANT -> vocabularyDao.observeAllImportantWords(cleanQuery)
            WordFilter.WRONG -> vocabularyDao.observeAllWrongWords(cleanQuery)
        }
        return entities.map { values -> values.map(WordEntryEntity::toDomain) }
    }


    override suspend fun addWord(bookId: Long, word: NewWord): Long = db.withTransaction {
        insertChecked(bookId, word, Instant.now())
    }

    override suspend fun updateWord(word: WordEntry) = db.withTransaction {
        val expression = word.expression.trim()
        require(expression.isNotEmpty()) { "영어 단어나 문구를 입력하세요." }
        val normalized = normalizeExpression(expression)
        if (vocabularyDao.expressionExists(word.bookId, normalized, word.id)) {
            throw DuplicateExpressionException(expression)
        }
        vocabularyDao.updateWord(
            WordEntryEntity(
                id = word.id,
                bookId = word.bookId,
                expression = expression,
                normalizedExpression = normalized,
                meaning = word.meaning?.trim()?.takeIf(String::isNotEmpty),
                isImportant = word.isImportant,
                sourceType = word.sourceType,
                createdAt = word.createdAt,
                updatedAt = Instant.now(),
            ),
        )
    }

    override suspend fun deleteWord(id: Long) {
        vocabularyDao.deleteWord(id)
    }

    override suspend fun importWords(bookId: Long, words: List<NewWord>): List<Long> {
        require(words.size <= 200) { "한 번에 최대 200개까지 가져올 수 있습니다." }
        return db.withTransaction {
            val now = Instant.now()
            words.map { insertChecked(bookId, it, now) }
        }
    }

    override fun dueWords(today: LocalDate, bookId: Long?): Flow<List<StudyWord>> =
        vocabularyDao.observeDueWords(today, bookId).map { words ->
            if (words.isEmpty()) return@map emptyList()
            val states = reviewDao.states(words.map { it.id }).associateBy { it.wordId }
            words.mapNotNull { word ->
                states[word.id]?.let { StudyWord(word.toDomain(), it.toSnapshot()) }
            }
        }

    override fun unstudiedCount(): Flow<Int> = vocabularyDao.observeUnstudiedCount()

    override suspend fun recordReview(
        wordId: Long,
        mode: StudyMode,
        submittedAnswer: String?,
        automaticResult: Boolean?,
        finalResult: Boolean,
        wasOverridden: Boolean,
        reviewedAt: Instant,
        localDate: LocalDate,
    ) = db.withTransaction {
        requireNotNull(vocabularyDao.word(wordId)) { "존재하지 않는 단어입니다." }
        val existing = reviewDao.state(wordId)
        val current = existing?.toSnapshot() ?: ReviewSnapshot.new()
        val result = if (finalResult) ReviewResult.CORRECT else ReviewResult.WRONG
        val updated = ReviewScheduler.answer(current, result, localDate)
        reviewDao.upsertState(updated.toEntity(wordId, reviewedAt))
        reviewDao.insertLog(
            ReviewLogEntity(
                wordId = wordId,
                mode = mode,
                submittedAnswer = submittedAnswer,
                automaticResult = automaticResult,
                finalResult = finalResult,
                wasOverridden = wasOverridden,
                reviewedAt = reviewedAt,
            ),
        )
        Unit
    }

    private suspend fun insertChecked(bookId: Long, word: NewWord, now: Instant): Long {
        requireNotNull(vocabularyDao.book(bookId)) { "존재하지 않는 단어장입니다." }
        val expression = word.expression.trim()
        require(expression.isNotEmpty()) { "영어 단어나 문구를 입력하세요." }
        val normalized = normalizeExpression(expression)
        if (vocabularyDao.expressionExists(bookId, normalized)) {
            throw DuplicateExpressionException(expression)
        }
        return vocabularyDao.insertWord(
            WordEntryEntity(
                bookId = bookId,
                expression = expression,
                normalizedExpression = normalized,
                meaning = word.meaning?.trim()?.takeIf(String::isNotEmpty),
                isImportant = word.isImportant,
                sourceType = word.sourceType,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }
}

private fun VocabularyBookEntity.toDomain() = VocabularyBook(id, name, createdAt, updatedAt)

private fun WordEntryEntity.toDomain() = WordEntry(
    id = id,
    bookId = bookId,
    expression = expression,
    meaning = meaning,
    isImportant = isImportant,
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun ReviewStateEntity.toSnapshot() = ReviewSnapshot(
    repetition = repetition,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    nextReviewDate = nextReviewDate,
    correctStreak = correctStreak,
    totalCorrect = totalCorrect,
    totalWrong = totalWrong,
    lastResult = lastResult,
)

private fun ReviewSnapshot.toEntity(wordId: Long, reviewedAt: Instant) = ReviewStateEntity(
    wordId = wordId,
    repetition = repetition,
    intervalDays = intervalDays,
    easeFactor = easeFactor,
    nextReviewDate = nextReviewDate,
    correctStreak = correctStreak,
    totalCorrect = totalCorrect,
    totalWrong = totalWrong,
    lastResult = lastResult,
    lastReviewedAt = reviewedAt,
)
