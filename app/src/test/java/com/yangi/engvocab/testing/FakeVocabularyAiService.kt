package com.yangi.engvocab.testing

import com.yangi.engvocab.core.openai.AnalyzedEntry
import com.yangi.engvocab.core.openai.ImageInput
import com.yangi.engvocab.core.openai.VocabularyAiService

class FakeVocabularyAiService(
    private val analyzed: List<AnalyzedEntry> = emptyList(),
    private val meaning: String = "",
) : VocabularyAiService {
    val analyzedImages = mutableListOf<ImageInput>()
    val meaningRequests = mutableListOf<String>()

    override suspend fun analyzeImage(input: ImageInput): List<AnalyzedEntry> {
        analyzedImages += input
        return analyzed
    }

    override suspend fun suggestMeaning(expression: String): String {
        meaningRequests += expression
        return meaning
    }
}
