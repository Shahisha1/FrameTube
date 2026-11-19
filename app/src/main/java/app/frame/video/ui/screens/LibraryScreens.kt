package app.frame.video.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import app.frame.video.ui.viewmodel.DiscoveryViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun ChannelsScreen(vm: LibraryViewModel, onImport: () -> Unit, onOpenChannel: (String) -> Unit) {
    val items = vm.channels.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        LibraryHeading("YOUR NETWORK", "Channels", "Channels from your saved archive.")
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "No channels yet", emptyHint = "Subscriptions from your Takeout appear here.", actionLabel = "Import Takeout", onAction = onImport) {
                ListItem(headlineContent = { Text(it.title) }, supportingContent = { Text("Channel ID ${it.id}") }, leadingContent = { Icon(Icons.Default.AccountCircle, null) }, modifier = Modifier.clickable(onClickLabel = "Open channel") { onOpenChannel(it.id) })
            }
        }
    }
}

@Composable
fun PlaylistsScreen(vm: LibraryViewModel, onOpenPlaylist: (String) -> Unit, onImport: () -> Unit) {
    val items = vm.playlists.collectAsLazyPagingItems()
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        LibraryHeading("YOUR COLLECTION", "Playlists", "Your saved video groups.")
        OutlinedButton(onClick = { error = null; showCreate = true }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("New playlist")
        }
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "No playlists yet", emptyHint = "Create a playlist or import one from Takeout.", actionLabel = "Import Takeout", onAction = onImport) {
                ListItem(
                    headlineContent = { Text(it.title) },
                    supportingContent = { Text(it.source.lowercase().replaceFirstChar { c -> c.uppercase() }) },
                    leadingContent = { Icon(Icons.Default.List, null) },
                    modifier = Modifier.clickable(onClickLabel = "Open playlist") { onOpenPlaylist(it.id) }
                )
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New playlist") },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Name") }, singleLine = true)
                    androidx.compose.material3.OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createPlaylist(title, description, onDone = {
                        showCreate = false; title = ""; description = ""; error = null
                    }, onError = { error = it })
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    vm: LibraryViewModel,
    onOpenVideo: (String) -> Unit,
    onDeleted: () -> Unit
) {
    val playlist by remember(playlistId) { vm.playlist(playlistId) }.collectAsStateWithLifecycle(initialValue = null)
    val items = remember(playlistId) { vm.playlistVideos(playlistId) }.collectAsLazyPagingItems()
    var showAdd by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var videoInput by remember { mutableStateOf("") }
    var title by remember(playlist?.title) { mutableStateOf(playlist?.title.orEmpty()) }
    var description by remember(playlist?.description) { mutableStateOf(playlist?.description.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        LibraryHeading("PLAYLIST", playlist?.title ?: "Playlist", playlist?.description ?: "Manage videos in this collection.")
        androidx.compose.foundation.layout.Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { error = null; showAdd = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Add video")
            }
            IconButton(onClick = { title = playlist?.title.orEmpty(); description = playlist?.description.orEmpty(); error = null; showEdit = true }, enabled = playlist != null) {
                Icon(Icons.Default.Edit, contentDescription = "Edit playlist")
            }
            IconButton(onClick = { error = null; showDelete = true }, enabled = playlist != null) {
                Icon(Icons.Default.Delete, contentDescription = "Delete playlist")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) }
        Box(Modifier.weight(1f)) {
            PagedList(items, key = { it.id }, emptyTitle = "This playlist is empty", emptyHint = "Add a YouTube video ID or link to start this list.") {
                VideoRow(it, onOpenVideo) {
                    IconButton(onClick = { vm.removeVideoFromPlaylist(playlistId, it.id) { error = it } }) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from playlist", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add video") },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = videoInput, onValueChange = { videoInput = it }, label = { Text("YouTube URL or video ID") }, singleLine = true)
                    Text("Frame saves the video to this playlist on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addVideoToPlaylist(playlistId, videoInput, onDone = { added ->
                        if (added) { showAdd = false; videoInput = ""; error = null }
                        else error = "This video is already in the playlist."
                    }, onError = { error = it })
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } }
        )
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit playlist") },
            text = {
                Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Name") }, singleLine = true)
                    androidx.compose.material3.OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updatePlaylist(playlistId, title, description, onDone = { showEdit = false; error = null }, onError = { error = it })
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("This removes the playlist and its list of videos. It does not delete the videos from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePlaylist(playlistId, onDone = onDeleted, onError = { error = it; showDelete = false })
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
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
fun SearchScreen(
    vm: SearchViewModel,
    onOpenVideo: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val query by vm.query.collectAsStateWithLifecycle()
    val submittedQuery by vm.submittedQuery.collectAsStateWithLifecycle()
    val filter by vm.filter.collectAsStateWithLifecycle()
    val online by vm.onlineResults.collectAsStateWithLifecycle()
    val loading by vm.onlineLoading.collectAsStateWithLifecycle()
    val loadingMore by vm.loadingMore.collectAsStateWithLifecycle()
    val error by vm.onlineError.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val recent by vm.recentSearches.collectAsStateWithLifecycle()
    val libraryItems = vm.results.collectAsLazyPagingItems()
    val searching = submittedQuery == query.trim() && submittedQuery.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            TextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Search YouTube") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                },
                shape = RoundedCornerShape(26.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.searchNow() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("videos" to "Videos", "channels" to "Channels", "playlists" to "Playlists").forEach { (value, label) ->
                FilterChip(
                    selected = filter == value,
                    onClick = { vm.selectFilter(value) },
                    label = { Text(label) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(Modifier.weight(1f)) {
            if (query.isBlank()) {
                if (recent.isNotEmpty()) {
                    item {
                        Text("Recent searches", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp))
                    }
                    items(recent, key = { it.id }) { item ->
                        ListItem(
                            headlineContent = { Text(item.query) },
                            leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.clickable { vm.useSuggestion(item.query) }
                        )
                    }
                } else {
                    item {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Search videos, channels and playlists", style = MaterialTheme.typography.titleMedium)
                            Text("Your search history stays on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (!searching) {
                if (suggestions.isNotEmpty()) {
                    items(suggestions, key = { it }) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion) },
                            leadingContent = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.clickable { vm.useSuggestion(suggestion) }
                        )
                    }
                } else {
                    item { Text("Press Enter to search.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp)) }
                }
            } else {
                if (loading && online.items.isEmpty() && online.channels.isEmpty() && online.playlists.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (error != null && online.items.isEmpty() && online.channels.isEmpty() && online.playlists.isEmpty()) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                            Button(onClick = vm::searchNow) { Text("Try again") }
                        }
                    }
                } else when (filter) {
                    "channels" -> {
                        if (online.channels.isEmpty()) item { Text("No channels found.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp)) }
                        items(online.channels, key = { it.url }) { channel ->
                            ListItem(
                                headlineContent = { Text(channel.title, maxLines = 2) },
                                supportingContent = { Text("YouTube channel") },
                                leadingContent = {
                                    if (!channel.thumbnailUrl.isNullOrBlank()) AsyncImage(
                                        model = channel.thumbnailUrl, contentDescription = null,
                                        modifier = Modifier.size(48.dp).clip(CircleShape)
                                    ) else Icon(Icons.Default.AccountCircle, null)
                                },
                                modifier = Modifier.clickable { onOpenChannel(channel.url) }
                            )
                        }
                    }
                    "playlists" -> {
                        if (online.playlists.isEmpty()) item { Text("No playlists found.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp)) }
                        items(online.playlists, key = { it.url }) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.title, maxLines = 2) },
                                supportingContent = { Text("YouTube playlist") },
                                leadingContent = {
                                    if (!playlist.thumbnailUrl.isNullOrBlank()) VideoThumbnail(playlist.thumbnailUrl, Modifier.size(112.dp, 63.dp))
                                    else Icon(Icons.Default.List, null)
                                },
                                modifier = Modifier.clickable {
                                    runCatching {
                                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(playlist.url)))
                                    }
                                }
                            )
                        }
                    }
                    else -> {
                        if (online.items.isEmpty()) item { Text("No videos found.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp)) }
                        items(online.items, key = { it.videoId }) { result ->
                            VideoRow(
                                app.frame.video.data.local.VideoEntity(
                                    id = result.videoId,
                                    title = result.title?.takeIf { it.isNotBlank() } ?: "YouTube video",
                                    channelId = result.channelId,
                                    thumbnailUrl = result.thumbnailUrl,
                                    durationSeconds = result.durationSeconds,
                                    source = "YOUTUBE"
                                ),
                                onOpenVideo
                            )
                        }
                    }
                }
                if (online.nextPageToken != null) {
                    item {
                        Button(onClick = vm::loadMore, enabled = !loadingMore, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                            if (loadingMore) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Load more")
                        }
                    }
                }
                item {
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text("Saved in your library", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 6.dp))
                }
                if (libraryItems.itemCount == 0) {
                    item { Text("No saved videos match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) }
                } else {
                    items(count = libraryItems.itemCount) { index -> libraryItems[index]?.let { VideoRow(it, onOpenVideo) } }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsScreen(
    vm: LibraryViewModel,
    discovery: DiscoveryViewModel,
    onImport: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val channels = vm.channels.collectAsLazyPagingItems()
    val feed by discovery.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Subscriptions", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            IconButton(onClick = discovery::refresh) { Icon(Icons.Default.Search, contentDescription = "Refresh subscriptions") }
        }
        if (channels.itemCount > 0) {
            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(channels.itemCount) { index ->
                    channels[index]?.let { channel ->
                        Column(
                            Modifier.width(72.dp).clickable { onOpenChannel(channel.id) },
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!channel.thumbnailUrl.isNullOrBlank()) AsyncImage(
                                model = channel.thumbnailUrl, contentDescription = channel.title,
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                            ) else Box(
                                Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) { Text(channel.title.take(1).uppercase()) }
                            Text(channel.title, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        when {
            feed.loading && feed.videos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            feed.videos.isEmpty() -> EmptyState(
                "No recent uploads",
                "Subscribe to channels to see their latest public videos here.",
                "Import subscriptions",
                onImport
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                items(feed.videos, key = { it.videoId }) { video ->
                    VideoRow(
                        app.frame.video.data.local.VideoEntity(
                            id = video.videoId,
                            title = video.title?.takeIf { it.isNotBlank() } ?: "YouTube video",
                            channelId = video.channelId,
                            thumbnailUrl = video.thumbnailUrl,
                            durationSeconds = video.durationSeconds,
                            source = "YOUTUBE"
                        ),
                        onOpenVideo
                    )
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
