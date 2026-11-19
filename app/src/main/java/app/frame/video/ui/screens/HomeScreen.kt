package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.data.local.VideoEntity
import app.frame.video.ui.components.VideoRow
import app.frame.video.ui.viewmodel.LibraryViewModel
import app.frame.video.ui.viewmodel.DiscoveryViewModel

@Composable
fun HomeScreen(
    vm: LibraryViewModel,
    discovery: DiscoveryViewModel,
    onOpenImport: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenVideo: (String) -> Unit,
    hideShorts: Boolean = false,
    mutedKeywords: String = "",
    maxPerChannel: Int = 3
) {
    val counts by vm.counts.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val feed by discovery.state.collectAsStateWithLifecycle()
    val recommendations = remember(feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(feed.videos, hideShorts, mutedKeywords, maxPerChannel)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(if (counts.isEmpty) "A HOME FOR YOUR WATCH HISTORY" else "WELCOME BACK", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text(if (counts.isEmpty) "Your videos, your space." else "Your library", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    if (counts.isEmpty) "Bring your saved videos and subscriptions into Frame. Your library stays on this device."
                    else "Pick up where you left off, or find something new from your channels.",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (counts.isEmpty) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(12.dp))
                        }
                        Text("Make Frame yours", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Import your Google Takeout archive to restore subscriptions, playlists and watch history. Or search for a video to start fresh.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onOpenImport, modifier = Modifier.fillMaxWidth()) {
                            Text("Import library")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                        OutlinedButton(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth()) { Text("Explore videos") }
                    }
                }
            }
            item { Text("PRIVATE BY DESIGN  ·  Your imported library stays on this device", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        LibraryStat(counts.videos.toString(), "VIDEOS")
                        LibraryStat(counts.channels.toString(), "CHANNELS")
                        LibraryStat(counts.playlists.toString(), "PLAYLISTS")
                        LibraryStat(counts.watched.toString(), "WATCHED")
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Recommended from your channels", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Recent public uploads, shaped by your feed filters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.IconButton(onClick = discovery::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh recommendations") }
                }
            }
            when {
                feed.loading && feed.videos.isEmpty() -> item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { androidx.compose.material3.CircularProgressIndicator() }
                }
                feed.error != null && feed.videos.isEmpty() -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(feed.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = discovery::refresh) { Text("Try again") }
                    }
                }
                recommendations.isEmpty() -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (feed.videos.isEmpty()) "No channel uploads yet. Subscribe to channels to see their latest public videos here."
                            else "Your feed filters hid these uploads. Adjust them in Settings to see more.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = onOpenChannels) { Text("Manage channels") }
                    }
                }
                else -> items(recommendations, key = { "feed-" + it.videoId }) { video ->
                    VideoRow(
                        VideoEntity(
                            id = video.videoId,
                            title = video.title?.takeIf { it.isNotBlank() } ?: "Untitled video",
                            channelId = video.channelId,
                            thumbnailUrl = video.thumbnailUrl,
                            durationSeconds = video.durationSeconds,
                            source = "YOUTUBE"
                        ),
                        onOpenVideo
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Recently watched", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Pick up where you left off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = onOpenSearch, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Text("Find videos") }
                }
            }
            if (recent.isEmpty()) {
                item { Text("No watch history yet. Search for something to watch or import a Takeout archive.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(recent, key = { it.id }) { VideoRow(it, onOpenVideo) }
            }
            item {
                OutlinedButton(onClick = onOpenImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Import another archive")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(feed.discoveryContext ?: "Explore something new", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("A discovery lane shaped by your recent viewing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                androidx.compose.material3.IconButton(onClick = discovery::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh discovery")
                }
            }
        }
        when {
            feed.discoveryLoading && feed.discoveredVideos.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
            feed.discoveredVideos.isEmpty() -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(feed.discoveryError ?: "Search for a topic to start building your discovery feed.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenSearch) { Text("Explore topics") }
                }
            }
            else -> items(
                app.frame.video.domain.filterRecommendations(
                    feed.discoveredVideos.filterNot { candidate -> feed.videos.any { it.videoId == candidate.videoId } },
                    hideShorts, mutedKeywords, maxPerChannel
                ).take(12),
                key = { "discover-" + it.videoId }
            ) { video ->
                VideoRow(
                    VideoEntity(
                        id = video.videoId,
                        title = video.title?.takeIf { it.isNotBlank() } ?: "Untitled video",
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

@Composable
private fun LibraryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
