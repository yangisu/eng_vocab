package com.yangi.engvocab.core.model

import java.time.Instant
import java.time.LocalDate

enum class SourceType { PHOTO, MANUAL }

enum class StudyMode { SELF_GRADED, TYPED }

enum class ReviewResult { CORRECT, WRONG }

data class VocabularyBook(
    val id: Long,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class WordEntry(
    val id: Long,
    val bookId: Long,
    val expression: String,
    val meaning: String?,
    val isImportant: Boolean,
    val sourceType: SourceType,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class NewWord(
    val expression: String,
    val meaning: String?,
    val isImportant: Boolean = false,
    val sourceType: SourceType = SourceType.MANUAL,
)

data class ReviewSnapshot(
    val repetition: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val nextReviewDate: LocalDate?,
    val correctStreak: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastResult: ReviewResult?,
) {
    companion object {
        fun new() = ReviewSnapshot(
            repetition = 0,
            intervalDays = 0,
            easeFactor = 2.5,
            nextReviewDate = null,
            correctStreak = 0,
            totalCorrect = 0,
            totalWrong = 0,
            lastResult = null,
        )
    }
}

data class ReviewLog(
    val id: Long,
    val wordId: Long,
    val mode: StudyMode,
    val submittedAnswer: String?,
    val result: ReviewResult,
    val reviewedAt: Instant,
)

data class StudyWord(
    val word: WordEntry,
    val review: ReviewSnapshot,
)
