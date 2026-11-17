package app.frame.video.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF725B36), onPrimary = Color.White,
    secondary = Color(0xFF58665A), onSecondary = Color.White,
    background = Color(0xFFF7F5F0), surface = Color(0xFFF7F5F0),
    surfaceVariant = Color(0xFFEAE5DA), onSurface = Color(0xFF24231F)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFD7B987), onPrimary = Color(0xFF392B12),
    secondary = Color(0xFFB7C5B5), onSecondary = Color(0xFF263528),
    background = Color(0xFF171815), surface = Color(0xFF171815),
    surfaceVariant = Color(0xFF30312B), onSurface = Color(0xFFE8E5DC)
)

@Composable
fun FrameTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
