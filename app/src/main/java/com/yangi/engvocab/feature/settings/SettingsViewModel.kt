package com.yangi.engvocab.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yangi.engvocab.core.security.ApiKeyStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val input: String = "",
    val isConfigured: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class SettingsViewModel(
    private val apiKeyStore: ApiKeyStore,
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

    class Factory(private val store: ApiKeyStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(store) as T
    }
}
