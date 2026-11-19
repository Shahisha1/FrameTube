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

// Frame keeps its red accent while using softer Material 3 surface layers.
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

@Composable
fun FrameTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) DarkColors else LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
