package com.yangi.engvocab.feature.study

import com.yangi.engvocab.core.model.ReviewSnapshot
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.testing.FakeVocabularyRepository
import com.yangi.engvocab.testing.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StudyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun typedOverridePersistsAutomaticAndFinalResultsOnce() = runTest {
        val repository = FakeVocabularyRepository()
        val vm = StudyViewModel(
            repository = repository,
            mode = StudyMode.TYPED,
            engine = StudySessionEngine(listOf(word(1, "don't"))),
            clock = Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC),
        )

        vm.submitTyped("dont")
        vm.overrideAsCorrect()
        vm.advance()
        vm.advance()
        advanceUntilIdle()

        val saved = repository.recordedReviews.single()
        assertEquals(false, saved.automaticResult)
        assertEquals(true, saved.finalResult)
        assertTrue(saved.overridden)
    }

    @Test
    fun selfGradedWrongQueuesRetryAndPersists() = runTest {
        val repository = FakeVocabularyRepository()
        val vm = StudyViewModel(
            repository,
            StudyMode.SELF_GRADED,
            StudySessionEngine(listOf(word(1, "apple"))),
            Clock.systemUTC(),
        )
        vm.revealAnswer()
        vm.markWrong()
        vm.advance()
        advanceUntilIdle()
        assertEquals(1, repository.recordedReviews.size)
        assertTrue(vm.state.value.isRetry)
    }

    private fun word(id: Long, expression: String): StudyWord {
        val now = Instant.EPOCH
        return StudyWord(
            WordEntry(id, 7, expression, "뜻", false, SourceType.MANUAL, now, now),
            ReviewSnapshot.new(),
        )
    }
}
