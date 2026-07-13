package com.yangi.engvocab.feature.books

import com.yangi.engvocab.testing.FakeVocabularyAiService
import com.yangi.engvocab.testing.FakeVocabularyRepository
import com.yangi.engvocab.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BookDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun manualEntryRequiresEnglishAndMeaningBeforeSave() = runTest {
        val vm = BookDetailViewModel(FakeVocabularyRepository(), FakeVocabularyAiService())
        vm.openNewEntry(bookId = 7)

        vm.saveEntry()
        assertEquals("영어 표현을 입력하세요.", vm.state.value.editorError)

        vm.updateExpression("apple")
        vm.saveEntry()
        assertEquals("뜻을 입력하거나 AI로 채우세요.", vm.state.value.editorError)
    }

    @Test
    fun aiMeaningFillsButDoesNotAutoSave() = runTest {
        val repository = FakeVocabularyRepository()
        val ai = FakeVocabularyAiService(meaning = "기대하다")
        val vm = BookDetailViewModel(repository, ai)
        vm.openNewEntry(7)
        vm.updateExpression("look forward to")

        vm.fillMeaningWithAi()
        advanceUntilIdle()

        assertEquals("기대하다", vm.state.value.editorMeaning)
        assertTrue(repository.savedWords.isEmpty())
    }

    @Test
    fun savesAndReportsDuplicateWithFixedMessage() = runTest {
        val repository = FakeVocabularyRepository()
        val vm = BookDetailViewModel(repository, FakeVocabularyAiService())
        vm.openNewEntry(7)
        vm.updateExpression("Apple")
        vm.updateMeaning("사과")
        vm.saveEntry()
        advanceUntilIdle()
        assertFalse(vm.state.value.editorOpen)

        vm.openNewEntry(7)
        vm.updateExpression(" apple ")
        vm.updateMeaning("사과")
        vm.saveEntry()
        advanceUntilIdle()

        assertEquals("같은 단어장에 이미 있는 표현입니다.", vm.state.value.editorError)
    }
}
