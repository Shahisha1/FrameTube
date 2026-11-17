package app.frame.video.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import app.frame.video.ui.components.EmptyState
import app.frame.video.ui.components.PagedList
import app.frame.video.ui.components.VideoRow
import app.frame.video.ui.viewmodel.LibraryViewModel
import app.frame.video.ui.viewmodel.SearchViewModel
import app.frame.video.core.provider.VideoMetadataResult

@Composable
fun ChannelsScreen(vm: LibraryViewModel, onImport: () -> Unit) {
    val items = vm.channels.collectAsLazyPagingItems()
    PagedList(items, key = { it.id }, emptyTitle = "No channels yet", emptyHint = "Subscriptions from your Takeout appear here.", actionLabel = "Import Takeout", onAction = onImport) {
        ListItem(headlineContent = { Text(it.title) }, supportingContent = { Text("Channel ID ${it.id}") }, leadingContent = { Icon(Icons.Default.AccountCircle, null) })
    }
}

@Composable
fun PlaylistsScreen(vm: LibraryViewModel, onOpenPlaylist: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.playlists.collectAsLazyPagingItems()
    PagedList(items, key = { it.id }, emptyTitle = "No playlists yet", emptyHint = "Playlists from your Takeout appear here.", actionLabel = "Import Takeout", onAction = onImport) {
        ListItem(
            headlineContent = { Text(it.title) },
            leadingContent = { Icon(Icons.Default.List, null) },
            modifier = Modifier.clickable(onClickLabel = "Open playlist") { onOpenPlaylist(it.id) }
        )
    }
}

@Composable
fun PlaylistDetailScreen(playlistId: String, vm: LibraryViewModel, onOpenVideo: (String) -> Unit) {
    val playlist by remember(playlistId) { vm.playlist(playlistId) }.collectAsStateWithLifecycle(initialValue = null)
    val items = remember(playlistId) { vm.playlistVideos(playlistId) }.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        Text(playlist?.title ?: "Playlist", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
        PagedList(items, key = { it.id }, emptyTitle = "This playlist is empty", emptyHint = "No videos were found for it in the archive.") { VideoRow(it, onOpenVideo) }
    }
}

@Composable
fun HistoryScreen(vm: LibraryViewModel, onOpenVideo: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.history.collectAsLazyPagingItems()
    PagedList(items, key = { it.id }, emptyTitle = "No watch history", emptyHint = "Imported watch history appears here, newest first.", actionLabel = "Import Takeout", onAction = onImport) { VideoRow(it, onOpenVideo) }
}

@Composable
fun LikedScreen(vm: LibraryViewModel, onOpenVideo: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.liked.collectAsLazyPagingItems()
    PagedList(items, key = { it.id }, emptyTitle = "No liked videos", emptyHint = "Frame has not found a verified liked-videos file in the Takeout formats it understands yet.", actionLabel = "Import Takeout", onAction = onImport) { VideoRow(it, onOpenVideo) }
}

@Composable
fun SearchScreen(vm: SearchViewModel, onOpenVideo: (String) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val online by vm.onlineResults.collectAsStateWithLifecycle()
    val loading by vm.onlineLoading.collectAsStateWithLifecycle()
    val items = vm.results.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = vm::setQuery, singleLine = true,
            label = { Text("Search YouTube and your library") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
        if (query.isBlank()) {
            EmptyState("Search videos", "Search YouTube or find videos in your saved library.")
        } else {
            Text("YouTube", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
            when {
                loading -> CircularProgressIndicator(Modifier.padding(16.dp))
                online.isEmpty() -> Text("No online matches yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                else -> LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(online, key = { it.videoId }) { result ->
                        OnlineVideoRow(result, onOpenVideo)
                    }
                }
            }
            Text("Your library", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            PagedList(
                items,
                key = { it.id },
                emptyTitle = "No local matches",
                emptyHint = "Nothing in your saved library matches \"${query.trim()}\"."
            ) { VideoRow(it, onOpenVideo) }
        }
    }
}

@Composable
private fun OnlineVideoRow(video: VideoMetadataResult, onOpenVideo: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(video.title ?: "YouTube video") },
        supportingContent = { Text("YouTube · NewPipe Extractor") },
        leadingContent = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
        modifier = Modifier.clickable(onClickLabel = "Open video") { onOpenVideo(video.videoId) }
    )
}
