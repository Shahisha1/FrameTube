package app.frame.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.ui.PlayerView
import app.frame.video.core.datastore.FramePreferences
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.core.player.PlaybackSession
import app.frame.video.domain.playback.PlaybackQueueEntry
import app.frame.video.domain.playback.PlaybackQueueMode
import app.frame.video.data.sponsorblock.SponsorBlockClient
import app.frame.video.data.sponsorblock.SponsorSegment
import app.frame.video.ui.components.openOnYouTube
import app.frame.video.domain.LibraryRepository
import app.frame.video.data.local.ChannelEntity
import app.frame.video.domain.provider.ChannelProvider
import app.frame.video.domain.provider.ChannelProfile
import kotlinx.coroutines.flow.flowOf
import coil.compose.AsyncImage
import app.frame.video.domain.provider.VideoProvider
import kotlinx.coroutines.launch
import app.frame.video.ui.viewmodel.PlayerUiState
import app.frame.video.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    videoId: String,
    vm: PlayerViewModel,
    session: PlaybackSession,
    preferences: FramePreferences,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onQueueItemChanged: (String) -> Unit,
    library: LibraryRepository,
    metadataProvider: VideoProvider,
    channelProvider: ChannelProvider,
    onOpenChannel: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsStateWithLifecycle()
    val sessionState by session.state.collectAsStateWithLifecycle()
    val liked by remember(videoId) { library.observeIsLiked(videoId) }.collectAsStateWithLifecycle(initialValue = false)
    val metadata by produceState<app.frame.video.domain.provider.VideoMetadataResult?>(initialValue = null, videoId) {
        try {
            value = metadataProvider.getVideoMetadata(videoId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = null
        }
    }
    val videoTitle = metadata?.title?.takeIf { it.isNotBlank() } ?: "Video"
    val channel by produceState<ChannelProfile?>(initialValue = null, metadata?.channelId) {
        val id = metadata?.channelId?.takeIf { it.isNotBlank() } ?: return@produceState
        val url = if (id.startsWith("http")) id else "https://www.youtube.com/channel/$id"
        try {
            value = channelProvider.getChannelPage(url).profile
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            value = null
        }
    }
    val subscribedFlow = remember(channel?.id) {
        channel?.let { library.observeIsSubscribed(it.id) } ?: flowOf(false)
    }
    val subscribed by subscribedFlow.collectAsStateWithLifecycle(initialValue = false)
    val upcomingVideo = sessionState.queue.entries.getOrNull(sessionState.queue.currentIndex + 1)
    LaunchedEffect(sessionState.currentVideoId, sessionState.queue.mode) {
        val nextId = sessionState.currentVideoId
        if (
            nextId != null &&
            nextId != videoId &&
            sessionState.queue.current?.videoId == nextId &&
            sessionState.queue.mode in setOf(PlaybackQueueMode.USER, PlaybackQueueMode.AUTOPLAY)
        ) {
            onQueueItemChanged(nextId)
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        when (val s = state) {
            PlayerUiState.Loading -> Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Loading video", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is PlayerUiState.Ready -> ExoSurface(videoId, s.source, vm, session, preferences, onBack)
            is PlayerUiState.Failed -> Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Video unavailable", style = MaterialTheme.typography.titleLarge)
                    Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = vm::load) { Text("Try again") }
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                videoTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                lineHeight = MaterialTheme.typography.titleLarge.lineHeight
            )
            metadata?.let { videoMetadata ->
                val details = listOfNotNull(
                    videoMetadata.viewCount?.takeIf { it >= 0L }?.let { "${compactVideoCount(it)} views" },
                    videoMetadata.textualUploadDate?.takeIf { it.isNotBlank() }
                ).joinToString(" · ")
                if (details.isNotBlank()) {
                    Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (channel != null) {
                val profile = channel!!
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = "${profile.name} channel",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.size(42.dp).clip(CircleShape).clickable { onOpenChannel(profile.id) }
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp).clickable { onOpenChannel(profile.id) },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        Modifier.weight(1f).clickable { onOpenChannel(profile.id) },
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(profile.name, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, maxLines = 1)
                        profile.subscriberCount?.let {
                            Text("${formatChannelCount(it)} subscribers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    library.saveChannel(ChannelEntity(profile.id, profile.name, profile.url, profile.avatarUrl))
                                    library.setSubscribed(profile.id, !subscribed)
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) { }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (subscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.onSurface,
                            contentColor = if (subscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.background
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (subscribed) "Subscribed" else "Subscribe")
                    }
                }
            } else if (!metadata?.channelName.isNullOrBlank()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!metadata?.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = metadata?.channelAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(48.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(videoTitle, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text(metadata?.channelName.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val details = listOfNotNull(
                            metadata?.viewCount?.takeIf { it >= 0L }?.let { "${compactVideoCount(it)} views" },
                            metadata?.textualUploadDate?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text(videoTitle, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconToggleButton(
                            checked = liked,
                            onCheckedChange = { requested ->
                                scope.launch {
                                    try {
                                        library.ensureVideo(videoId, videoTitle, metadata?.thumbnailUrl)
                                        library.setLiked(videoId, requested)
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) { }
                                }
                            }
                        ) {
                            Icon(
                                if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (liked) "Unlike video" else "Like video",
                                tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(
                            Modifier.size(width = 1.dp, height = 22.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        IconButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$videoId")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share video"))
                        }) { Icon(Icons.Default.Share, contentDescription = "Share video") }
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    TextButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Text("More", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }

            if (sessionState.queue.entries.size > 1) {
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Up next", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(upcomingVideo?.title ?: "End of queue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                    }
                    IconButton(onClick = session::playPrevious, enabled = sessionState.queue.hasPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Play previous video")
                    }
                    IconButton(onClick = session::playNext, enabled = sessionState.queue.hasNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Play next video")
                    }
                }
            }

            OutlinedButton(onClick = { openOnYouTube(context, videoId) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Text("Open on YouTube", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun ExoSurface(videoId: String, source: PlaybackResolution.Ready, vm: PlayerViewModel, session: PlaybackSession, preferences: FramePreferences, onBack: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = session.player
    LaunchedEffect(videoId, source.uri) {
        session.playQueued(PlaybackQueueEntry(videoId, "Video"), source, vm.positionMs)
    }
    val sponsorBlockEnabled by preferences.sponsorBlockEnabled.collectAsStateWithLifecycle(initialValue = false)
    val sponsorBlockCategories by preferences.sponsorBlockCategories.collectAsStateWithLifecycle(initialValue = FramePreferences.DEFAULT_SPONSORBLOCK_CATEGORIES)
    val sponsorSegments = remember(player) { mutableStateListOf<SponsorSegment>() }
    val skippedStarts = remember(player) { mutableSetOf<Long>() }
    var skipNotice by remember(player) { mutableStateOf<String?>(null) }
    var isPlaying by remember(player) { mutableStateOf(player.isPlaying) }
    var positionMs by remember(player) { mutableStateOf(player.currentPosition) }
    var durationMs by remember(player) { mutableStateOf(0L) }
    var speed by remember(player) { mutableStateOf(1f) }
    var scrubbing by remember(player) { mutableStateOf(false) }
    var controlsVisible by remember(player) { mutableStateOf(true) }

    LaunchedEffect(player, videoId, sponsorBlockEnabled, sponsorBlockCategories) {
        sponsorSegments.clear()
        skippedStarts.clear()
        skipNotice = null
        if (sponsorBlockEnabled && sponsorBlockCategories.isNotEmpty()) {
            try {
                sponsorSegments.addAll(SponsorBlockClient().getSegments(videoId, sponsorBlockCategories))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Playback remains available if the optional community API is unavailable.
            }
        }
    }
    LaunchedEffect(player, sponsorBlockEnabled) {
        while (true) {
            if (sponsorBlockEnabled && player.isPlaying && player.currentMediaItem?.mediaId == videoId) {
                val current = player.currentPosition
                val segment = sponsorSegments.firstOrNull {
                    current >= it.startMs && current < it.endMs && it.startMs !in skippedStarts
                }
                if (segment != null) {
                    skippedStarts.add(segment.startMs)
                    player.seekTo(segment.endMs)
                    skipNotice = "Skipped ${segment.category.replace('_', ' ')} segment"
                }
            }
            delay(250)
        }
    }
    LaunchedEffect(skipNotice) {
        if (skipNotice != null) {
            delay(3_000)
            skipNotice = null
        }
    }
    LaunchedEffect(player) {
        while (true) {
            durationMs = player.duration.takeIf { it > 0L } ?: 0L
            if (!scrubbing) positionMs = player.currentPosition.coerceAtLeast(0L)
            isPlaying = player.isPlaying
            delay(500)
        }
    }
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying) {
            delay(3_500)
            controlsVisible = false
        }
    }
    LaunchedEffect(player) {
        while (true) {
            delay(10_000)
            if (player.isPlaying && player.currentMediaItem?.mediaId == videoId) vm.savePosition(player.currentPosition, player.duration)
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (player.currentMediaItem?.mediaId == videoId) {
                    vm.positionMs = player.currentPosition
                    vm.savePosition(player.currentPosition, player.duration)
                    vm.failPlayback("Playback stopped (${error.errorCodeName}). Check your connection and try again.")
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && player.currentMediaItem?.mediaId == videoId) vm.savePosition(player.currentPosition, player.duration, ended = true)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && player.currentMediaItem?.mediaId == videoId) {
                vm.positionMs = player.currentPosition
                vm.savePosition(player.currentPosition, player.duration)
                session.pause()
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            if (player.currentMediaItem?.mediaId == videoId) {
                vm.positionMs = player.currentPosition
                vm.savePosition(player.currentPosition, player.duration)
            }
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
        }
    }

    Box(
        Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .background(Color.Black)
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().clickable { controlsVisible = !controlsVisible })
        if (controlsVisible) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.32f), Color.Transparent, Color.Black.copy(alpha = 0.58f))
                    )
                )
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.42f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to videos", tint = Color.White)
            }
            Row(
                Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(26.dp)
            ) {
                IconButton(
                    onClick = { player.seekBack(); controlsVisible = true },
                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "Back 10 seconds", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        if (player.isPlaying) player.pause() else player.play()
                        isPlaying = player.isPlaying
                        controlsVisible = true
                    },
                    modifier = Modifier.size(58.dp).background(Color.Black.copy(alpha = 0.48f), CircleShape)
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                IconButton(
                    onClick = { player.seekForward(); controlsVisible = true },
                    modifier = Modifier.size(48.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10 seconds", tint = Color.White)
                }
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Slider(
                    value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
                    onValueChange = { scrubbing = true; positionMs = it.toLong(); controlsVisible = true },
                    onValueChangeFinished = { player.seekTo(positionMs); scrubbing = false },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    enabled = durationMs > 0L,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFF0033),
                        activeTrackColor = Color(0xFFFF0033),
                        inactiveTrackColor = Color.White.copy(alpha = 0.48f)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatPlayerTime(positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                            val next = options[(options.indexOf(speed).coerceAtLeast(0) + 1) % options.size]
                            speed = next
                            player.playbackParameters = PlaybackParameters(next)
                            controlsVisible = true
                        }
                    ) {
                        Text("${speed}×", color = Color.White)
                    }
                    Text(formatPlayerTime(durationMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (skipNotice != null) {
            Text(
                skipNotice.orEmpty(),
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

private fun formatPlayerTime(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0L) / 1_000L
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remainder = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, remainder) else "%d:%02d".format(minutes, remainder)
}

private fun compactVideoCount(count: Long): String = when {
    count >= 1_000_000_000L -> "%.1fB".format(count / 1_000_000_000.0)
    count >= 1_000_000L -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000L -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}

private fun formatChannelCount(count: Long): String = when {
    count >= 1_000_000_000L -> "%.1fB".format(count / 1_000_000_000.0)
    count >= 1_000_000L -> "%.1fM".format(count / 1_000_000.0)
    count >= 1_000L -> "%.1fK".format(count / 1_000.0)
    else -> count.toString()
}
