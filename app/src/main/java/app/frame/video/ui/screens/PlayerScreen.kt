package app.frame.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.frame.video.core.datastore.FramePreferences
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.data.sponsorblock.SponsorBlockClient
import app.frame.video.data.sponsorblock.SponsorSegment
import app.frame.video.ui.components.openOnYouTube
import app.frame.video.ui.viewmodel.PlayerUiState
import app.frame.video.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    videoId: String,
    vm: PlayerViewModel,
    preferences: FramePreferences,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Video player", style = MaterialTheme.typography.headlineSmall)
        }
        when (val s = state) {
            PlayerUiState.Loading -> Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Finding a playable source", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is PlayerUiState.Ready -> {
                ExoSurface(videoId, s.source, vm, preferences)
                Text(s.source.sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is PlayerUiState.Failed -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Can't play this video", style = MaterialTheme.typography.titleLarge)
                    Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = vm::load) { Text("Try again") }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { openOnYouTube(context, videoId) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Text("Open source", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Text("Settings", modifier = Modifier.padding(start = 6.dp))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to video") }
    }
}

@Composable
private fun ExoSurface(videoId: String, source: PlaybackResolution.Ready, vm: PlayerViewModel, preferences: FramePreferences) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(source.uri) {
        ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.Builder().setUri(source.uri).apply { source.mimeType?.let { setMimeType(it) } }.build()
            setMediaItem(item)
            seekTo(vm.positionMs)
            prepare()
            playWhenReady = true
        }
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
            if (sponsorBlockEnabled && player.isPlaying) {
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
    LaunchedEffect(player) {
        while (true) {
            delay(10_000)
            if (player.isPlaying) vm.savePosition(player.currentPosition, player.duration)
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                vm.positionMs = player.currentPosition
                vm.savePosition(player.currentPosition, player.duration)
                vm.failPlayback("Playback stopped (${error.errorCodeName}). Check your connection and try again.")
            }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) vm.savePosition(player.currentPosition, player.duration, ended = true)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.positionMs = player.currentPosition
                vm.savePosition(player.currentPosition, player.duration)
                player.pause()
            }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            vm.positionMs = player.currentPosition
            vm.savePosition(player.currentPosition, player.duration)
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    this.player = player
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp)).background(Color.Black)
        )
        if (skipNotice != null) Text(skipNotice.orEmpty(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Slider(
            value = positionMs.coerceIn(0L, durationMs.coerceAtLeast(1L)).toFloat(),
            onValueChange = { scrubbing = true; positionMs = it.toLong() },
            onValueChangeFinished = { player.seekTo(positionMs); scrubbing = false },
            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
            enabled = durationMs > 0L
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatPlayerTime(positionMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(formatPlayerTime(durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { player.seekBack() }) { Icon(Icons.Default.Replay10, contentDescription = "Back 10 seconds") }
            IconButton(onClick = {
                if (player.isPlaying) player.pause() else player.play()
                isPlaying = player.isPlaying
            }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pause" else "Play") }
            IconButton(onClick = { player.seekForward() }) { Icon(Icons.Default.Forward10, contentDescription = "Forward 10 seconds") }
            TextButton(onClick = {
                val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)
                val next = options[(options.indexOf(speed).coerceAtLeast(0) + 1) % options.size]
                speed = next
                player.playbackParameters = PlaybackParameters(next)
            }) { Text("${speed}×") }
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
