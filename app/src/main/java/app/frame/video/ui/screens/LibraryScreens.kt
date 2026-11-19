package app.frame.video.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
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
import app.frame.video.ui.components.VideoThumbnail
import app.frame.video.ui.viewmodel.LibraryViewModel
import app.frame.video.ui.viewmodel.SearchViewModel
import app.frame.video.domain.provider.VideoMetadataResult

@Composable
fun ChannelsScreen(vm: LibraryViewModel, onImport: () -> Unit) {
    val items = vm.channels.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        LibraryHeading("YOUR NETWORK", "Channels", "Channels from your saved archive.")
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "No channels yet", emptyHint = "Subscriptions from your Takeout appear here.", actionLabel = "Import Takeout", onAction = onImport) {
                ListItem(headlineContent = { Text(it.title) }, supportingContent = { Text("Channel ID ${it.id}") }, leadingContent = { Icon(Icons.Default.AccountCircle, null) })
            }
        }
    }
}

@Composable
fun PlaylistsScreen(vm: LibraryViewModel, onOpenPlaylist: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.playlists.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        LibraryHeading("YOUR COLLECTION", "Playlists", "Your saved video groups.")
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "No playlists yet", emptyHint = "Playlists from your Takeout appear here.", actionLabel = "Import Takeout", onAction = onImport) {
                ListItem(
                    headlineContent = { Text(it.title) },
                    leadingContent = { Icon(Icons.Default.List, null) },
                    modifier = Modifier.clickable(onClickLabel = "Open playlist") { onOpenPlaylist(it.id) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(playlistId: String, vm: LibraryViewModel, onOpenVideo: (String) -> Unit) {
    val playlist by remember(playlistId) { vm.playlist(playlistId) }.collectAsStateWithLifecycle(initialValue = null)
    val items = remember(playlistId) { vm.playlistVideos(playlistId) }.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        LibraryHeading("PLAYLIST", playlist?.title ?: "Playlist", "Videos saved in this collection.")
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "This playlist is empty", emptyHint = "No videos were found for it in the archive.") { VideoRow(it, onOpenVideo) }
        }
    }
}

@Composable
fun HistoryScreen(vm: LibraryViewModel, onOpenVideo: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.history.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        LibraryHeading("YOUR ACTIVITY", "Watch history", "Videos from your imported watch history.")
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "No watch history", emptyHint = "Imported watch history appears here, newest first.", actionLabel = "Import Takeout", onAction = onImport) { VideoRow(it, onOpenVideo) }
        }
    }
}

@Composable
fun LikedScreen(vm: LibraryViewModel, onOpenVideo: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.liked.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        LibraryHeading("YOUR COLLECTION", "Liked videos", "Videos marked as liked in your archive.")
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "No liked videos", emptyHint = "Frame has not found a verified liked-videos file in the Takeout formats it understands yet.", actionLabel = "Import Takeout", onAction = onImport) { VideoRow(it, onOpenVideo) }
        }
    }
}

@Composable
private fun LibraryHeading(kicker: String, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Text(kicker, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SearchScreen(vm: SearchViewModel, onOpenVideo: (String) -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val submittedQuery by vm.submittedQuery.collectAsStateWithLifecycle()
    val online by vm.onlineResults.collectAsStateWithLifecycle()
    val loading by vm.onlineLoading.collectAsStateWithLifecycle()
    val error by vm.onlineError.collectAsStateWithLifecycle()
    val libraryItems = vm.results.collectAsLazyPagingItems()

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text("DISCOVER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Find a video", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Search online or find something in your saved library.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    singleLine = true,
                    label = { Text("Video title or topic") },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { vm.searchNow() }),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = vm::searchNow,
                    enabled = query.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("Go") }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(Modifier.weight(1f)) {
            if (query.isBlank()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        Text("Start with a topic", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Try a channel name, a game, a song, or a question.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "ONLINE RESULTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 6.dp)
                    )
                }
                if (submittedQuery != query.trim()) {
                    item {
                        Text(
                            "Press Go or Enter to search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                } else when {
                    loading -> item {
                        androidx.compose.foundation.layout.Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                    error != null -> item {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                            Button(onClick = vm::searchNow, modifier = Modifier.padding(top = 8.dp)) { Text("Try again") }
                        }
                    }
                    online.isEmpty() -> item {
                        Text(
                            "No online videos found. Try another search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                    else -> items(online, key = { it.videoId }) { result -> OnlineVideoRow(result, onOpenVideo) }
                }
                item {
                    androidx.compose.material3.HorizontalDivider(
                        Modifier.padding(top = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Text(
                        "YOUR LIBRARY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 18.dp, top = 18.dp, bottom = 6.dp)
                    )
                }
                if (libraryItems.itemCount == 0) {
                    item {
                        Text(
                            "No saved videos match this search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                } else {
                    items(count = libraryItems.itemCount) { index ->
                        libraryItems[index]?.let { VideoRow(it, onOpenVideo) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineVideoRow(video: VideoMetadataResult, onOpenVideo: (String) -> Unit) {
    ListItem(
        headlineContent = { Text(video.title ?: "YouTube video", maxLines = 2) },
        supportingContent = { Text("YouTube · NewPipe Extractor") },
        leadingContent = {
            if (!video.thumbnailUrl.isNullOrBlank()) {
                VideoThumbnail(video.thumbnailUrl, Modifier.size(116.dp, 66.dp))
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
            }
        },
        modifier = Modifier.clickable(onClickLabel = "Open video") { onOpenVideo(video.videoId) }
    )
}
