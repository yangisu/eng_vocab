package com.yangi.engvocab.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yangi.engvocab.core.model.StudyMode
import java.time.Instant

@Entity(
    tableName = "review_logs",
    foreignKeys = [
        ForeignKey(
            entity = WordEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["wordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("wordId")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wordId: Long,
    val mode: StudyMode,
    val submittedAnswer: String?,
    val automaticResult: Boolean?,
    val finalResult: Boolean,
    val wasOverridden: Boolean,
    val reviewedAt: Instant,
)
