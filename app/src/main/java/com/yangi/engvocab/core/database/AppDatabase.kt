package com.yangi.engvocab.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yangi.engvocab.core.database.dao.ReviewDao
import com.yangi.engvocab.core.database.dao.VocabularyDao
import com.yangi.engvocab.core.database.entity.ReviewLogEntity
import com.yangi.engvocab.core.database.entity.ReviewStateEntity
import com.yangi.engvocab.core.database.entity.VocabularyBookEntity
import com.yangi.engvocab.core.database.entity.WordEntryEntity

@Database(
    entities = [
        VocabularyBookEntity::class,
        WordEntryEntity::class,
        ReviewStateEntity::class,
        ReviewLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun reviewDao(): ReviewDao
}
