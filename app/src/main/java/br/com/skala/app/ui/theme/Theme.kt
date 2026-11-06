package br.com.skala.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkalaColorScheme = lightColorScheme(
    primary = SkalaColors.Rose,
    onPrimary = Color.White,
    secondary = SkalaColors.Wine,
    onSecondary = Color.White,
    background = SkalaColors.Ivory,
    onBackground = SkalaColors.Wine,
    surface = Color.White,
    onSurface = SkalaColors.Wine,
    onSurfaceVariant = SkalaColors.Muted,
    outline = SkalaColors.Border,
    outlineVariant = SkalaColors.CardBorder,
    error = SkalaColors.Error,
)

/** O app usa sempre o tema claro, como definido no design. */
@Composable
fun SkalaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkalaColorScheme,
        typography = SkalaTypography,
        content = content,
    )
}
