package com.yangi.engvocab.feature.home

import com.yangi.engvocab.core.model.ReviewSnapshot
import com.yangi.engvocab.core.model.SourceType
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
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun homeSeparatesDueFromUnstudied() = runTest {
        val repository = FakeVocabularyRepository().apply {
            due = listOf(studyWord(1), studyWord(2))
            unstudied = 5
        }
        val vm = HomeViewModel(
            repository,
            Clock.fixed(Instant.parse("2026-07-14T00:00:00Z"), ZoneOffset.UTC),
        )

        advanceUntilIdle()

        assertEquals(2, vm.state.value.dueCount)
        assertEquals(5, vm.state.value.unstudiedCount)
        assertEquals(false, vm.state.value.loading)
    }

    private fun studyWord(id: Long): StudyWord {
        val now = Instant.EPOCH
        return StudyWord(
            WordEntry(id, 7, "word$id", "뜻$id", false, SourceType.MANUAL, now, now),
            ReviewSnapshot.new(),
        )
    }
}
