package app.frame.video.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.core.player.PlaybackSession
import app.frame.video.domain.playback.PlaybackQueueEntry
import app.frame.video.domain.playback.PlaybackQueueMode
import app.frame.video.core.provider.ProviderManager
import app.frame.video.domain.LibraryRepository
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.ui.components.VideoThumbnail
import app.frame.video.ui.viewmodel.DiscoveryViewModel
import app.frame.video.ui.viewmodel.PlayerUiState
import app.frame.video.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A vertical Shorts feed backed by the app-wide playback session. Inactive
 * pages render thumbnails; the selected page attaches the shared Media3 player.
 */
@Composable
fun ShortsScreen(
    discovery: DiscoveryViewModel,
    providers: ProviderManager,
    library: LibraryRepository,
    session: PlaybackSession,
    onOpenVideo: (String) -> Unit,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit = {}
) {
    val feed by discovery.state.collectAsStateWithLifecycle()
    val shorts = feed.shortsVideos
    val pagerState = rememberPagerState(pageCount = { shorts.size })
    val scope = rememberCoroutineScope()
    val sessionState by session.state.collectAsStateWithLifecycle()

    LaunchedEffect(shorts) {
        if (shorts.isNotEmpty()) {
            val activeIndex = shorts.indexOfFirst { it.videoId == sessionState.currentVideoId }
                .takeIf { it >= 0 } ?: pagerState.currentPage.coerceIn(0, shorts.lastIndex)
            session.setQueue(
                entries = shorts.map { PlaybackQueueEntry(it.videoId, it.title.orEmpty(), it.thumbnailUrl) },
                mode = PlaybackQueueMode.SHORTS,
                startIndex = activeIndex
            )
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        session.pause()
    }

    LaunchedEffect(sessionState.currentVideoId, shorts) {
        val activeIndex = shorts.indexOfFirst { it.videoId == sessionState.currentVideoId }
        if (activeIndex >= 0 && activeIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(activeIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage, shorts) {
        val nextVideo = shorts.getOrNull(pagerState.currentPage + 1) ?: return@LaunchedEffect
        try {
            withTimeoutOrNull(12_000) { providers.prefetchPlayback(nextVideo.videoId) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            shorts.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (feed.shortsLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text("No Shorts available", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(
                            feed.shortsError ?: "No public short-form videos were found.",
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        IconButton(onClick = discovery::refresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Shorts", tint = Color.White)
                        }
                    }
                }
            }
            else -> VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 0,
                key = { page -> shorts[page].videoId }
            ) { page ->
                val video = shorts[page]
                if (page == pagerState.currentPage) {
                    val playerVm: PlayerViewModel = viewModel(
                        key = "shorts-${video.videoId}",
                        factory = viewModelFactory {
                            initializer { PlayerViewModel(video.videoId, providers, library) }
                        }
                    )
                    ShortsActivePage(
                        video = video,
                        index = page,
                        total = shorts.size,
                        vm = playerVm,
                        library = library,
                        session = session,
                        onOpenVideo = onOpenVideo
                    )
                } else {
                    ShortsPreviewPage(video = video)
                }
            }
        }

        Row(
            Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { session.pause(); onClose() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.34f), MaterialTheme.shapes.small)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Shorts", tint = Color.White)
            }
            Text("Shorts", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        IconButton(
            onClick = onOpenSearch,
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp, top = 8.dp)
                .background(Color.Black.copy(alpha = 0.34f), MaterialTheme.shapes.small)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search videos", tint = Color.White)
        }
    }
}

@Composable
private fun ShortsPreviewPage(video: VideoMetadataResult) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (!video.thumbnailUrl.isNullOrBlank()) {
            VideoThumbnail(video.thumbnailUrl, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ShortsActivePage(
    video: app.frame.video.domain.provider.VideoMetadataResult,
    index: Int,
    total: Int,
    vm: PlayerViewModel,
    library: LibraryRepository,
    session: PlaybackSession,
    onOpenVideo: (String) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val liked by remember(video.videoId) { library.observeIsLiked(video.videoId) }
        .collectAsStateWithLifecycle(initialValue = false)
    val sessionState by session.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when (val playback = state) {
            PlayerUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
            is PlayerUiState.Ready -> ShortsExoSurface(
                videoId = video.videoId,
                source = playback.source,
                vm = vm,
                session = session
            )
            is PlayerUiState.Failed -> Column(
                Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("This Short couldn't play", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(playback.message, color = Color.White.copy(alpha = 0.72f))
                IconButton(onClick = vm::load) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry playback", tint = Color.White)
                }
            }
        }

        if (sessionState.currentVideoId == video.videoId && sessionState.error != null) {
            Column(
                Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(sessionState.error.orEmpty(), color = Color.White, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = session::retryCurrent) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry Short", tint = Color.White)
                }
            }
        }

        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(0.78f)
                .background(Color.Black.copy(alpha = 0.30f))
                .padding(start = 18.dp, end = 12.dp, top = 12.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(video.title.orEmpty(), color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 3)
            Text("YouTube · Short ${index + 1} of ${total}", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall)
        }

        Column(
            Modifier.align(Alignment.CenterEnd).padding(end = 8.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconToggleButton(
                    checked = liked,
                    onCheckedChange = { requested ->
                        scope.launch {
                            try {
                                library.ensureVideo(video.videoId, video.title, video.thumbnailUrl)
                                library.setLiked(video.videoId, requested)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) { }
                        }
                    }
                ) {
                    Icon(
                        if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (liked) "Unlike Short" else "Like Short",
                        tint = if (liked) Color(0xFFFF5A70) else Color.White
                    )
                }
                Text("Like", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=${video.videoId}")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share Short"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Share Short", tint = Color.White)
                }
                Text("Share", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { onOpenVideo(video.videoId) }) {
                    Icon(Icons.Default.Info, contentDescription = "Video details", tint = Color.White)
                }
                Text("Details", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun ShortsExoSurface(
    videoId: String,
    source: PlaybackResolution.Ready,
    vm: PlayerViewModel,
    session: PlaybackSession
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = session.player

    LaunchedEffect(videoId, source.uri) {
        session.playQueued(PlaybackQueueEntry(videoId, ""), source, vm.positionMs)
    }

    LaunchedEffect(player, videoId) {
        while (true) {
            delay(5_000)
            if (player.currentMediaItem?.mediaId == videoId) {
                vm.positionMs = player.currentPosition
                vm.savePosition(player.currentPosition, player.duration)
            }
        }
    }

    var wasPlayingBeforeStop by remember(videoId) { mutableStateOf(false) }
    DisposableEffect(player, lifecycleOwner, videoId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (player.currentMediaItem?.mediaId == videoId) {
                    wasPlayingBeforeStop = player.isPlaying
                    vm.positionMs = player.currentPosition
                    vm.savePosition(player.currentPosition, player.duration)
                    session.pause()
                }
                Lifecycle.Event.ON_START -> if (
                    wasPlayingBeforeStop && player.currentMediaItem?.mediaId == videoId
                ) {
                    session.resume()
                    wasPlayingBeforeStop = false
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (player.currentMediaItem?.mediaId == videoId) {
                vm.positionMs = player.currentPosition
                vm.savePosition(player.currentPosition, player.duration)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                keepScreenOn = true
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize().clickable { session.togglePlayback() }
    )
}
