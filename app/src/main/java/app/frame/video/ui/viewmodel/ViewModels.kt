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
import app.frame.video.data.worker.MetadataEnrichmentScheduler
import app.frame.video.core.provider.PlaybackState
import app.frame.video.core.provider.ProviderManager
import app.frame.video.data.importer.TakeoutImportResult
import app.frame.video.data.importer.TakeoutImporter
import app.frame.video.data.importer.TakeoutParser
import app.frame.video.data.local.VideoEntity
import app.frame.video.domain.LibraryCounts
import app.frame.video.domain.LibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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

class LibraryViewModel(private val repo: LibraryRepository) : ViewModel() {
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
    fun deleteAll(onDone: () -> Unit) { viewModelScope.launch { repo.deleteAll(); onDone() } }
}

class SearchViewModel(
    repo: LibraryRepository,
    private val onlineProvider: VideoProvider
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query
    private val _submittedQuery = MutableStateFlow("")
    val submittedQuery: StateFlow<String> = _submittedQuery
    private val _onlineResults = MutableStateFlow<List<VideoMetadataResult>>(emptyList())
    val onlineResults: StateFlow<List<VideoMetadataResult>> = _onlineResults
    private val _onlineLoading = MutableStateFlow(false)
    val onlineLoading: StateFlow<Boolean> = _onlineLoading
    private val _onlineError = MutableStateFlow<String?>(null)
    val onlineError: StateFlow<String?> = _onlineError
    private var searchJob: Job? = null

    fun setQuery(q: String) {
        _query.value = q
        if (q.trim() != _submittedQuery.value) {
            searchJob?.cancel()
            _onlineResults.value = emptyList()
            _onlineError.value = null
            _onlineLoading.value = false
        }
    }

    /** Explicit submit, with cancellable requests and visible failure state. */
    fun searchNow() {
        val term = _query.value.trim()
        if (term.isBlank()) return
        searchJob?.cancel()
        _submittedQuery.value = term
        _onlineResults.value = emptyList()
        _onlineError.value = null
        searchJob = viewModelScope.launch {
            _onlineLoading.value = true
            try {
                val result = onlineProvider.searchVideos(term)
                if (_submittedQuery.value == term) {
                    _onlineResults.value = result.items
                    _onlineError.value = result.errorMessage
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (_submittedQuery.value == term) _onlineError.value = "Search failed: ${error.message ?: error.javaClass.simpleName}"
            } finally {
                if (_submittedQuery.value == term) _onlineLoading.value = false
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

class PlayerViewModel(private val videoId: String, private val providers: ProviderManager) : ViewModel() {
    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state
    /** Last position, kept across rotation. */
    var positionMs: Long = 0

    init { load() }

    fun load() {
        _state.value = PlayerUiState.Loading
        viewModelScope.launch {
            val provider = providers.playbackProvider(videoId)
            _state.value = if (provider == null) {
                PlayerUiState.Failed(PlaybackState.UNSUPPORTED, "No playback source is available for this video.")
            } else when (val r = provider.resolve(videoId)) {
                is PlaybackResolution.Ready -> PlayerUiState.Ready(r)
                is PlaybackResolution.Failed -> PlayerUiState.Failed(r.state, r.message)
            }
        }
    }

    fun failPlayback(message: String) { _state.value = PlayerUiState.Failed(PlaybackState.NETWORK_ERROR, message) }
}
