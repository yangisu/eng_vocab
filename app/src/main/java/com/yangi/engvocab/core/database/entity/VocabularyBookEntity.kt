package com.yangi.engvocab.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "vocabulary_books")
data class VocabularyBookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
