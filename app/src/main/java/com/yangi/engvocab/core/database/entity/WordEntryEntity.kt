package com.yangi.engvocab.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.yangi.engvocab.core.model.SourceType
import java.time.Instant

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("bookId"),
        Index(value = ["bookId", "normalizedExpression"], unique = true),
    ],
)
data class WordEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val expression: String,
    val normalizedExpression: String,
    val meaning: String?,
    val isImportant: Boolean,
    val sourceType: SourceType,
    val createdAt: Instant,
    val updatedAt: Instant,
)
