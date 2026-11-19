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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    val subscriptions = remember(feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(feed.videos, hideShorts, mutedKeywords, maxPerChannel)
    }
    val discoveryVideos = remember(feed.discoveredVideos, feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(
            feed.discoveredVideos.filterNot { candidate -> feed.videos.any { it.videoId == candidate.videoId } },
            hideShorts, mutedKeywords, maxPerChannel
        ).take(18)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("YOUR NEXT WATCH", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("For you", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(
                    "A mix of channels you follow and videos discovered from your viewing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onOpenSearch, modifier = Modifier.weight(1f)) { Text("Explore videos") }
                OutlinedButton(onClick = onOpenImport) {
                    Text("Import library")
                    Spacer(Modifier.padding(start = 2.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
                IconButton(onClick = discovery::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh feed")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("From your subscriptions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Recent public uploads from channels you follow", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = discovery::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh subscriptions") }
            }
        }
        when {
            feed.loading && subscriptions.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            feed.error != null && subscriptions.isEmpty() -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(feed.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = discovery::refresh) { Text("Try again") }
                }
            }
            subscriptions.isEmpty() -> item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your subscriptions will appear here", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Follow channels to see their latest public uploads in this feed. You can also start watching without importing anything.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = onOpenChannels) { Text("Browse subscriptions") }
                    }
                }
            }
            else -> items(subscriptions, key = { "sub-" + it.videoId }) { video ->
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
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(feed.discoveryContext ?: "Picked for you", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Discovery that works before you import a library", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = discovery::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh discovery") }
            }
        }
        when {
            feed.discoveryLoading && discoveryVideos.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            discoveryVideos.isEmpty() -> item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        feed.discoveryError ?: "We couldn't find videos for this feed yet. Try searching for a topic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onOpenSearch) { Text("Search videos") }
                    if (feed.discoveryError != null) OutlinedButton(onClick = discovery::refresh) { Text("Try again") }
                }
            }
            else -> items(discoveryVideos, key = { "discover-" + it.videoId }) { video ->
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
        if (recent.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Continue watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Your recent watch history stays on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(recent, key = { "recent-" + it.id }) { VideoRow(it, onOpenVideo) }
        }
        item {
            Text(
                "${counts.videos} saved videos · ${counts.channels} subscriptions · Your library stays on this device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
