package com.yangi.engvocab.feature.importphoto

import androidx.lifecycle.SavedStateHandle
import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.openai.AnalyzedEntry
import com.yangi.engvocab.core.openai.Confidence
import com.yangi.engvocab.core.openai.ImageInput
import com.yangi.engvocab.testing.FakeVocabularyAiService
import com.yangi.engvocab.testing.FakeVocabularyRepository
import com.yangi.engvocab.testing.MainDispatcherRule
import java.nio.file.Path
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoImportViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun analysisKeepsLowConfidenceRowsEditableAndDoesNotPersist() = runTest {
        val repository = FakeVocabularyRepository()
        val service = FakeVocabularyAiService(
            analyzed = listOf(AnalyzedEntry("applc", "사과", Confidence.LOW, false)),
        )
        val deleted = mutableListOf<Path>()
        val vm = PhotoImportViewModel(
            repository = repository,
            aiService = service,
            imagePreparation = ImagePreparation { ImageInput("image/jpeg", "YWJj") },
            tempImages = fakeTempImages(deleted),
            savedStateHandle = SavedStateHandle(),
        )

        vm.selectBook(7)
        vm.attach(Path.of("cache/photo.jpg"))
        vm.analyze()
        advanceUntilIdle()

        assertEquals(ImportPhase.REVIEW, vm.state.value.phase)
        assertTrue(vm.state.value.rows.single().isLowConfidence)
        assertTrue(repository.imported.isEmpty())
    }

    @Test
    fun saveSkipsChosenDuplicateAndDeletesTempFile() = runTest {
        val repository = FakeVocabularyRepository()
        repository.addWord(7, NewWord("apple", "기존", sourceType = SourceType.PHOTO))
        val service = FakeVocabularyAiService(
            analyzed = listOf(
                AnalyzedEntry("Apple", "사과", Confidence.HIGH, true),
                AnalyzedEntry("banana", "바나나", Confidence.HIGH, false),
            ),
        )
        val deleted = mutableListOf<Path>()
        val path = Path.of("cache/photo.jpg")
        val vm = PhotoImportViewModel(
            repository = repository,
            aiService = service,
            imagePreparation = ImagePreparation { ImageInput("image/jpeg", "YWJj") },
            tempImages = fakeTempImages(deleted),
            savedStateHandle = SavedStateHandle(),
        )

        vm.selectBook(7)
        vm.attach(path)
        vm.analyze()
        advanceUntilIdle()
        assertTrue(vm.state.value.rows.first().duplicateWordId != null)
        vm.setDuplicateAction(vm.state.value.rows.first().id, DuplicateAction.SKIP)
        vm.save()
        advanceUntilIdle()

        assertEquals(listOf("banana"), repository.imported.map { it.expression })
        assertEquals(listOf(path), deleted)
        assertEquals(ImportPhase.COMPLETE, vm.state.value.phase)
    }

    @Test
    fun openAiFailureMapsToKoreanAndKeepsPreview() = runTest {
        val repository = FakeVocabularyRepository()
        val vm = PhotoImportViewModel(
            repository,
            aiService = object : com.yangi.engvocab.core.openai.VocabularyAiService {
                override suspend fun checkConnection() = Unit
                override suspend fun analyzeImage(input: ImageInput) = throw com.yangi.engvocab.core.openai.OpenAiFailure.MissingKey
                override suspend fun suggestMeaning(expression: String) = ""
            },
            imagePreparation = ImagePreparation { ImageInput("image/jpeg", "YWJj") },
            tempImages = fakeTempImages(mutableListOf()),
            savedStateHandle = SavedStateHandle(),
        )
        vm.selectBook(7); vm.attach(Path.of("cache/photo.jpg")); vm.analyze(); advanceUntilIdle()
        assertEquals(ImportPhase.PREVIEW, vm.state.value.phase)
        assertEquals("설정에서 OpenAI API 키를 먼저 저장하세요.", vm.state.value.error)
    }

    @Test
    fun savedStateRestoresSelectedBookAndExistingPhotoPreview() = runTest {
        val path = Path.of("cache/restored.jpg")
        val vm = PhotoImportViewModel(
            repository = FakeVocabularyRepository(),
            aiService = FakeVocabularyAiService(),
            imagePreparation = ImagePreparation { ImageInput("image/jpeg", "YWJj") },
            tempImages = fakeTempImages(mutableListOf()),
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "photo_import_book_id" to 7L,
                    "photo_import_path" to path.toString(),
                ),
            ),
        )

        assertEquals(7L, vm.state.value.selectedBookId)
        assertEquals(path, vm.state.value.imagePath)
        assertEquals(ImportPhase.PREVIEW, vm.state.value.phase)
    }

    private fun fakeTempImages(deleted: MutableList<Path>) = object : TempImageLifecycle {
        override fun delete(path: Path?) { if (path != null) deleted.add(path) }
        override fun exists(path: Path): Boolean = true
    }
}
