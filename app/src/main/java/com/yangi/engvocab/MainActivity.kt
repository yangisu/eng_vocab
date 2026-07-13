package com.yangi.engvocab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yangi.engvocab.ui.theme.EngVocabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EngVocabTheme {
                EngVocabApp((application as VocabularyApplication).container)
            }
        }
    }
}

