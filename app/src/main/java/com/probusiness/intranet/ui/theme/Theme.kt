package com.probusiness.intranet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Orange600,
    onPrimary = Color.White,
    primaryContainer = Orange100,
    onPrimaryContainer = Orange900,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Slate800,
    tertiary = Sky500,
    onTertiary = Color.White,
    tertiaryContainer = Sky50,
    onTertiaryContainer = Sky800,
    error = Red600,
    onError = Color.White,
    errorContainer = Red50,
    onErrorContainer = Red800,
    background = AppBackgroundLight,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200,
)

private val DarkColors = darkColorScheme(
    primary = Orange400,
    onPrimary = Orange900,
    primaryContainer = Orange800,
    onPrimaryContainer = Orange100,
    secondary = Slate300,
    onSecondary = Slate900,
    secondaryContainer = Slate700,
    onSecondaryContainer = Slate100,
    tertiary = Sky300,
    onTertiary = Sky800,
    tertiaryContainer = Sky800,
    onTertiaryContainer = Sky50,
    error = Red200,
    onError = Red800,
    errorContainer = Red800,
    onErrorContainer = Red50,
    background = AppBackgroundDark,
    onBackground = Slate100,
    surface = SurfaceDarkAlt,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate300,
    outline = Slate600,
    outlineVariant = Slate700,
)

private val IntranetShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun IntranetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IntranetTypography,
        shapes = IntranetShapes,
        content = content,
    )
}
