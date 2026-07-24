package com.yangi.engvocab.core.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnalyzedEntry(
    val expression: String,
    val meaning: String,
    val confidence: Confidence,
    val sourceMeaningPresent: Boolean,
)

@Serializable
enum class Confidence {
    @SerialName("high") HIGH,
    @SerialName("medium") MEDIUM,
    @SerialName("low") LOW,
}

data class ImageInput(
    val mimeType: String,
    val base64: String,
)

sealed class OpenAiFailure(message: String) : Exception(message) {
    data object MissingKey : OpenAiFailure("API key is not configured")
    data object Unauthorized : OpenAiFailure("API key was rejected")
    data object Forbidden : OpenAiFailure("OpenAI access was forbidden")
    data object BadRequest : OpenAiFailure("OpenAI request was rejected")
    data object RateLimited : OpenAiFailure("OpenAI rate or usage limit reached")
    data object Server : OpenAiFailure("OpenAI server failed")
    data object Network : OpenAiFailure("Network request failed")
    data object InvalidResponse : OpenAiFailure("OpenAI response was invalid")
    data object EmptyResult : OpenAiFailure("No vocabulary entries found")
    data object TooManyItems : OpenAiFailure("More than 200 entries returned")
}
