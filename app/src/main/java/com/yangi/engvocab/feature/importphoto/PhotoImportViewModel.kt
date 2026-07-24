package com.yangi.engvocab.feature.importphoto

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.image.ImagePreprocessor
import com.yangi.engvocab.core.image.TempImageStore
import com.yangi.engvocab.core.model.NewWord
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.openai.AnalyzedEntry
import com.yangi.engvocab.core.openai.Confidence
import com.yangi.engvocab.core.openai.ImageInput
import com.yangi.engvocab.core.openai.OpenAiFailure
import com.yangi.engvocab.core.openai.VocabularyAiService
import com.yangi.engvocab.core.repository.DuplicateExpressionException
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.repository.WordFilter
import com.yangi.engvocab.core.text.normalizeExpression
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportPhase { DESTINATION, SOURCE, PREVIEW, ANALYZING, REVIEW, SAVING, COMPLETE }
enum class DuplicateAction { SKIP, EDIT }

data class ImportRow(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val meaning: String,
    val confidence: Confidence,
    val sourceMeaningPresent: Boolean,
    val duplicateWordId: Long? = null,
    val duplicateExpression: String? = null,
    val duplicateMeaning: String? = null,
    val duplicateAction: DuplicateAction? = null,
    val error: String? = null,
) {
    val isLowConfidence: Boolean get() = confidence == Confidence.LOW
}

data class PhotoImportState(
    val phase: ImportPhase = ImportPhase.DESTINATION,
    val books: List<VocabularyBook> = emptyList(),
    val selectedBookId: Long? = null,
    val imagePath: Path? = null,
    val rows: List<ImportRow> = emptyList(),
    val savedWordIds: List<Long> = emptyList(),
    val error: String? = null,
)

fun interface ImagePreparation {
    suspend fun prepare(path: Path): ImageInput
}

interface TempImageLifecycle {
    fun delete(path: Path?)
    fun exists(path: Path): Boolean
}

