package app.frame.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.data.download.DownloadStore
import app.frame.video.ui.components.VideoThumbnail
import app.frame.video.ui.viewmodel.DiscoveryViewModel
import app.frame.video.ui.viewmodel.LibraryViewModel

private enum class LibrarySection(val label: String) {
    HISTORY("History"), PLAYLISTS("Playlists"), LIKED("Liked"), DOWNLOADS("Downloads"), CHANNELS("Channels")
}

@Composable
fun LibraryScreen(
    vm: LibraryViewModel,
    downloads: DownloadStore,
    onOpenVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlayDownload: (Long) -> Unit,
    onImport: () -> Unit
) {
    var section by remember { mutableStateOf(LibrarySection.HISTORY) }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 8.dp)) {
            Text("YOUR COLLECTION", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Library", style = MaterialTheme.typography.headlineLarge)
            Text("Your activity and saved videos, kept on this device.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibrarySection.values().forEach { item ->
                FilterChip(selected = section == item, onClick = { section = item }, label = { Text(item.label) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                LibrarySection.HISTORY -> HistoryScreen(vm, onOpenVideo, onImport)
                LibrarySection.PLAYLISTS -> PlaylistsScreen(vm, onOpenPlaylist, onImport)
                LibrarySection.LIKED -> LikedScreen(vm, onOpenVideo, onImport)
                LibrarySection.DOWNLOADS -> DownloadsScreen(downloads, onPlayDownload)
                LibrarySection.CHANNELS -> ChannelsScreen(vm, onImport, onOpenChannel)
            }
        }
    }
}

/**
 * A discovery surface for provider-identified short-form videos.
 * Playback is handed to the existing player; this screen does not create a second engine.
 */
@Composable
fun ShortsScreen(discovery: DiscoveryViewModel, onOpenVideo: (String) -> Unit) {
    val state by discovery.state.collectAsStateWithLifecycle()
    val shorts = state.shortsVideos
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("SHORTS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("Short-form videos", style = MaterialTheme.typography.headlineMedium)
            }
            IconButton(onClick = discovery::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Shorts")
            }
        }
        when {
            state.shortsLoading && shorts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            shorts.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("No Shorts available", style = MaterialTheme.typography.titleLarge)
                    Text(
                        state.shortsError ?: "Channels without a Shorts tab may not have public short-form results.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = discovery::refresh) { Text("Refresh") }
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(shorts, key = { it.videoId }) { video ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenVideo(video.videoId) },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Box {
                            if (!video.thumbnailUrl.isNullOrBlank()) {
                                VideoThumbnail(video.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(9f / 16f))
                            } else {
                                Box(Modifier.fillMaxWidth().aspectRatio(9f / 16f), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(48.dp))
                                }
                            }
                            Column(
                                Modifier.align(Alignment.BottomStart).fillMaxWidth()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))))
                                    .padding(16.dp)
                            ) {
                                Text(video.title ?: "Short video", color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                                Text("Open video", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
