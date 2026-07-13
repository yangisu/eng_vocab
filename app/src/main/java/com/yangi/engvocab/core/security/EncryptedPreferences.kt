package com.yangi.engvocab.core.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.secureSettingsDataStore by preferencesDataStore(name = "secure_settings")

interface EncryptedPreferences {
    suspend fun readCiphertext(): String?
    suspend fun readIv(): String?
    suspend fun write(ciphertext: String, iv: String)
    suspend fun clear()
    fun observeConfigured(): Flow<Boolean>
}

class DataStoreEncryptedPreferences(
    private val dataStore: DataStore<Preferences>,
) : EncryptedPreferences {
    constructor(context: Context) : this(context.secureSettingsDataStore)

    private val safeData = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override suspend fun readCiphertext(): String? = safeData.first()[CIPHERTEXT]

    override suspend fun readIv(): String? = safeData.first()[IV]

    override suspend fun write(ciphertext: String, iv: String) {
        dataStore.edit { preferences ->
            preferences[CIPHERTEXT] = ciphertext
            preferences[IV] = iv
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(CIPHERTEXT)
            preferences.remove(IV)
        }
    }

    override fun observeConfigured(): Flow<Boolean> = safeData.map { preferences ->
        preferences[CIPHERTEXT] != null && preferences[IV] != null
    }

    private companion object {
        val CIPHERTEXT = stringPreferencesKey("openai_key_ciphertext")
        val IV = stringPreferencesKey("openai_key_iv")
    }
}