class PhotoImportViewModel(
    private val repository: VocabularyRepository,
    private val aiService: VocabularyAiService,
    private val imagePreparation: ImagePreparation,
    private val tempImages: TempImageLifecycle,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val restoredBookId = savedStateHandle.get<Long>(KEY_BOOK_ID)
    private val restoredPath = savedStateHandle.get<String>(KEY_PATH)?.let(Path::of)
    private val canRestore = restoredPath?.let(tempImages::exists) == true
    private val mutableState = MutableStateFlow(
        PhotoImportState(
            phase = when {
                canRestore && restoredBookId != null -> ImportPhase.PREVIEW
                restoredBookId != null -> ImportPhase.SOURCE
                else -> ImportPhase.DESTINATION
            },
            selectedBookId = restoredBookId,
            imagePath = restoredPath?.takeIf { canRestore },
        ),
    )
    val state: StateFlow<PhotoImportState> = mutableState.asStateFlow()
    private var analysisJob: Job? = null

    init {
        viewModelScope.launch {
            repository.books().collect { books -> mutableState.update { it.copy(books = books) } }
        }
    }

    fun selectBook(bookId: Long) {
        savedStateHandle[KEY_BOOK_ID] = bookId
        mutableState.update {
            it.copy(
                selectedBookId = bookId,
                phase = if (it.imagePath == null) ImportPhase.SOURCE else ImportPhase.PREVIEW,
                error = null,
            )
        }
    }

    fun attach(path: Path) {
        val old = state.value.imagePath
        if (old != null && old != path) tempImages.delete(old)
        savedStateHandle[KEY_PATH] = path.toString()
        mutableState.update { it.copy(imagePath = path, phase = ImportPhase.PREVIEW, rows = emptyList(), error = null) }
    }

    fun reportError(message: String) {
        setError(message)
    }

    fun analyze() {
        val bookId = state.value.selectedBookId ?: return setError("저장할 단어장을 선택하세요.")
        val path = state.value.imagePath ?: return setError("분석할 사진을 선택하세요.")
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            mutableState.update { it.copy(phase = ImportPhase.ANALYZING, error = null) }
            try {
                val input = imagePreparation.prepare(path)
                val analyzed = aiService.analyzeImage(input)
                val existing = repository.words(bookId, WordFilter.ALL).first()
                val byExpression = existing.associateBy { normalizeExpression(it.expression) }
                val rows = analyzed.map { entry ->
                    val duplicate = byExpression[normalizeExpression(entry.expression)]
                    entry.toRow(duplicate?.id, duplicate?.expression, duplicate?.meaning)
                }
                mutableState.update { it.copy(phase = ImportPhase.REVIEW, rows = rows, error = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                mutableState.update {
                    it.copy(phase = ImportPhase.PREVIEW, error = failure.toUserMessage())
                }
            }
        }
    }

    fun updateRow(id: String, expression: String? = null, meaning: String? = null) {
        mutableState.update { current ->
            current.copy(rows = current.rows.map { row ->
                if (row.id == id) row.copy(
                    expression = expression ?: row.expression,
                    meaning = meaning ?: row.meaning,
                    error = null,
                ) else row
            })
        }
    }

    fun setDuplicateAction(id: String, action: DuplicateAction) {
        mutableState.update { current ->
            current.copy(rows = current.rows.map { if (it.id == id) it.copy(duplicateAction = action, error = null) else it })
        }
    }

    fun deleteRow(id: String) = mutableState.update { current ->
        current.copy(rows = current.rows.filterNot { it.id == id })
    }

    fun addRow() = mutableState.update { current ->
        current.copy(
            rows = current.rows + ImportRow(
                expression = "",
                meaning = "",
                confidence = Confidence.MEDIUM,
                sourceMeaningPresent = false,
            ),
        )
    }

    fun save() {
        val current = state.value
        val bookId = current.selectedBookId ?: return setError("저장할 단어장을 선택하세요.")
        if (current.rows.isEmpty()) return setError("저장할 단어가 없습니다.")
        if (current.rows.size > MAX_ITEMS) return setError("항목이 200개를 넘습니다. 사진을 나누어 촬영하세요.")

        var invalid = false
        val validatedRows = current.rows.map { row ->
            val error = when {
                !LATIN.containsMatchIn(row.expression) -> "영어 표현을 확인하세요."
                row.meaning.isBlank() -> "뜻을 입력하세요."
                row.duplicateWordId != null && row.duplicateAction == null -> "중복 처리 방법을 선택하세요."
                row.duplicateAction == DuplicateAction.EDIT &&
                    normalizeExpression(row.expression) == normalizeExpression(row.duplicateExpression.orEmpty()) ->
                    "기존 표현과 다르게 수정하세요."
                else -> null
            }
            if (error != null) invalid = true
            row.copy(error = error)
        }
        if (invalid) {
            mutableState.update { it.copy(rows = validatedRows, error = "확인이 필요한 항목이 있습니다.") }
            return
        }

        val accepted = validatedRows
            .filterNot { it.duplicateAction == DuplicateAction.SKIP }
            .map {
                NewWord(
                    expression = it.expression.trim(),
                    meaning = it.meaning.trim(),
                    sourceType = SourceType.PHOTO,
                )
            }
        viewModelScope.launch {
            mutableState.update { it.copy(phase = ImportPhase.SAVING, error = null) }
            try {
                val ids = repository.importWords(bookId, accepted)
                tempImages.delete(current.imagePath)
                savedStateHandle[KEY_PATH] = null
                mutableState.update {
                    it.copy(phase = ImportPhase.COMPLETE, imagePath = null, savedWordIds = ids, error = null)
                }
            } catch (_: DuplicateExpressionException) {
                mutableState.update {
                    it.copy(phase = ImportPhase.REVIEW, error = "같은 단어장에 이미 있는 표현이 있습니다.")
                }
            } catch (_: Throwable) {
                mutableState.update { it.copy(phase = ImportPhase.REVIEW, error = "단어를 저장하지 못했습니다.") }
            }
        }
    }

    fun cancel() {
        analysisJob?.cancel()
        tempImages.delete(state.value.imagePath)
        savedStateHandle[KEY_PATH] = null
        mutableState.update { it.copy(imagePath = null, rows = emptyList(), phase = ImportPhase.DESTINATION) }
    }

    private fun setError(message: String) {
        mutableState.update { it.copy(error = message) }
    }

    class Factory(
        private val repository: VocabularyRepository,
        private val aiService: VocabularyAiService,
        private val preprocessor: ImagePreprocessor,
        private val tempImageStore: TempImageStore,
        private val initialBookId: Long? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            if (savedStateHandle.get<Long>(KEY_BOOK_ID) == null && initialBookId != null) {
                savedStateHandle[KEY_BOOK_ID] = initialBookId
            }
            return PhotoImportViewModel(
                repository,
                aiService,
                ImagePreparation { path -> withContext(Dispatchers.IO) { preprocessor.prepare(path) } },
                object : TempImageLifecycle {
                    override fun delete(path: Path?) = tempImageStore.delete(path)
                    override fun exists(path: Path): Boolean = Files.exists(path)
                },
                savedStateHandle,
            ) as T
        }
    }

    private companion object {
        const val KEY_BOOK_ID = "photo_import_book_id"
        const val KEY_PATH = "photo_import_path"
        const val MAX_ITEMS = 200
        val LATIN = Regex("[A-Za-z]")
    }
}

private fun AnalyzedEntry.toRow(
    duplicateId: Long?,
    duplicateExpression: String?,
    duplicateMeaning: String?,
) = ImportRow(
    expression = expression,
    meaning = meaning,
    confidence = confidence,
    sourceMeaningPresent = sourceMeaningPresent,
    duplicateWordId = duplicateId,
    duplicateExpression = duplicateExpression,
    duplicateMeaning = duplicateMeaning,
)

private fun Throwable.toUserMessage(): String = when (this) {
    OpenAiFailure.MissingKey -> "설정에서 OpenAI API 키를 먼저 저장하세요."
    OpenAiFailure.Unauthorized -> "API 키가 올바르지 않습니다."
    OpenAiFailure.Forbidden -> "이 API 키로 OpenAI 모델에 접근할 수 없습니다."
    OpenAiFailure.BadRequest -> "OpenAI가 사진 분석 요청을 거부했습니다."
    OpenAiFailure.RateLimited -> "사용량 또는 요청 한도에 도달했습니다. 잠시 후 다시 시도하세요."
    OpenAiFailure.Server -> "OpenAI 서버에 문제가 발생했습니다. 잠시 후 다시 시도하세요."
    OpenAiFailure.Network -> "네트워크 연결을 확인하고 다시 시도하세요."
    OpenAiFailure.EmptyResult -> "단어를 찾지 못했습니다. 더 가까이에서 수평으로 촬영해 보세요."
    OpenAiFailure.TooManyItems -> "항목이 200개를 넘습니다. 사진을 나누어 촬영하세요."
    OpenAiFailure.InvalidResponse -> "분석 결과를 읽지 못했습니다. 다시 시도하세요."
    else -> "사진을 분석하지 못했습니다. 다시 시도하세요."
}
