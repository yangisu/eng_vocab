package com.yangi.engvocab.feature.study

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yangi.engvocab.core.model.ReviewSnapshot
import com.yangi.engvocab.core.model.StudyMode
import com.yangi.engvocab.core.model.StudyWord
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.repository.WordFilter
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.first

@Composable
fun StudyEntryRoute(
    repository: VocabularyRepository,
    clock: Clock,
    bookId: Long?,
    dueOnly: Boolean,
    explicitIds: List<Long>,
    onRetryWrong: (List<Long>) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words by produceState<List<StudyWord>?>(null, bookId, dueOnly, explicitIds) {
        value = when {
            explicitIds.isNotEmpty() -> explicitIds.mapNotNull { id ->
                repository.word(id)?.let { StudyWord(it, ReviewSnapshot.new()) }
            }
            dueOnly -> repository.dueWords(LocalDate.now(clock), bookId).first()
            bookId != null -> repository.words(bookId, WordFilter.ALL).first().map {
                StudyWord(it, ReviewSnapshot.new())
            }
            else -> emptyList()
        }
    }
    var mode by remember { mutableStateOf<StudyMode?>(null) }

    when {
        words == null -> CenteredMessage("학습 목록 불러오는 중…", modifier)
        words!!.isEmpty() -> CenteredMessage("학습할 단어가 없습니다.", modifier)
        mode == null -> StudyModeSelectionScreen(onSelect = { mode = it }, onCancel = onDone, modifier = modifier)
        else -> {
            val selectedMode = requireNotNull(mode)
            val idsKey = words!!.joinToString("-") { it.word.id.toString() }
            val studyViewModel: StudyViewModel = viewModel(
                key = "study-$selectedMode-$idsKey",
                factory = StudyViewModel.Factory(repository, selectedMode, words!!, clock),
            )
            StudyRoute(studyViewModel, onRetryWrong, onDone, modifier)
        }
    }
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text) }
}
