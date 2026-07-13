package com.yangi.engvocab.feature.study

import com.yangi.engvocab.core.model.StudyWord

data class StudySummary(
    val correctCount: Int,
    val wrongCount: Int,
    val wrongWords: List<StudyWord>,
)

class StudySessionEngine(words: List<StudyWord>) {
    private val initial = words.toList()
    private val queue = words.toMutableList()
    private val appendedForRetry = mutableSetOf<Long>()
    private val answeredIndices = mutableSetOf<Int>()
    private val originalResults = linkedMapOf<Long, Boolean>()
    private val wrongOriginalIds = linkedSetOf<Long>()
    private var index = 0

    val originalTotal: Int get() = initial.size
    val hasCurrent: Boolean get() = index in queue.indices
    val current: StudyWord get() = queue[index]
    val isRetry: Boolean get() = index >= initial.size
    val displayPosition: Int get() = if (isRetry) originalTotal else (index + 1).coerceAtMost(originalTotal)

    fun answerCurrent(correct: Boolean) {
        check(hasCurrent) { "학습할 단어가 없습니다." }
        if (!answeredIndices.add(index)) return
        val word = current
        if (index < initial.size) {
            originalResults[word.word.id] = correct
            if (!correct) wrongOriginalIds += word.word.id
        }
        if (!correct && appendedForRetry.add(word.word.id)) {
            queue += word
        }
    }

    fun advance() {
        check(!hasCurrent || index in answeredIndices) { "현재 단어의 결과를 먼저 기록하세요." }
        index += 1
    }

    fun queueIds(): List<Long> = queue.map { it.word.id }

    fun summary(): StudySummary {
        val correct = originalResults.values.count { it }
        val wrong = originalResults.values.count { !it }
        val byId = initial.associateBy { it.word.id }
        return StudySummary(
            correctCount = correct,
            wrongCount = wrong,
            wrongWords = wrongOriginalIds.mapNotNull(byId::get),
        )
    }
}
