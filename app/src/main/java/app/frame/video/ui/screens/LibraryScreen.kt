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
