package com.yangi.engvocab

import android.content.Context
import androidx.room.Room
import com.yangi.engvocab.core.database.AppDatabase
import com.yangi.engvocab.core.image.ImagePreprocessor
import com.yangi.engvocab.core.image.TempImageStore
import com.yangi.engvocab.core.openai.OpenAiClient
import com.yangi.engvocab.core.openai.OpenAiVocabularyService
import com.yangi.engvocab.core.openai.VocabularyAiService
import com.yangi.engvocab.core.repository.RoomVocabularyRepository
import com.yangi.engvocab.core.repository.VocabularyRepository
import com.yangi.engvocab.core.security.AndroidApiKeyCipher
import com.yangi.engvocab.core.security.ApiKeyStore
import com.yangi.engvocab.core.security.DataStoreEncryptedPreferences
import java.time.Clock
import kotlinx.serialization.json.Json

interface AppContainer {
    val vocabularyRepository: VocabularyRepository
    val apiKeyStore: ApiKeyStore
    val openAiService: VocabularyAiService
    val tempImageStore: TempImageStore
    val imagePreprocessor: ImagePreprocessor
    val clock: Clock
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "eng_vocab.db",
    ).build()

    override val vocabularyRepository: VocabularyRepository = RoomVocabularyRepository(database)
    override val apiKeyStore = ApiKeyStore(
        preferences = DataStoreEncryptedPreferences(appContext),
        cipher = AndroidApiKeyCipher(),
    )
    override val openAiService: VocabularyAiService = OpenAiVocabularyService(
        client = OpenAiClient(),
        apiKeyProvider = apiKeyStore,
        json = Json { ignoreUnknownKeys = true; explicitNulls = false },
    )
    override val tempImageStore = TempImageStore(appContext)
    override val imagePreprocessor = ImagePreprocessor(appContext)
    override val clock: Clock = Clock.systemDefaultZone()
}
