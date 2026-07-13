package com.yangi.engvocab.core.security

import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

fun interface ApiKeyProvider {
    suspend fun load(): String?
}

class ApiKeyStore(
    private val preferences: EncryptedPreferences,
    private val cipher: ApiKeyCipher,
) : ApiKeyProvider {
    fun observeConfigured(): Flow<Boolean> = preferences.observeConfigured()

    suspend fun save(apiKey: String) {
        val cleanKey = apiKey.trim()
        require(cleanKey.isNotEmpty()) { "API 키를 입력하세요." }
        val encrypted = cipher.encrypt(cleanKey.encodeToByteArray())
        preferences.write(
            ciphertext = Base64.getEncoder().encodeToString(encrypted.ciphertext),
            iv = Base64.getEncoder().encodeToString(encrypted.iv),
        )
    }

    override suspend fun load(): String? {
        val ciphertext = preferences.readCiphertext()
        val iv = preferences.readIv()
        if (ciphertext == null || iv == null) {
            if (ciphertext != null || iv != null) preferences.clear()
            return null
        }
        return try {
            val encrypted = EncryptedValue(
                ciphertext = Base64.getDecoder().decode(ciphertext),
                iv = Base64.getDecoder().decode(iv),
            )
            cipher.decrypt(encrypted).decodeToString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            preferences.clear()
            null
        }
    }

    suspend fun clear() = preferences.clear()
}
