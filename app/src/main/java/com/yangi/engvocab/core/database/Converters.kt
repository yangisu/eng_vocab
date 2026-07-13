package com.yangi.engvocab.core.database

import androidx.room.TypeConverter
import com.yangi.engvocab.core.model.ReviewResult
import com.yangi.engvocab.core.model.SourceType
import com.yangi.engvocab.core.model.StudyMode
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun sourceTypeToString(value: SourceType): String = value.name
    @TypeConverter fun stringToSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter fun studyModeToString(value: StudyMode): String = value.name
    @TypeConverter fun stringToStudyMode(value: String): StudyMode = StudyMode.valueOf(value)

    @TypeConverter
    fun reviewResultToString(value: ReviewResult?): String? = value?.name

    @TypeConverter
    fun stringToReviewResult(value: String?): ReviewResult? = value?.let(ReviewResult::valueOf)
}
