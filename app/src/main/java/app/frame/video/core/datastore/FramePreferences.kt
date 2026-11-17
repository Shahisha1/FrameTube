package app.frame.video.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.frameDataStore by preferencesDataStore(name = "frame_preferences")

data class PipedConfig(val enabled: Boolean, val instanceUrl: String)

class FramePreferences(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val pipedEnabledKey = booleanPreferencesKey("piped_enabled")
    private val pipedUrlKey = stringPreferencesKey("piped_instance_url")

    val darkMode: Flow<Boolean?> = context.frameDataStore.data.map { it[darkModeKey] }
    val pipedConfig: Flow<PipedConfig> = context.frameDataStore.data.map {
        PipedConfig(it[pipedEnabledKey] ?: false, it[pipedUrlKey].orEmpty())
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.frameDataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setPiped(enabled: Boolean, instanceUrl: String) {
        context.frameDataStore.edit {
            it[pipedEnabledKey] = enabled
            it[pipedUrlKey] = instanceUrl
        }
    }
}
