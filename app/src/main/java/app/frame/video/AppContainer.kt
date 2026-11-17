package app.frame.video

import android.app.Application
import android.content.Context
import android.content.ContentResolver
import app.frame.video.core.datastore.FramePreferences
import app.frame.video.core.provider.ProviderManager
import app.frame.video.core.provider.FallbackPlaybackProvider
import app.frame.video.core.provider.newpipe.NewPipePlaybackProvider
import app.frame.video.data.remote.newpipe.NewPipeVideoProvider
import app.frame.video.data.worker.MetadataEnrichmentScheduler
import app.frame.video.core.provider.piped.PipedApi
import app.frame.video.core.provider.piped.PipedPlaybackProvider
import app.frame.video.data.importer.TakeoutImporter
import app.frame.video.data.local.FrameDatabaseProvider
import app.frame.video.data.repository.RoomLibraryRepository
import app.frame.video.domain.LibraryRepository
import kotlinx.coroutines.flow.first

/** Manual dependency container. Everything is lazy so cold start stays fast. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val contentResolver: ContentResolver get() = appContext.contentResolver
    val database by lazy { FrameDatabaseProvider.get(appContext) }
    val preferences by lazy { FramePreferences(appContext) }
    val library: LibraryRepository by lazy { RoomLibraryRepository(database.libraryDao()) }
    val importer by lazy { TakeoutImporter(database) }
    val metadataEnrichmentScheduler by lazy { MetadataEnrichmentScheduler(appContext) }
    val providerManager by lazy {
        ProviderManager(
            metadataProviders = listOf(NewPipeVideoProvider()),
            playbackProviders = listOf(
                FallbackPlaybackProvider(
                    listOf(
                        NewPipePlaybackProvider(),
                        PipedPlaybackProvider(PipedApi()) { preferences.pipedConfig.first() }
                    )
                )
            )
        )
    }
}

class FrameApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        container.metadataEnrichmentScheduler.schedule()
    }
}
