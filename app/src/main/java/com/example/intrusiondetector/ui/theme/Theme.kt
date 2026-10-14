package com.example.intrusiondetector.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Прямокутні форми (0.dp) для всіх елементів інтерфейсу.
 */
val RectangularShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

/**
 * Моноширинний шрифт (Courier New / FontFamily.Monospace) для всіх стилів тексту.
 */
val CourierTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Monospace),
    displayMedium = TextStyle(fontFamily = FontFamily.Monospace),
    displaySmall = TextStyle(fontFamily = FontFamily.Monospace),
    headlineLarge = TextStyle(fontFamily = FontFamily.Monospace),
    headlineMedium = TextStyle(fontFamily = FontFamily.Monospace),
    headlineSmall = TextStyle(fontFamily = FontFamily.Monospace),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace),
    titleSmall = TextStyle(fontFamily = FontFamily.Monospace),
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace),
    labelLarge = TextStyle(fontFamily = FontFamily.Monospace),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace),
)

@Composable
fun IntrusionDetectorTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = CourierTypography,
        shapes = RectangularShapes,
        content = content,
    )
}
