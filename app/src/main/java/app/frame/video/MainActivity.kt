package app.frame.video

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.frame.video.ui.FrameApp
import app.frame.video.ui.theme.FrameTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FrameApplication).container
        setContent {
            val saved by container.preferences.darkMode.collectAsStateWithLifecycle(initialValue = null)
            val dark = saved ?: isSystemInDarkTheme()
            FrameTheme(darkTheme = dark) {
                FrameApp(
                    container = container,
                    darkTheme = dark,
                    onToggleTheme = { enabled -> lifecycleScope.launch { container.preferences.setDarkMode(enabled) } }
                )
            }
        }
    }
}
