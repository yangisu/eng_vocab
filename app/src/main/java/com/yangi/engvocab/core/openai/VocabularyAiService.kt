package com.yangi.engvocab.core.openai

interface VocabularyAiService {
    suspend fun checkConnection()

    suspend fun analyzeImage(input: ImageInput): List<AnalyzedEntry>

    suspend fun suggestMeaning(expression: String): String
}
