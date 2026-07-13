package com.yangi.engvocab.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yangi.engvocab.core.model.ReviewResult
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "review_states",
    foreignKeys = [
        ForeignKey(
            entity = WordEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("nextReviewDate")],
)
data class ReviewStateEntity(
    @PrimaryKey val wordId: Long,
    val repetition: Int,
    val intervalDays: Int,
    val easeFactor: Double,
    val nextReviewDate: LocalDate?,
    val correctStreak: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val lastResult: ReviewResult?,
    val lastReviewedAt: Instant?,
)
