package app.frame.video.ui.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.domain.provider.VideoProvider
import app.frame.video.domain.provider.ChannelProvider
import app.frame.video.domain.provider.ChannelProfile
import app.frame.video.data.worker.MetadataEnrichmentScheduler
import app.frame.video.core.provider.PlaybackState
import app.frame.video.core.provider.ProviderManager
import app.frame.video.data.importer.TakeoutImportResult
import app.frame.video.data.importer.TakeoutImporter
import app.frame.video.data.importer.TakeoutParser
import app.frame.video.data.local.VideoEntity
import app.frame.video.data.local.ChannelEntity
import app.frame.video.data.local.SearchHistoryEntity
import app.frame.video.data.local.PlaylistEntity
import app.frame.video.domain.LibraryCounts
import app.frame.video.domain.LibraryRepository
import app.frame.video.domain.PlaybackProgress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class LibraryViewModel(private val repo: LibraryRepository) : ViewModel() {
    private companion object {
        val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
        val VIDEO_URL_ID = Regex("(?:v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})")
    }
    val history = repo.pagedHistory().cachedIn(viewModelScope)
    val liked = repo.pagedLiked().cachedIn(viewModelScope)
    val channels = repo.pagedChannels().cachedIn(viewModelScope)
    val playlists = repo.pagedPlaylists().cachedIn(viewModelScope)
    val counts: StateFlow<LibraryCounts> =
        repo.observeCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryCounts())
    val recent: StateFlow<List<VideoEntity>> =
        repo.observeRecentHistory(10).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playlistVideos(id: String) = repo.pagedPlaylistVideos(id).cachedIn(viewModelScope)
    fun playlist(id: String) = repo.observePlaylist(id)
    fun createPlaylist(title: String, description: String, onDone: () -> Unit, onError: (String) -> Unit) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) { onError("Playlist name cannot be empty."); return }
        viewModelScope.launch {
            try {
                repo.savePlaylist(PlaylistEntity(UUID.randomUUID().toString(), cleanTitle, description.trim().ifBlank { null }))
                onDone()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError(error.message ?: "Could not create playlist.") }
        }
    }

    fun updatePlaylist(id: String, title: String, description: String, onDone: () -> Unit, onError: (String) -> Unit) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) { onError("Playlist name cannot be empty."); return }
        viewModelScope.launch {
            try {
                repo.updatePlaylist(id, cleanTitle, description.trim().ifBlank { null })
                onDone()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError(error.message ?: "Could not update playlist.") }
        }
    }

    fun deletePlaylist(id: String, onDone: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try { repo.deletePlaylist(id); onDone() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError(error.message ?: "Could not delete playlist.") }
        }
    }

    fun addVideoToPlaylist(playlistId: String, input: String, onDone: (Boolean) -> Unit, onError: (String) -> Unit) {
        val value = input.trim()
        val videoId = if (VIDEO_ID.matches(value)) value else VIDEO_URL_ID.find(value)?.groupValues?.getOrNull(1)
        if (videoId == null) { onError("Enter an 11-character video ID or a YouTube video URL."); return }
        viewModelScope.launch {
            try {
                repo.ensureVideo(videoId, null, null)
                onDone(repo.addVideoToPlaylist(playlistId, videoId))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError(error.message ?: "Could not add this video.") }
        }
    }

    fun removeVideoFromPlaylist(playlistId: String, videoId: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try { repo.removeVideoFromPlaylist(playlistId, videoId) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { onError(error.message ?: "Could not remove this video.") }
        }
    }

    fun deleteAll(onDone: () -> Unit) { viewModelScope.launch { repo.deleteAll(); onDone() } }
}

