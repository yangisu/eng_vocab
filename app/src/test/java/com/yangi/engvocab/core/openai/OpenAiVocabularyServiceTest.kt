package com.yangi.engvocab.core.openai

import com.yangi.engvocab.core.security.ApiKeyProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiVocabularyServiceTest {
    private lateinit var server: MockWebServer
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun close() = server.close()

    @Test
    fun sendsPrivateVisionRequestAndParsesStructuredItems() = runTest {
        server.enqueue(MockResponse(body = responseFor(validItems())))

        val result = service().analyzeImage(ImageInput("image/jpeg", "YWJj"))

        val request = server.takeRequest()
        val body = request.body?.utf8().orEmpty()
        assertEquals("Bearer test-key", request.headers["Authorization"])
        assertTrue(body.contains("\"model\":\"gpt-5.4-mini\""))
        assertTrue(body.contains("\"detail\":\"high\""))
        assertTrue(body.contains("\"store\":false"))
        assertFalse(body.contains("\"store\":true"))
        assertEquals("look forward to", result.single().expression)
        assertEquals("기대하다", result.single().meaning)
    }

    @Test
    fun connectionCheckUsesPrivateMinimalResponseRequest() = runTest {
        server.enqueue(MockResponse(body = """{"id":"resp_test","output":[]}"""))

        service().checkConnection()

        val body = server.takeRequest().body?.utf8().orEmpty()
        assertTrue(body.contains("\"model\":\"gpt-5.4-mini\""))
        assertTrue(body.contains("\"store\":false"))
        assertTrue(body.contains("Reply with OK"))
    }

    @Test
    fun malformedStructuredOutputRetriesOnlyOnce() = runTest {
        server.enqueue(MockResponse(body = responseFor("{not-json")))
        server.enqueue(MockResponse(body = responseFor(validItems())))

        service().analyzeImage(ImageInput("image/jpeg", "YWJj"))

        assertEquals(2, server.requestCount)
    }

    @Test
    fun missingKeyDoesNotSendRequest() = runTest {
        val service = service(ApiKeyProvider { null })

        assertFailure<OpenAiFailure.MissingKey> {
            service.analyzeImage(ImageInput("image/jpeg", "YWJj"))
        }
        assertEquals(0, server.requestCount)
    }

    @Test
    fun mapsHttpFailuresWithoutRetry() = runTest {
        val cases = listOf(
            401 to OpenAiFailure.Unauthorized::class.java,
            403 to OpenAiFailure.Forbidden::class.java,
            400 to OpenAiFailure.BadRequest::class.java,
            429 to OpenAiFailure.RateLimited::class.java,
            500 to OpenAiFailure.Server::class.java,
        )
        for ((code, expected) in cases) {
            server.enqueue(MockResponse(code = code, body = "{}"))
            val failure = captureFailure {
                service().analyzeImage(ImageInput("image/jpeg", "YWJj"))
            }
            assertEquals(expected, failure.javaClass)
        }
        assertEquals(5, server.requestCount)
    }

    @Test
    fun rejectsEmptyAndOverflowResultsWithoutRetry() = runTest {
        server.enqueue(MockResponse(body = responseFor("{\"items\":[]}")))
        assertFailure<OpenAiFailure.EmptyResult> {
            service().analyzeImage(ImageInput("image/jpeg", "YWJj"))
        }

        val items = buildJsonObject {
            putJsonArray("items") {
                repeat(201) {
                    add(entry("word$it", "뜻", "high", false))
                }
            }
        }.toString()
        server.enqueue(MockResponse(body = responseFor(items)))
        assertFailure<OpenAiFailure.TooManyItems> {
            service().analyzeImage(ImageInput("image/jpeg", "YWJj"))
        }
        assertEquals(2, server.requestCount)
    }

    @Test
    fun invalidLocalEntryShapeGetsOneRetryThenFails() = runTest {
        val invalid = buildJsonObject {
            putJsonArray("items") { add(entry("123", "", "low", false)) }
        }.toString()
        server.enqueue(MockResponse(body = responseFor(invalid)))
        server.enqueue(MockResponse(body = responseFor(invalid)))

        assertFailure<OpenAiFailure.InvalidResponse> {
            service().analyzeImage(ImageInput("image/jpeg", "YWJj"))
        }
        assertEquals(2, server.requestCount)
    }

    private fun service(provider: ApiKeyProvider = ApiKeyProvider { "test-key" }) =
        OpenAiVocabularyService(
            client = OpenAiClient(endpoint = server.url("/v1/responses").toString()),
            apiKeyProvider = provider,
            json = json,
        )

    private fun validItems() = buildJsonObject {
        putJsonArray("items") {
            add(entry(" look forward to ", " 기대하다 ", "high", true))
        }
    }.toString()

    private fun entry(expression: String, meaning: String, confidence: String, sourceMeaning: Boolean) =
        buildJsonObject {
            put("expression", expression)
            put("meaning", meaning)
            put("confidence", confidence)
            put("sourceMeaningPresent", sourceMeaning)
        }

    private fun responseFor(outputText: String) = buildJsonObject {
        put("id", "resp_test")
        put("output", buildJsonArray {
            add(buildJsonObject {
                put("type", "message")
                put("role", "assistant")
                put("content", buildJsonArray {
                    add(buildJsonObject {
                        put("type", "output_text")
                        put("text", outputText)
                    })
                })
            })
        })
    }.toString()

    private suspend inline fun <reified T : Throwable> assertFailure(noinline block: suspend () -> Unit) {
        assertTrue(captureFailure(block) is T)
    }

    private suspend fun captureFailure(block: suspend () -> Unit): Throwable = try {
        block()
        throw AssertionError("Expected failure")
    } catch (failure: Throwable) {
        failure
    }
}
