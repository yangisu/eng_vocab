package com.yangi.engvocab.core.openai

import com.yangi.engvocab.core.security.ApiKeyProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class OpenAiVocabularyService(
    private val client: OpenAiClient,
    private val apiKeyProvider: ApiKeyProvider,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : VocabularyAiService {
    override suspend fun checkConnection() {
        val apiKey = apiKeyProvider.load()?.takeIf(String::isNotBlank)
            ?: throw OpenAiFailure.MissingKey
        client.createResponse(apiKey, connectionRequest().toString())
    }

    override suspend fun analyzeImage(input: ImageInput): List<AnalyzedEntry> {
        require(input.mimeType.startsWith("image/")) { "이미지 형식이 아닙니다." }
        require(input.base64.isNotBlank()) { "이미지 데이터가 비어 있습니다." }
        val apiKey = apiKeyProvider.load()?.takeIf(String::isNotBlank)
            ?: throw OpenAiFailure.MissingKey
        val request = analysisRequest(input).toString()
        return requestWithShapeRetry(apiKey, request, ::parseAnalysis)
    }

    override suspend fun suggestMeaning(expression: String): String {
        val cleanExpression = expression.trim()
        require(LATIN_LETTER.containsMatchIn(cleanExpression)) { "영어 단어나 문구를 입력하세요." }
        val apiKey = apiKeyProvider.load()?.takeIf(String::isNotBlank)
            ?: throw OpenAiFailure.MissingKey
        val request = meaningRequest(cleanExpression).toString()
        return requestWithShapeRetry(apiKey, request, ::parseMeaning)
    }

    private suspend fun <T> requestWithShapeRetry(
        apiKey: String,
        request: String,
        parse: (String) -> T,
    ): T {
        repeat(2) { attempt ->
            val raw = client.createResponse(apiKey, request)
            try {
                return parse(raw)
            } catch (failure: OpenAiFailure.InvalidResponse) {
                if (attempt == 1) throw failure
            }
        }
        throw OpenAiFailure.InvalidResponse
    }

    private fun parseAnalysis(rawResponse: String): List<AnalyzedEntry> {
        val structured = extractOutputText(rawResponse)
        val payload = try {
            json.decodeFromString<AnalyzedItems>(structured)
        } catch (_: SerializationException) {
            throw OpenAiFailure.InvalidResponse
        } catch (_: IllegalArgumentException) {
            throw OpenAiFailure.InvalidResponse
        }
        if (payload.items.isEmpty()) throw OpenAiFailure.EmptyResult
        if (payload.items.size > MAX_ITEMS) throw OpenAiFailure.TooManyItems
        return payload.items.map { item ->
            val expression = item.expression.trim()
            val meaning = item.meaning.trim()
            if (!LATIN_LETTER.containsMatchIn(expression) || meaning.isEmpty()) {
                throw OpenAiFailure.InvalidResponse
            }
            item.copy(expression = expression, meaning = meaning)
        }
    }

    private fun parseMeaning(rawResponse: String): String {
        val structured = extractOutputText(rawResponse)
        val meaning = try {
            json.decodeFromString<MeaningPayload>(structured).meaning.trim()
        } catch (_: SerializationException) {
            throw OpenAiFailure.InvalidResponse
        } catch (_: IllegalArgumentException) {
            throw OpenAiFailure.InvalidResponse
        }
        if (meaning.isEmpty()) throw OpenAiFailure.InvalidResponse
        return meaning
    }

    private fun extractOutputText(rawResponse: String): String = try {
        val root = json.parseToJsonElement(rawResponse).jsonObject
        root["output"]!!.jsonArray
            .asSequence()
            .map(JsonElement::jsonObject)
            .filter { it["type"]?.jsonPrimitive?.contentOrNull == "message" }
            .filter { it["role"]?.jsonPrimitive?.contentOrNull == "assistant" }
            .flatMap { it["content"]!!.jsonArray.asSequence() }
            .map(JsonElement::jsonObject)
            .first { it["type"]?.jsonPrimitive?.contentOrNull == "output_text" }
            .getValue("text").jsonPrimitive.content
    } catch (_: Exception) {
        throw OpenAiFailure.InvalidResponse
    }

    private fun analysisRequest(input: ImageInput): JsonObject = buildJsonObject {
        put("model", MODEL)
        put("store", false)
        put("input", buildJsonArray {
            add(buildJsonObject {
                put("role", "user")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "input_text")
                        put("text", ANALYSIS_PROMPT)
                    })
                    add(buildJsonObject {
                        put("type", "input_image")
                        put("image_url", "data:${input.mimeType};base64,${input.base64}")
                        put("detail", "high")
                    })
                })
            })
        })
        put("text", format(ANALYSIS_SCHEMA, "vocabulary_entries"))
    }

    private fun connectionRequest(): JsonObject = buildJsonObject {
        put("model", MODEL)
        put("store", false)
        put("input", "Reply with OK")
    }

    private fun meaningRequest(expression: String): JsonObject = buildJsonObject {
        put("model", MODEL)
        put("store", false)
        put("input", buildJsonArray {
            add(buildJsonObject {
                put("role", "user")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "input_text")
                        put("text", "다음 영어 단어나 문구의 가장 간결하고 일반적인 한국어 뜻만 제시하세요: $expression")
                    })
                })
            })
        })
        put("text", format(MEANING_SCHEMA, "vocabulary_meaning"))
    }

    private fun format(schema: JsonObject, name: String) = buildJsonObject {
        putJsonObject("format") {
            put("type", "json_schema")
            put("name", name)
            put("strict", true)
            put("schema", schema)
        }
    }

    @Serializable
    private data class AnalyzedItems(val items: List<AnalyzedEntry>)

    @Serializable
    private data class MeaningPayload(val meaning: String)

    private companion object {
        const val MODEL = "gpt-5.4-mini"
        const val MAX_ITEMS = 200
        val LATIN_LETTER = Regex("[A-Za-z]")

        val ANALYSIS_PROMPT = """
            사진에 실제로 보이는 영어 단어와 영어 문구를 읽는 순서대로 추출하세요.
            제목, 페이지 번호, 장식 문구, 영어가 아닌 잡음은 제외하세요.
            사진에 한국어 뜻이 함께 있으면 그대로 정리하고 sourceMeaningPresent를 true로 표시하세요.
            뜻이 없으면 간결하고 일반적인 한국어 뜻을 작성하고 sourceMeaningPresent를 false로 표시하세요.
            예문이나 추가 설명은 만들지 마세요. 최대 200개까지만 반환하세요.
            확신이 낮은 철자나 뜻은 confidence를 low로 표시하세요.
        """.trimIndent()

        val ANALYSIS_SCHEMA by lazy { buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("items") {
                    put("type", "array")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            put("expression", stringSchema())
                            put("meaning", stringSchema())
                            putJsonObject("confidence") {
                                put("type", "string")
                                put("enum", JsonArray(listOf("high", "medium", "low").map { JsonPrimitive(it) }))
                            }
                            putJsonObject("sourceMeaningPresent") { put("type", "boolean") }
                        }
                        put("required", JsonArray(REQUIRED_ITEM_FIELDS.map { JsonPrimitive(it) }))
                        put("additionalProperties", false)
                    }
                }
            }
            put("required", JsonArray(listOf(JsonPrimitive("items"))))
            put("additionalProperties", false)
        } }

        val MEANING_SCHEMA = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") { put("meaning", stringSchema()) }
            put("required", JsonArray(listOf(JsonPrimitive("meaning"))))
            put("additionalProperties", false)
        }

        val REQUIRED_ITEM_FIELDS = listOf(
            "expression", "meaning", "confidence", "sourceMeaningPresent",
        )

        fun stringSchema() = buildJsonObject { put("type", "string") }
    }
}
