package com.yangi.engvocab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColors = lightColorScheme(
    primary = Color(0xFF365F91),
    onPrimary = Color.White,
    secondary = Color(0xFF4E616F),
    background = Color(0xFFF8F9FC),
    surface = Color(0xFFF8F9FC),
)

@Composable
fun EngVocabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content,
    )
}
