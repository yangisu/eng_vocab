package com.yangi.engvocab.feature.books

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.VocabularyBook
import com.yangi.engvocab.core.model.WordEntry
import com.yangi.engvocab.core.repository.WordFilter
import com.yangi.engvocab.ui.theme.EngVocabTheme
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class BookFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun importantAndWrongFiltersUpdateVisibleRows() {
        val now = Instant.EPOCH
        val important = WordEntry(
            id = 1,
            bookId = 7,
            expression = "look forward to",
            meaning = "기대하다",
            isImportant = true,
            sourceType = SourceType.MANUAL,
            createdAt = now,
            updatedAt = now,
        )
        var state by mutableStateOf(
            BookDetailUiState(
                bookId = 7,
                book = VocabularyBook(7, "여행", now, now),
                words = listOf(important),
            ),
        )

        composeRule.setContent {
            EngVocabTheme {
                BookDetailScreen(
                    state = state,
                    onBack = {},
                    onQueryChange = {},
                    onFilter = { filter ->
                        state = state.copy(
                            filter = filter,
                            words = when (filter) {
                                WordFilter.ALL, WordFilter.IMPORTANT -> listOf(important)
                                WordFilter.WRONG -> emptyList()
                            },
                        )
                    },
                    onToggleImportant = {},
                    onEdit = {},
                    onDelete = {},
                    onAdd = {},
                    onPhotoAdd = {},
                    onStudy = {},
                )
            }
        }

        composeRule.onNodeWithText("중요").performClick()
        composeRule.onNodeWithText("look forward to").assertIsDisplayed()
        composeRule.onNodeWithText("틀린 단어").performClick()
        assertTrue(composeRule.onAllNodesWithText("look forward to").fetchSemanticsNodes().isEmpty())
    }
}
