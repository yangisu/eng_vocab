package com.yangi.engvocab.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yangi.engvocab.core.database.entity.VocabularyBookEntity
import com.yangi.engvocab.core.database.entity.WordEntryEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabularyDao {
    @Query("SELECT * FROM vocabulary_books ORDER BY updatedAt DESC")
    fun observeBooks(): Flow<List<VocabularyBookEntity>>

    @Query("SELECT * FROM vocabulary_books WHERE id = :id")
    suspend fun book(id: Long): VocabularyBookEntity?

    @Insert
    suspend fun insertBook(book: VocabularyBookEntity): Long

    @Query("UPDATE vocabulary_books SET name = :name, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameBook(id: Long, name: String, updatedAt: Instant)

    @Delete
    suspend fun deleteBook(book: VocabularyBookEntity)

    @Query(
        """
        SELECT * FROM words
        WHERE bookId = :bookId
          AND (:query = '' OR normalizedExpression LIKE '%' || :query || '%'
               OR COALESCE(meaning, '') LIKE '%' || :query || '%')
        ORDER BY createdAt DESC
        """,
    )
    fun observeWords(bookId: Long, query: String): Flow<List<WordEntryEntity>>

    @Query(
        """
        SELECT * FROM words
        WHERE bookId = :bookId AND isImportant = 1
          AND (:query = '' OR normalizedExpression LIKE '%' || :query || '%'
               OR COALESCE(meaning, '') LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
        """,
    )
    fun observeImportantWords(bookId: Long, query: String): Flow<List<WordEntryEntity>>

    @Query(
        """
        SELECT w.* FROM words w
        JOIN review_states r ON r.wordId = w.id
        WHERE w.bookId = :bookId AND r.totalWrong > 0
          AND (:query = '' OR w.normalizedExpression LIKE '%' || :query || '%'
               OR COALESCE(w.meaning, '') LIKE '%' || :query || '%')
        ORDER BY (
            SELECT MAX(l.reviewedAt) FROM review_logs l
            WHERE l.wordId = w.id AND l.finalResult = 0
        ) DESC
        """,
    )
    fun observeWrongWords(bookId: Long, query: String): Flow<List<WordEntryEntity>>

    @Query("SELECT * FROM words WHERE id = :id")
    suspend fun word(id: Long): WordEntryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM words WHERE bookId = :bookId AND normalizedExpression = :normalized AND id != :exceptId)")
    suspend fun expressionExists(bookId: Long, normalized: String, exceptId: Long = 0): Boolean

    @Insert
    suspend fun insertWord(word: WordEntryEntity): Long

    @Update
    suspend fun updateWord(word: WordEntryEntity)

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteWord(id: Long)

    @Query(
        """
        SELECT w.* FROM words w
        JOIN review_states r ON r.wordId = w.id
        WHERE (:bookId IS NULL OR w.bookId = :bookId)
          AND r.lastReviewedAt IS NOT NULL
          AND r.nextReviewDate <= :today
        ORDER BY r.nextReviewDate ASC
        """,
    )
    fun observeDueWords(today: LocalDate, bookId: Long?): Flow<List<WordEntryEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM words w
        LEFT JOIN review_states r ON r.wordId = w.id
        WHERE r.lastReviewedAt IS NULL
        """,
    )
    fun observeUnstudiedCount(): Flow<Int>
}
