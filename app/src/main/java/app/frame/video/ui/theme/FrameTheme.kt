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

// Swiss-inspired hierarchy with Frame's charcoal surfaces and restrained red accent.
private val LightColors = lightColorScheme(
    primary = Color(0xFFC62828), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6), onPrimaryContainer = Color(0xFF410006),
    secondary = Color(0xFF55565A), onSecondary = Color.White,
    background = Color(0xFFFAFAFA), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0F0F0), onSurface = Color(0xFF171717),
    onSurfaceVariant = Color(0xFF66666A), outline = Color(0xFFDADADA)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF514B), onPrimary = Color(0xFF2B0807),
    primaryContainer = Color(0xFF650F13), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFB8B8BC), onSecondary = Color(0xFF242428),
    background = Color(0xFF090909), surface = Color(0xFF101010),
    surfaceVariant = Color(0xFF202020), onSurface = Color(0xFFF1F1F1),
    onSurfaceVariant = Color(0xFFAAAAAE), outline = Color(0xFF38383B)
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
