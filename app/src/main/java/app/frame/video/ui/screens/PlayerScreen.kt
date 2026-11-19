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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Video player", style = MaterialTheme.typography.headlineSmall)
        }
        when (val s = state) {
            PlayerUiState.Loading -> Card(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Finding a playable source", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is PlayerUiState.Ready -> {
                ExoSurface(s.source, vm)
                Text(s.source.sourceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is PlayerUiState.Failed -> Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                androidx.compose.material3.Icon(Icons.Default.OpenInNew, contentDescription = null)
                Text("YouTube", modifier = Modifier.padding(start = 6.dp))
            }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                androidx.compose.material3.Icon(Icons.Default.Settings, contentDescription = null)
                Text("Settings", modifier = Modifier.padding(start = 6.dp))
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to video") }
    }
}

@Composable
private fun ExoSurface(source: PlaybackResolution.Ready, vm: PlayerViewModel) {
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
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)).background(Color.Black)
    )
}
