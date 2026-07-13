package com.yangi.engvocab.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.yangi.engvocab.core.database.entity.ReviewLogEntity
import com.yangi.engvocab.core.database.entity.ReviewStateEntity

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_states WHERE wordId = :wordId")
    suspend fun state(wordId: Long): ReviewStateEntity?

    @Query("SELECT * FROM review_states WHERE wordId IN (:wordIds)")
    suspend fun states(wordIds: List<Long>): List<ReviewStateEntity>

    @Upsert
    suspend fun upsertState(state: ReviewStateEntity)

    @Insert
    suspend fun insertLog(log: ReviewLogEntity): Long

    @Query("SELECT COUNT(*) FROM review_logs")
    suspend fun countLogs(): Int

    @Query("SELECT COUNT(*) FROM review_states")
    suspend fun countStates(): Int
}
