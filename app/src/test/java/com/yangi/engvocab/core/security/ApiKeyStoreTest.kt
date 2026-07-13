package com.yangi.engvocab.core.security

import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiKeyStoreTest {
    @Test
    fun storesOnlyCiphertextAndRoundTripsPlaintext() = runTest {
        val preferences = FakeEncryptedPreferences()
        val store = ApiKeyStore(preferences, FakeCipher())

        store.save("  sk-secret  ")

        assertFalse(preferences.rawValues().any { it?.contains("sk-secret") == true })
        assertEquals("sk-secret", store.load())
        assertTrue(preferences.isConfigured())
    }

    @Test
    fun rejectsBlankKeyWithoutWriting() = runTest {
        val preferences = FakeEncryptedPreferences()
        val store = ApiKeyStore(preferences, FakeCipher())

        var rejected = false
        try {
            store.save("   ")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
        assertFalse(preferences.isConfigured())
    }

    @Test
    fun incompleteOrCorruptPayloadIsCleared() = runTest {
        val preferences = FakeEncryptedPreferences()
        preferences.write("not-valid-base64", Base64.getEncoder().encodeToString(byteArrayOf(1)))
        val store = ApiKeyStore(preferences, FakeCipher())

        assertNull(store.load())
        assertFalse(preferences.isConfigured())
    }
}

private class FakeEncryptedPreferences : EncryptedPreferences {
    private val value = MutableStateFlow<Pair<String?, String?>>(null to null)

    override suspend fun readCiphertext(): String? = value.value.first

    override suspend fun readIv(): String? = value.value.second

    override suspend fun write(ciphertext: String, iv: String) {
        value.value = ciphertext to iv
    }

    override suspend fun clear() {
        value.value = null to null
    }

    override fun observeConfigured(): Flow<Boolean> =
        value.map { (ciphertext, iv) -> ciphertext != null && iv != null }

    fun rawValues(): List<String?> = listOf(value.value.first, value.value.second)

    fun isConfigured(): Boolean = value.value.first != null && value.value.second != null
}

private class FakeCipher : ApiKeyCipher {
    override fun encrypt(plaintext: ByteArray): EncryptedValue =
        EncryptedValue(ciphertext = plaintext.reversedArray(), iv = byteArrayOf(7, 4, 1))

    override fun decrypt(value: EncryptedValue): ByteArray {
        require(value.iv.contentEquals(byteArrayOf(7, 4, 1)))
        return value.ciphertext.reversedArray()
    }
}
