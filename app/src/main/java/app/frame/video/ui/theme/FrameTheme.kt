package app.frame.video.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// YouTube-like neutral surfaces keep content prominent; Frame's red is reserved for actions.
private val LightColors = lightColorScheme(
    primary = Color(0xFFCC0000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1DE),
    onPrimaryContainer = Color(0xFF410000),
    secondary = Color(0xFF606060),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F2),
    onSurface = Color(0xFF0F0F0F),
    onSurfaceVariant = Color(0xFF606060),
    outline = Color(0xFF909090),
    outlineVariant = Color(0xFFE5E5E5)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF0033),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A1019),
    onPrimaryContainer = Color(0xFFFFD9DE),
    secondary = Color(0xFFAAAAAA),
    onSecondary = Color(0xFF202020),
    background = Color(0xFF0F0F0F),
    surface = Color(0xFF0F0F0F),
    surfaceVariant = Color(0xFF272727),
    onSurface = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF717171),
    outlineVariant = Color(0xFF303030)
)

@Composable
fun FrameTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    accentPalette: String = "red",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val base = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) DarkColors else LightColors

    val colors = when (accentPalette) {
        "blue" -> base.copy(primary = if (darkTheme) Color(0xFF8AB4F8) else Color(0xFF065FD4))
        "violet" -> base.copy(primary = if (darkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4))
        "green" -> base.copy(primary = if (darkTheme) Color(0xFF9CD67D) else Color(0xFF386A20))
        else -> base
    }
    MaterialTheme(colorScheme = colors, content = content)
}
