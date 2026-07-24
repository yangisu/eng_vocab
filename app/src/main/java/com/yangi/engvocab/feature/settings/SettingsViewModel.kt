package com.yangi.engvocab.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.openai.OpenAiFailure
import com.yangi.engvocab.core.openai.VocabularyAiService
import com.yangi.engvocab.core.security.ApiKeyStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val input: String = "",
    val isConfigured: Boolean = false,
    val isSaving: Boolean = false,
    val isChecking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore,
    private val vocabularyAiService: VocabularyAiService,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            apiKeyStore.observeConfigured().collect { configured ->
                mutableState.update { it.copy(isConfigured = configured) }
            }
        }
    }

    fun onInputChange(value: String) {
        mutableState.update { it.copy(input = value, message = null, error = null) }
    }

    fun save() {
        val value = state.value.input
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true, message = null, error = null) }
            runCatching { apiKeyStore.save(value) }
                .onSuccess {
                    mutableState.update {
                        it.copy(input = "", isSaving = false, message = "API 키를 저장했습니다.")
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(isSaving = false, error = error.message ?: "API 키를 저장하지 못했습니다.")
                    }
                }
        }
    }

    fun clear() {
        viewModelScope.launch {
            runCatching { apiKeyStore.clear() }
                .onSuccess {
                    mutableState.update { it.copy(input = "", message = "API 키를 삭제했습니다.", error = null) }
                }
                .onFailure {
                    mutableState.update { it.copy(error = "API 키를 삭제하지 못했습니다.", message = null) }
                }
        }
    }

    fun checkConnection() {
        viewModelScope.launch {
            mutableState.update { it.copy(isChecking = true, message = null, error = null) }
            try {
                vocabularyAiService.checkConnection()
                mutableState.update {
                    it.copy(isChecking = false, message = "OpenAI 연결에 성공했습니다.")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(isChecking = false, error = error.toConnectionMessage())
                }
            }
        }
    }

    class Factory(
        private val store: ApiKeyStore,
        private val vocabularyAiService: VocabularyAiService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(store, vocabularyAiService) as T
    }
}

private fun Exception.toConnectionMessage(): String = when (this) {
    OpenAiFailure.MissingKey -> "API 키를 먼저 저장하세요."
    OpenAiFailure.Unauthorized -> "API 키가 거부되었습니다. 키를 다시 확인하세요."
    OpenAiFailure.Forbidden -> "이 API 키로 OpenAI 모델에 접근할 수 없습니다."
    OpenAiFailure.BadRequest -> "OpenAI가 연결 확인 요청을 거부했습니다."
    OpenAiFailure.RateLimited -> "OpenAI 사용량 또는 결제 한도에 도달했습니다."
    OpenAiFailure.Server -> "OpenAI 서버에 문제가 발생했습니다. 잠시 후 다시 시도하세요."
    OpenAiFailure.Network -> "네트워크 연결을 확인하세요."
    OpenAiFailure.InvalidResponse -> "OpenAI 응답을 확인하지 못했습니다."
    OpenAiFailure.EmptyResult,
    OpenAiFailure.TooManyItems,
    -> "OpenAI 연결을 확인하지 못했습니다."
    else -> message ?: "OpenAI 연결을 확인하지 못했습니다."
}
