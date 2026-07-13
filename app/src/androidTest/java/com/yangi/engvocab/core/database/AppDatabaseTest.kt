package com.yangi.engvocab.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.repository.DuplicateExpressionException
import com.yangi.engvocab.core.repository.RoomVocabularyRepository
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.repository.WordFilter
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: VocabularyRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomVocabularyRepository(db)
    }

    @After
    fun close() = db.close()

    @Test
    fun duplicateInSameBookRollsBackWholeBatch() = runTest {
        val bookId = repository.createBook("시험")
        repository.addWord(bookId, NewWord("Apple", "사과", sourceType = SourceType.PHOTO))

        var duplicateThrown = false
        try {
            repository.importWords(
                bookId,
                listOf(
                    NewWord("banana", "바나나", sourceType = SourceType.PHOTO),
                    NewWord(" apple ", "사과", sourceType = SourceType.PHOTO),
                ),
            )
        } catch (_: DuplicateExpressionException) {
            duplicateThrown = true
        }

        assertEquals(true, duplicateThrown)
        assertEquals(listOf("Apple"), repository.words(bookId, WordFilter.ALL).first().map { it.expression })
    }

    @Test
    fun deletingBookCascadesWordsStatesAndLogs() = runTest {
        val bookId = repository.createBook("삭제")
        val wordId = repository.addWord(bookId, NewWord("test", "시험"))
        repository.recordReview(
            wordId = wordId,
            mode = StudyMode.SELF_GRADED,
            submittedAnswer = null,
            automaticResult = null,
            finalResult = false,
            wasOverridden = false,
            reviewedAt = Instant.EPOCH,
            localDate = LocalDate.of(2026, 7, 14),
        )

        repository.deleteBook(bookId)

        assertNull(repository.word(wordId))
        assertEquals(0, db.reviewDao().countLogs())
        assertEquals(0, db.reviewDao().countStates())
    }

    @Test
    fun importantWrongAndDueQueriesUseReviewState() = runTest {
        val bookId = repository.createBook("필터")
        val importantId = repository.addWord(bookId, NewWord("keep", "유지", isImportant = true))
        val wrongId = repository.addWord(bookId, NewWord("miss", "놓치다"))
        repository.recordReview(
            wrongId, StudyMode.TYPED, "mist", false, false, false,
            Instant.parse("2026-07-14T00:00:00Z"), LocalDate.of(2026, 7, 14),
        )

        assertEquals(listOf(importantId), repository.words(bookId, WordFilter.IMPORTANT).first().map { it.id })
        assertEquals(listOf(wrongId), repository.words(bookId, WordFilter.WRONG).first().map { it.id })
        assertEquals(listOf(wrongId), repository.dueWords(LocalDate.of(2026, 7, 15)).first().map { it.word.id })
    }
}
