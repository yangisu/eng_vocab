package com.yangi.engvocab.feature.books

import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.repository.WordFilter
import com.yangi.engvocab.testing.FakeVocabularyRepository
import com.yangi.engvocab.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilteredWordsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun importantCollectionCombinesBooksAndSupportsSearch() = runTest {
        val repository = FakeVocabularyRepository()
        repository.addWord(7, NewWord("alpha", "알파", isImportant = true))
        repository.addBookDirect(8, "둘째")
        repository.addWord(8, NewWord("beta", "베타", isImportant = true))
        repository.addWord(8, NewWord("ordinary", "보통"))
        val vm = FilteredWordsViewModel(repository, WordFilter.IMPORTANT)

        advanceUntilIdle()
        assertEquals(setOf("alpha", "beta"), vm.state.value.words.map { it.expression }.toSet())

        vm.updateQuery("alp")
        advanceUntilIdle()

        assertEquals(listOf("alpha"), vm.state.value.words.map { it.expression })
    }
}