class SearchViewModel(
    private val repo: LibraryRepository,
    private val onlineProvider: VideoProvider
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _submittedQuery = MutableStateFlow("")
    val submittedQuery: StateFlow<String> = _submittedQuery
    private val _filter = MutableStateFlow("videos")
    val filter: StateFlow<String> = _filter
    private val _onlineResults = MutableStateFlow(app.frame.video.domain.provider.VideoSearchResult(emptyList(), null))
    val onlineResults: StateFlow<app.frame.video.domain.provider.VideoSearchResult> = _onlineResults
    private val _onlineLoading = MutableStateFlow(false)
    val onlineLoading: StateFlow<Boolean> = _onlineLoading
    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore
    private val _onlineError = MutableStateFlow<String?>(null)
    val onlineError: StateFlow<String?> = _onlineError
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions
    val recentSearches: StateFlow<List<SearchHistoryEntity>> =
        repo.observeSearchHistory(8).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var searchJob: Job? = null
    private var suggestionJob: Job? = null
    private var requestGeneration: Int = 0

    fun setQuery(q: String) {
        _query.value = q
        if (q.trim() != _submittedQuery.value) {
            requestGeneration += 1
            searchJob?.cancel()
            _onlineResults.value = app.frame.video.domain.provider.VideoSearchResult(emptyList(), null)
            _onlineError.value = null
            _onlineLoading.value = false
        }
        suggestionJob?.cancel()
        _suggestions.value = emptyList()
        val term = q.trim()
        if (term.length >= 2 && term != _submittedQuery.value) {
            suggestionJob = viewModelScope.launch {
                delay(280)
                try {
                    _suggestions.value = onlineProvider.suggestions(term)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    _suggestions.value = emptyList()
                }
            }
        }
    }

    fun selectFilter(value: String) {
        if (value !in setOf("videos", "channels", "playlists")) return
        if (_filter.value == value) return
        _filter.value = value
        if (_submittedQuery.value.isNotBlank() && _query.value.trim() == _submittedQuery.value) searchNow()
    }

    fun useSuggestion(value: String) {
        _query.value = value
        _suggestions.value = emptyList()
        searchNow()
    }

    fun searchNow() {
        val term = _query.value.trim()
        if (term.isBlank()) return
        suggestionJob?.cancel()
        _suggestions.value = emptyList()
        val generation = ++requestGeneration
        val selectedFilter = _filter.value
        searchJob?.cancel()
        _submittedQuery.value = term
        _onlineResults.value = app.frame.video.domain.provider.VideoSearchResult(emptyList(), null)
        _onlineError.value = null
        searchJob = viewModelScope.launch {
            _onlineLoading.value = true
            try {
                repo.recordSearchQuery(term)
                val result = onlineProvider.searchVideos(term, contentFilter = selectedFilter)
                if (requestGeneration == generation && _submittedQuery.value == term && _filter.value == selectedFilter) {
                    _onlineResults.value = result
                    _onlineError.value = result.errorMessage
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (requestGeneration == generation) _onlineError.value = "Search failed: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                if (requestGeneration == generation) _onlineLoading.value = false
            }
        }
    }

    fun loadMore() {
        val token = _onlineResults.value.nextPageToken ?: return
        if (_loadingMore.value || _onlineLoading.value) return
        val generation = requestGeneration
        val term = _submittedQuery.value
        val selectedFilter = _filter.value
        viewModelScope.launch {
            _loadingMore.value = true
            try {
                val next = onlineProvider.searchVideos(term, token, selectedFilter)
                if (generation == requestGeneration && term == _submittedQuery.value && selectedFilter == _filter.value) {
                    val old = _onlineResults.value
                    _onlineResults.value = old.copy(
                        items = (old.items + next.items).distinctBy { it.videoId },
                        channels = (old.channels + next.channels).distinctBy { it.url },
                        playlists = (old.playlists + next.playlists).distinctBy { it.url },
                        nextPageToken = next.nextPageToken,
                        errorMessage = next.errorMessage
                    )
                    if (next.errorMessage != null) _onlineError.value = next.errorMessage
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == requestGeneration) _onlineError.value = "Could not load more results. Try again."
            } finally {
                _loadingMore.value = false
            }
        }
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val results: Flow<PagingData<VideoEntity>> = _query
        .debounce(200)
        .map { it.trim() }
        .distinctUntilChanged()
        .flatMapLatest { if (it.isEmpty()) flowOf(PagingData.empty<VideoEntity>()) else repo.searchVideos(it) }
        .cachedIn(viewModelScope)
}
data class SubscriptionFeedUiState(
    val loading: Boolean = false,
    val videos: List<VideoMetadataResult> = emptyList(),
    val error: String? = null,
    val discoveredVideos: List<VideoMetadataResult> = emptyList(),
    val discoveryLoading: Boolean = false,
    val discoveryError: String? = null,
    val discoveryContext: String? = null,
    val shortsVideos: List<VideoMetadataResult> = emptyList(),
    val shortsLoading: Boolean = false,
    val shortsError: String? = null
)

class DiscoveryViewModel(
    private val repo: LibraryRepository,
    private val channelProvider: ChannelProvider,
    private val videoProvider: VideoProvider
) : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionFeedUiState())
    val state: StateFlow<SubscriptionFeedUiState> = _state

    init {
        viewModelScope.launch {
            repo.observeSubscribedChannels().collectLatest { channels -> loadFeed(channels) }
        }
        viewModelScope.launch {
            repo.observeRecentHistory(12).collectLatest { history -> loadDiscovery(history) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loadFeed(repo.observeSubscribedChannels().first())
            loadDiscovery(repo.observeRecentHistory(12).first())
        }
    }

    private suspend fun loadDiscovery(history: List<VideoEntity>) {
        val titles = history.mapNotNull { it.title.takeIf(String::isNotBlank) }
        val queries = app.frame.video.domain.HomeRecommendationEngine.buildQueries(titles)
        val personalQueries = queries.filterNot { it in setOf("popular videos", "trending videos") }
        val context = personalQueries.firstOrNull()
        _state.value = _state.value.copy(discoveryLoading = true, discoveryError = null)

        try {
            // Bound concurrency: several independent topic searches run together,
            // without opening an unbounded number of extractor requests.
            val gate = Semaphore(3)
            val responses = coroutineScope {
                queries.map { query ->
                    async {
                        gate.withPermit {
                            val response = withTimeoutOrNull(12_000) {
                                videoProvider.searchVideos(query)
                            }
                            query to response
                        }
                    }
                }.awaitAll()
            }

            val videos = app.frame.video.domain.HomeRecommendationEngine.interleave(
                batches = responses.mapNotNull { (_, response) -> response?.items },
                limit = 36
            )
            val error = responses.mapNotNull { (_, response) -> response?.errorMessage }.firstOrNull()
            val timedOut = responses.any { it.second == null }

            _state.value = _state.value.copy(
                discoveryLoading = false,
                discoveredVideos = videos,
                discoveryError = if (videos.isEmpty()) {
                    error ?: if (timedOut) {
                        "Discovery took too long. Check your connection and try again."
                    } else {
                        "No public videos were returned. Try refreshing or search for a topic."
                    }
                } else null,
                discoveryContext = context?.let { "Based on: $it" } ?: "Picked for you"
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                discoveryLoading = false,
                discoveryError = "Could not load discovery videos. Check your connection and try again."
            )
        }
    }

    private suspend fun loadFeed(channels: List<ChannelEntity>) {
        if (channels.isEmpty()) {
            _state.value = _state.value.copy(
                loading = false,
                videos = emptyList(),
                error = null,
                shortsLoading = true
            )
            loadFallbackShorts()
            return
        }
        _state.value = _state.value.copy(loading = true, error = null)
        val gate = Semaphore(3)
        val pages = coroutineScope {
            channels.take(12).map { channel ->
                async {
                    gate.withPermit {
                        val url = channel.channelUrl?.takeIf { it.isNotBlank() }
                            ?: "https://www.youtube.com/channel/" + channel.id
                        try {
                            withTimeoutOrNull(20_000) { channelProvider.getChannelPage(url) }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }.awaitAll()
        }
        val videos = pages.flatMap { it?.videos.orEmpty() }.distinctBy { it.videoId }.take(60)
        val shorts = pages.flatMap { it?.shorts.orEmpty() }.distinctBy { it.videoId }.take(60)
        _state.value = _state.value.copy(
            loading = false,
            videos = videos,
            error = if (videos.isEmpty()) "Could not load public videos from your saved channels. Check your connection and try again." else null,
            shortsVideos = shorts,
            shortsLoading = shorts.isEmpty(),
            shortsError = null
        )
        if (shorts.isEmpty()) loadFallbackShorts()
    }

    /**
     * Some channels do not expose a Shorts tab. In that case, use public
     * search results only when NewPipe explicitly identifies them as Shorts.
     */
    private suspend fun loadFallbackShorts() {
        _state.value = _state.value.copy(shortsLoading = true, shortsError = null)
        try {
            val result = withTimeoutOrNull(15_000) { videoProvider.searchVideos("shorts") }
            val shorts = result?.items.orEmpty()
                .filter { it.isShortFormContent }
                .distinctBy { it.videoId }
                .take(40)
            _state.value = _state.value.copy(
                shortsLoading = false,
                shortsVideos = shorts,
                shortsError = if (shorts.isEmpty()) {
                    result?.errorMessage ?: "No videos were identified as Shorts. Try again later."
                } else null
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                shortsLoading = false,
                shortsError = "Could not load Shorts. Check your connection and try again."
            )
        }
    }
}

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data class Reading(val sourceName: String) : ImportUiState
    data class Preview(val sourceName: String, val parsed: TakeoutParser.Parsed) : ImportUiState
    data object Saving : ImportUiState
    data class Done(val result: TakeoutImportResult) : ImportUiState
    data class Failed(val message: String) : ImportUiState
}

class ImportViewModel(
    private val importer: TakeoutImporter,
    private val contentResolver: ContentResolver,
    private val metadataEnrichment: MetadataEnrichmentScheduler
) : ViewModel() {
    private val _state = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val state: StateFlow<ImportUiState> = _state
    private var job: Job? = null

    fun onPicked(uri: Uri, sourceName: String) {
        job?.cancel()
        _state.value = ImportUiState.Reading(sourceName)
        job = viewModelScope.launch {
            try {
                val parsed = importer.parse { contentResolver.openInputStream(uri) ?: error("Could not open the selected file.") }
                _state.value = if (parsed.result.filesFound.isEmpty())
                    ImportUiState.Failed("No supported YouTube files were found in this archive. Frame looks for subscriptions, playlists and activity history files.")
                else ImportUiState.Preview(sourceName, parsed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ImportUiState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    /** Safe while reading (nothing written yet). Not offered while saving, because saving is one atomic transaction. */
    fun cancel() {
        if (_state.value is ImportUiState.Reading || _state.value is ImportUiState.Preview) {
            job?.cancel()
            _state.value = ImportUiState.Idle
        }
    }

    fun confirm() {
        val s = _state.value as? ImportUiState.Preview ?: return
        _state.value = ImportUiState.Saving
        job = viewModelScope.launch {
            try {
                val result = importer.commit(s.parsed, s.sourceName)
                metadataEnrichment.schedule()
                _state.value = ImportUiState.Done(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = ImportUiState.Failed("Saving failed, nothing was changed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun reset() { _state.value = ImportUiState.Idle }
}

sealed interface PlayerUiState {
    data object Loading : PlayerUiState
    data class Ready(val source: PlaybackResolution.Ready) : PlayerUiState
    data class Failed(val state: PlaybackState, val message: String) : PlayerUiState
}

class PlayerViewModel(
    private val videoId: String,
    private val providers: ProviderManager,
    private val library: LibraryRepository
) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state
    var positionMs: Long = 0
    private var historyEntryId: Long? = null

    init { load() }

    fun load() {
        _state.value = PlayerUiState.Loading
        viewModelScope.launch {
            val provider = providers.playbackProvider(videoId)
            if (provider == null) {
                _state.value = PlayerUiState.Failed(PlaybackState.UNSUPPORTED, "No playback source is available for this video.")
                return@launch
            }
            when (val result = provider.resolve(videoId)) {
                is PlaybackResolution.Ready -> {
                    if (positionMs == 0L) positionMs = PlaybackProgress.resumePositionMs(savedPositionSeconds())
                    recordPlayOnce()
                    _state.value = PlayerUiState.Ready(result)
                }
                is PlaybackResolution.Failed -> _state.value = PlayerUiState.Failed(result.state, result.message)
            }
        }
    }

    fun savePosition(currentPositionMs: Long, durationMs: Long, ended: Boolean = false) {
        val seconds = PlaybackProgress.secondsToSave(currentPositionMs, durationMs, ended)
        if (seconds == 0L) positionMs = 0L
        val entryId = historyEntryId ?: return
        viewModelScope.launch {
            withContext(NonCancellable) {
                try { library.updatePlayPosition(entryId, seconds) }
                catch (_: Exception) { /* Progress must never interrupt playback. */ }
            }
        }
    }

    fun failPlayback(message: String) { _state.value = PlayerUiState.Failed(PlaybackState.NETWORK_ERROR, message) }

    private suspend fun savedPositionSeconds(): Long? = try {
        library.lastPositionSeconds(videoId)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun recordPlayOnce() {
        if (historyEntryId != null) return
        try {
            library.ensureVideo(videoId, null, null)
            historyEntryId = library.recordPlay(videoId, positionMs / 1_000L)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Playback remains available if local history cannot be written.
        }
    }
}

data class ChannelPageUiState(
    val loading: Boolean = true,
    val profile: ChannelProfile? = null,
    val videos: List<VideoMetadataResult> = emptyList(),
    val error: String? = null
)

class ChannelDetailViewModel(
    private val channelId: String,
    private val library: LibraryRepository,
    private val provider: ChannelProvider
) : ViewModel() {
    private val _state = MutableStateFlow(ChannelPageUiState())
    val state: StateFlow<ChannelPageUiState> = _state
    val subscribed: StateFlow<Boolean> = library.observeIsSubscribed(channelId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init { load() }

    fun load() {
        _state.value = ChannelPageUiState(loading = true)
        viewModelScope.launch {
            try {
                val saved = library.getChannel(channelId)
                val url = saved?.channelUrl?.takeIf { it.isNotBlank() }
                    ?: if (channelId.startsWith("https://") || channelId.startsWith("http://")) channelId
                    else "https://www.youtube.com/channel/$channelId"
                val page = provider.getChannelPage(url)
                val profile = page.profile
                library.saveChannel(
                    ChannelEntity(
                        id = profile.id,
                        title = profile.name,
                        channelUrl = profile.url,
                        thumbnailUrl = profile.avatarUrl,
                        importedAt = saved?.importedAt
                    )
                )
                _state.value = ChannelPageUiState(profile = profile, videos = page.videos)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _state.value = ChannelPageUiState(
                    loading = false,
                    error = error.message?.takeIf { it.isNotBlank() }
                        ?: "Could not load this channel. Check your connection and try again."
                )
            }
        }
    }

    fun toggleSubscription() {
        viewModelScope.launch {
            try {
                val next = !subscribed.value
                library.setSubscribed(channelId, next)
                val profileId = state.value.profile?.id
                if (!profileId.isNullOrBlank() && profileId != channelId) library.setSubscribed(profileId, next)
            }
            catch (_: Exception) { /* Subscription state remains backed by Room. */ }
        }
    }
}
