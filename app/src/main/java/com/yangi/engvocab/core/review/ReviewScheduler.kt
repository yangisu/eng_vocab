package com.yangi.engvocab.core.review

import com.yangi.engvocab.core.model.ReviewResult
import com.yangi.engvocab.core.model.ReviewSnapshot
import java.time.LocalDate
import kotlin.math.roundToInt

object ReviewScheduler {
    fun answer(
        current: ReviewSnapshot,
        result: ReviewResult,
        today: LocalDate,
    ): ReviewSnapshot = when (result) {
        ReviewResult.CORRECT -> {
            val days = when (current.repetition) {
                0 -> 1
                1 -> 3
                else -> (current.intervalDays * current.easeFactor).roundToInt().coerceAtLeast(1)
            }
            current.copy(
                repetition = current.repetition + 1,
                intervalDays = days,
                easeFactor = (current.easeFactor + 0.05).coerceAtMost(3.0),
                nextReviewDate = today.plusDays(days.toLong()),
                correctStreak = current.correctStreak + 1,
                totalCorrect = current.totalCorrect + 1,
                lastResult = result,
            )
        }

        ReviewResult.WRONG -> current.copy(
            repetition = 0,
            intervalDays = 1,
            easeFactor = (current.easeFactor - 0.20).coerceAtLeast(1.3),
            nextReviewDate = today.plusDays(1),
            correctStreak = 0,
            totalWrong = current.totalWrong + 1,
            lastResult = result,
        )
    }
}
