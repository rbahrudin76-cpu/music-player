package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = WarmAmber,
    onSecondary = ObsidianBg,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = WarmAmber,
    tertiary = NeonEmerald,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = PearlText,
    surface = ObsidianSurface,
    onSurface = PearlText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SilverText,
    surfaceContainer = ObsidianSurfaceVariant,
    surfaceContainerHigh = ObsidianCardElevated,
    outline = SlateBorder,
    outlineVariant = SlateBorder,
    error = CoralRose,
    onError = Color.White
)

private val StudioLightColorScheme = lightColorScheme(
    primary = DaylightPrimary,
    onPrimary = Color.White,
    primaryContainer = DaylightPrimaryContainer,
    onPrimaryContainer = DaylightPrimary,
    secondary = DaylightSecondary,
    onSecondary = Color.White,
    secondaryContainer = DaylightSecondaryContainer,
    onSecondaryContainer = DaylightSecondary,
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = DaylightBg,
    onBackground = InkText,
    surface = DaylightSurface,
    onSurface = InkText,
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = SlateText,
    surfaceContainer = DaylightSurfaceVariant,
    surfaceContainerHigh = Color.White,
    outline = DaylightBorder,
    outlineVariant = DaylightBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

val AuraPlayShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AuraPlayTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AuraPlayTypography,
        shapes = AuraPlayShapes,
        content = content
    )
}
