package com.yangi.engvocab

import android.app.Application

open class VocabularyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = createContainer()
        runCatching { container.tempImageStore.cleanupOlderThan() }
    }

    protected open fun createContainer(): AppContainer = DefaultAppContainer(this)
}

