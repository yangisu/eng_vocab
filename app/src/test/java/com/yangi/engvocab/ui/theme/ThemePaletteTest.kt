package com.yangi.engvocab.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePaletteTest {
    @Test
    fun usesRequestedBrandPalette() {
        assertEquals(Color(0xFF0E718C), EngVocabLightColors.primary)
        assertEquals(Color(0xFFE2BFD0), EngVocabLightColors.secondaryContainer)
    }

    @Test
    fun primaryAndBodyColorsMeetReadableContrast() {
        assertTrue(
            contrastRatio(
                EngVocabLightColors.onPrimary,
                EngVocabLightColors.primary,
            ) >= 4.5,
        )
        assertTrue(
            contrastRatio(
                EngVocabLightColors.onBackground,
                EngVocabLightColors.background,
            ) >= 4.5,
        )
    }
}

private fun contrastRatio(foreground: Color, background: Color): Double {
    val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
    val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: Color): Double {
    fun channel(value: Float): Double {
        val normalized = value.toDouble()
        return if (normalized <= 0.04045) {
            normalized / 12.92
        } else {
            Math.pow((normalized + 0.055) / 1.055, 2.4)
        }
    }
    return 0.2126 * channel(color.red) +
        0.7152 * channel(color.green) +
        0.0722 * channel(color.blue)
}
