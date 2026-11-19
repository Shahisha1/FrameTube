package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import app.frame.video.data.download.DownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LocalDownloadPlayerScreen(downloadId: Long, store: DownloadStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val uri by produceState<android.net.Uri?>(initialValue = null, downloadId) {
        value = withContext(Dispatchers.IO) { store.uri(downloadId) }
    }
    if (uri == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("This file is not ready yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri!!))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("OFFLINE PLAYBACK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        AndroidView(
            factory = { PlayerView(it).apply { this.player = player } },
            update = { it.player = player },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to downloads") }
    }
}
