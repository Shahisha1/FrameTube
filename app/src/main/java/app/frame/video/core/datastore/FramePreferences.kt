package app.frame.video.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.frameDataStore by preferencesDataStore(name = "frame_preferences")

data class PipedConfig(val enabled: Boolean, val instanceUrl: String)

class FramePreferences(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val pipedEnabledKey = booleanPreferencesKey("piped_enabled")
    private val pipedUrlKey = stringPreferencesKey("piped_instance_url")
    private val sponsorBlockEnabledKey = booleanPreferencesKey("sponsorblock_enabled")
    private val sponsorBlockCategoriesKey = stringSetPreferencesKey("sponsorblock_categories")
    private val hideShortsKey = booleanPreferencesKey("feed_hide_shorts")
    private val mutedKeywordsKey = stringPreferencesKey("feed_muted_keywords")
    private val maxPerChannelKey = intPreferencesKey("feed_max_per_channel")

    val darkMode: Flow<Boolean?> = context.frameDataStore.data.map { it[darkModeKey] }
    val dynamicColor: Flow<Boolean> = context.frameDataStore.data.map { it[dynamicColorKey] ?: false }
    val pipedConfig: Flow<PipedConfig> = context.frameDataStore.data.map {
        PipedConfig(it[pipedEnabledKey] ?: false, it[pipedUrlKey].orEmpty())
    }
    val sponsorBlockEnabled: Flow<Boolean> = context.frameDataStore.data.map { it[sponsorBlockEnabledKey] ?: false }
    val sponsorBlockCategories: Flow<Set<String>> = context.frameDataStore.data.map {
        it[sponsorBlockCategoriesKey] ?: DEFAULT_SPONSORBLOCK_CATEGORIES
    }
    val hideShorts: Flow<Boolean> = context.frameDataStore.data.map { it[hideShortsKey] ?: false }
    val mutedKeywords: Flow<String> = context.frameDataStore.data.map { it[mutedKeywordsKey].orEmpty() }
    val maxPerChannel: Flow<Int> = context.frameDataStore.data.map { (it[maxPerChannelKey] ?: 3).coerceIn(1, 10) }

    suspend fun setDarkMode(enabled: Boolean) {
        context.frameDataStore.edit { it[darkModeKey] = enabled }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.frameDataStore.edit { it[dynamicColorKey] = enabled }
    }

    suspend fun setPiped(enabled: Boolean, instanceUrl: String) {
        context.frameDataStore.edit {
            it[pipedEnabledKey] = enabled
            it[pipedUrlKey] = instanceUrl
        }
    }

    suspend fun setSponsorBlock(enabled: Boolean, categories: Set<String>) {
        context.frameDataStore.edit {
            it[sponsorBlockEnabledKey] = enabled
            it[sponsorBlockCategoriesKey] = categories.intersect(SPONSORBLOCK_CATEGORIES)
        }
    }

    suspend fun setFeedQuality(hideShorts: Boolean, mutedKeywords: String, maxPerChannel: Int) {
        context.frameDataStore.edit {
            it[hideShortsKey] = hideShorts
            it[mutedKeywordsKey] = mutedKeywords.take(500)
            it[maxPerChannelKey] = maxPerChannel.coerceIn(1, 10)
        }
    }

    companion object {
        val SPONSORBLOCK_CATEGORIES = setOf(
            "sponsor", "selfpromo", "interaction", "intro", "outro",
            "preview", "music_offtopic", "filler"
        )
        val DEFAULT_SPONSORBLOCK_CATEGORIES = setOf("sponsor", "selfpromo", "interaction", "intro", "outro")
    }
}
