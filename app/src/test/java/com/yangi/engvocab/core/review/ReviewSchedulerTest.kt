package com.yangi.engvocab.core.review

import com.yangi.engvocab.core.model.ReviewResult
import com.yangi.engvocab.core.model.ReviewSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewSchedulerTest {
    private val today = LocalDate.of(2026, 7, 14)

    @Test
    fun correctIntervalsAreOneThenThreeThenEaseMultiplied() {
        val first = ReviewScheduler.answer(ReviewSnapshot.new(), ReviewResult.CORRECT, today)
        val second = ReviewScheduler.answer(first, ReviewResult.CORRECT, today.plusDays(1))
        val third = ReviewScheduler.answer(second, ReviewResult.CORRECT, today.plusDays(4))

        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertEquals(8, third.intervalDays)
        assertEquals(today.plusDays(12), third.nextReviewDate)
        assertEquals(2.65, third.easeFactor, 0.0001)
    }

    @Test
    fun wrongResetsProgressAndBoundsEase() {
        val current = ReviewSnapshot.new().copy(
            repetition = 4,
            intervalDays = 20,
            easeFactor = 1.3,
            correctStreak = 4,
        )

        val result = ReviewScheduler.answer(current, ReviewResult.WRONG, today)

        assertEquals(0, result.repetition)
        assertEquals(1, result.intervalDays)
        assertEquals(1.3, result.easeFactor, 0.0)
        assertEquals(0, result.correctStreak)
        assertEquals(1, result.totalWrong)
        assertEquals(today.plusDays(1), result.nextReviewDate)
    }

    @Test
    fun correctEaseNeverExceedsThree() {
        val current = ReviewSnapshot.new().copy(easeFactor = 2.99)

        val result = ReviewScheduler.answer(current, ReviewResult.CORRECT, today)

        assertEquals(3.0, result.easeFactor, 0.0)
    }
}
