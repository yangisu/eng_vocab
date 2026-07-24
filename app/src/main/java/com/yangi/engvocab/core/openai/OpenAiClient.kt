package com.yangi.engvocab.core.openai

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiClient(
    private val endpoint: String = "https://api.openai.com/v1/responses",
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    suspend fun createResponse(apiKey: String, requestJson: String): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", JSON_MEDIA_TYPE.toString())
                .post(requestJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body.string()
                    when {
                        response.isSuccessful -> body
                        response.code == 401 -> throw OpenAiFailure.Unauthorized
                        response.code == 403 -> throw OpenAiFailure.Forbidden
                        response.code == 429 -> throw OpenAiFailure.RateLimited
                        response.code in 400..499 -> throw OpenAiFailure.BadRequest
                        response.code in 500..599 -> throw OpenAiFailure.Server
                        else -> throw OpenAiFailure.Network
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: OpenAiFailure) {
                throw failure
            } catch (_: IOException) {
                throw OpenAiFailure.Network
            }
        }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
