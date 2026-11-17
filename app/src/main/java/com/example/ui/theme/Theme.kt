package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KMobileDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepObsidian,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = ElectricCyan,
    secondary = CobaltBlue,
    onSecondary = Color.White,
    secondaryContainer = CobaltContainerDark,
    onSecondaryContainer = Color(0xFFBFDBFE),
    tertiary = AmberGold,
    onTertiary = DeepObsidian,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Color(0xFFFDE68A),
    error = CoralRed,
    onError = Color.White,
    errorContainer = CoralContainerDark,
    onErrorContainer = Color(0xFFFECDD3),
    background = DeepObsidian,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = NavyOutline
)

private val KMobileLightColorScheme = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = Color(0xFF00382F),
    secondary = CobaltDark,
    onSecondary = Color.White,
    secondaryContainer = CobaltContainerLight,
    onSecondaryContainer = Color(0xFF1E3A8A),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Color(0xFF78350F),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = Color(0xFF881337),
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline
)

val KMobileShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KMobileDarkColorScheme else KMobileLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KMobileTypography,
        shapes = KMobileShapes,
        content = content
    )
}
