package com.yangi.engvocab.feature.study

import com.yangi.engvocab.core.model.ReviewSnapshot
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.model.WordEntry
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudySessionEngineTest {
    @Test
    fun wrongWordIsAppendedAtMostOnce() {
        val engine = StudySessionEngine(listOf(word(1), word(2)))

        engine.answerCurrent(false)
        assertEquals(listOf(1L, 2L, 1L), engine.queueIds())
        engine.advance()
        engine.answerCurrent(true)
        engine.advance()
        engine.answerCurrent(false)

        assertEquals(listOf(1L, 2L, 1L), engine.queueIds())
        assertTrue(engine.isRetry)
    }

    @Test
    fun summaryCountsOriginalEncountersAndUniqueWrongWords() {
        val engine = StudySessionEngine(listOf(word(1), word(2)))
        engine.answerCurrent(false); engine.advance()
        engine.answerCurrent(true); engine.advance()
        engine.answerCurrent(true); engine.advance()

        val summary = engine.summary()
        assertEquals(1, summary.correctCount)
        assertEquals(1, summary.wrongCount)
        assertEquals(listOf(1L), summary.wrongWords.map { it.word.id })
        assertFalse(engine.hasCurrent)
    }

    private fun word(id: Long): StudyWord {
        val now = Instant.EPOCH
        return StudyWord(
            word = WordEntry(
                id = id,
                bookId = 7,
                expression = "word$id",
                meaning = "뜻$id",
                isImportant = false,
                sourceType = SourceType.MANUAL,
                createdAt = now,
                updatedAt = now,
            ),
            review = ReviewSnapshot.new(),
        )
    }
}
