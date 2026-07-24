package com.yangi.engvocab.feature.settings

import com.yangi.engvocab.core.openai.OpenAiFailure
import com.yangi.engvocab.core.security.ApiKeyCipher
import com.yangi.engvocab.core.security.ApiKeyStore
import com.yangi.engvocab.core.security.EncryptedPreferences
import com.yangi.engvocab.core.security.EncryptedValue
import com.yangi.engvocab.testing.FakeVocabularyAiService
import com.yangi.engvocab.testing.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun connectionCheckReportsSuccess() = runTest {
        val service = FakeVocabularyAiService()
        val viewModel = configuredViewModel(service)

        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(1, service.connectionChecks)
        assertFalse(viewModel.state.value.isChecking)
        assertEquals("OpenAI 연결에 성공했습니다.", viewModel.state.value.message)
    }

    @Test
    fun connectionCheckExplainsRejectedKey() = runTest {
        val viewModel = configuredViewModel(
            FakeVocabularyAiService(connectionFailure = OpenAiFailure.Unauthorized),
        )

        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(
            "API 키가 거부되었습니다. 키를 다시 확인하세요.",
            viewModel.state.value.error,
        )
    }

    @Test
    fun connectionCheckExplainsUsageLimit() = runTest {
        val viewModel = configuredViewModel(
            FakeVocabularyAiService(connectionFailure = OpenAiFailure.RateLimited),
        )

        viewModel.checkConnection()
        advanceUntilIdle()

        assertEquals(
            "OpenAI 사용량 또는 결제 한도에 도달했습니다.",
            viewModel.state.value.error,
        )
    }

    private suspend fun configuredViewModel(
        service: FakeVocabularyAiService,
    ): SettingsViewModel {
        val store = ApiKeyStore(FakeEncryptedPreferences(), FakeCipher())
        store.save("test-key")
        return SettingsViewModel(store, service)
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
}

private class FakeCipher : ApiKeyCipher {
    override fun encrypt(plaintext: ByteArray): EncryptedValue =
        EncryptedValue(plaintext.reversedArray(), byteArrayOf(1, 2, 3))

    override fun decrypt(value: EncryptedValue): ByteArray = value.ciphertext.reversedArray()
}
