package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.frame.video.core.provider.PlaybackResolution
import app.frame.video.ui.components.openOnYouTube
import app.frame.video.ui.viewmodel.PlayerUiState
import app.frame.video.ui.viewmodel.PlayerViewModel

@Composable
fun PlayerScreen(videoId: String, vm: PlayerViewModel, onBack: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = state) {
            PlayerUiState.Loading -> Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is PlayerUiState.Ready -> {
                ExoSurface(s.source, vm)
                Text(s.source.sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is PlayerUiState.Failed -> {
                Text("Can't play this video", style = MaterialTheme.typography.titleLarge)
                Text(s.message)
                Button(onClick = vm::load) { Text("Try again") }
            }
        }
        OutlinedButton(onClick = { openOnYouTube(context, videoId) }) { Text("Open on YouTube") }
        OutlinedButton(onClick = onOpenSettings) { Text("Playback settings") }
        OutlinedButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun ExoSurface(source: PlaybackResolution.Ready, vm: PlayerViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val player = remember(source.uri) {
        // No cache is configured on purpose: Frame does not store video.
        ExoPlayer.Builder(context).build().apply {
            val item = MediaItem.Builder().setUri(source.uri).apply { source.mimeType?.let { setMimeType(it) } }.build()
            setMediaItem(item)
            seekTo(vm.positionMs)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                vm.positionMs = player.currentPosition
                vm.failPlayback("Playback stopped (${error.errorCodeName}). Check your connection and try again.")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { vm.positionMs = player.currentPosition; player.pause() }
        }
        player.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            vm.positionMs = player.currentPosition
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.removeListener(listener)
            player.release()
        }
    }
    AndroidView(
        factory = { PlayerView(it).apply { this.player = player } },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
    )
}
