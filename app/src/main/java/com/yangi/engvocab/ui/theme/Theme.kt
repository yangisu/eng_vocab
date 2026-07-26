package com.yangi.engvocab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val EngVocabLightColors = lightColorScheme(
    primary = Color(0xFF0E718C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F0F7),
    onPrimaryContainer = Color(0xFF063D4C),
    secondary = Color(0xFF8A4566),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2BFD0),
    onSecondaryContainer = Color(0xFF4A1D32),
    tertiary = Color(0xFF496B57),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD6ECDD),
    onTertiaryContainer = Color(0xFF173C27),
    background = Color(0xFFFFF9FB),
    onBackground = Color(0xFF17292F),
    surface = Color(0xFFFFFBFC),
    onSurface = Color(0xFF17292F),
    surfaceVariant = Color(0xFFF5E9EF),
    onSurfaceVariant = Color(0xFF53636A),
    outline = Color(0xFF78898F),
    outlineVariant = Color(0xFFD7C9CF),
    error = Color(0xFF9B4045),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF5A1018),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.35).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
)

@Composable
fun EngVocabTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EngVocabLightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
