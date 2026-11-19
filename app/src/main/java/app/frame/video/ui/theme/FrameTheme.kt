package app.frame.video.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Frame keeps its own red identity by default, with optional restrained accent palettes.
private val LightColors = lightColorScheme(
    primary = Color(0xFFB3261E), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5), onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF5C5F63), onSecondary = Color.White,
    background = Color(0xFFF7F7F8), surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFECEDEF), onSurface = Color(0xFF191B1E),
    onSurfaceVariant = Color(0xFF5F6267), outline = Color(0xFF777A80),
    outlineVariant = Color(0xFFD0D2D6)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AA), onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A), onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFC4C7CC), onSecondary = Color(0xFF292C30),
    background = Color(0xFF101114), surface = Color(0xFF17191D),
    surfaceVariant = Color(0xFF24272C), onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFC2C6CC), outline = Color(0xFF8C9097),
    outlineVariant = Color(0xFF42464D)
)

private fun ColorScheme.withAccent(name: String, dark: Boolean): ColorScheme {
    val values = when (name) {
        "blue" -> if (dark) listOf(Color(0xFFB1C5FF), Color(0xFF002E69), Color(0xFF004494), Color(0xFFD8E2FF))
            else listOf(Color(0xFF005AC1), Color.White, Color(0xFFD8E2FF), Color(0xFF001A41))
        "violet" -> if (dark) listOf(Color(0xFFD0BCFF), Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF))
            else listOf(Color(0xFF6750A4), Color.White, Color(0xFFEADDFF), Color(0xFF21005D))
        "green" -> if (dark) listOf(Color(0xFF9CD67D), Color(0xFF1C3709), Color(0xFF2B5016), Color(0xFFB8F397))
            else listOf(Color(0xFF386A20), Color.White, Color(0xFFB8F397), Color(0xFF0D2000))
        else -> return this
    }
    return copy(
        primary = values[0],
        onPrimary = values[1],
        primaryContainer = values[2],
        onPrimaryContainer = values[3]
    )
}

@Composable
fun FrameTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    accentPalette: String = "red",
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        (if (darkTheme) DarkColors else LightColors).withAccent(accentPalette, darkTheme)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
