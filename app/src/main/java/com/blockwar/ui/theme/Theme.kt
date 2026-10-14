package com.blockwar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ArenaStyle.Cyan,
    secondary = ArenaStyle.Purple,
    tertiary = ArenaStyle.Gold,
    background = ArenaStyle.Background,
    surface = ArenaStyle.SurfaceLow,
    surfaceVariant = ArenaStyle.SurfaceHigh,
    onPrimary = ArenaStyle.Recessed,
    onSecondary = ArenaStyle.Recessed,
    onTertiary = ArenaStyle.Recessed,
    onBackground = ArenaStyle.Text,
    onSurface = ArenaStyle.Text,
    onSurfaceVariant = ArenaStyle.Muted,
    outline = ArenaStyle.Outline,
    outlineVariant = ArenaStyle.Outline.copy(alpha = .55f),
    error = ArenaStyle.Error
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = androidx.compose.material3.Shapes(
            small = androidx.compose.foundation.shape.CutCornerShape(6.dp),
            medium = androidx.compose.foundation.shape.CutCornerShape(8.dp),
            large = androidx.compose.foundation.shape.CutCornerShape(12.dp)
        ),
        content = content
    )
}
