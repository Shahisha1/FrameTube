package app.frame.video.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.data.local.VideoEntity
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.ui.components.VideoRow
import app.frame.video.ui.viewmodel.DiscoveryViewModel
import app.frame.video.ui.viewmodel.LibraryViewModel

private val discoveryTopics = listOf("All", "Gaming", "Music", "Technology", "Education", "News", "Sports")

@Composable
fun HomeScreen(
    vm: LibraryViewModel,
    discovery: DiscoveryViewModel,
    onOpenImport: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenSearch: () -> Unit,
    onExploreTopic: (String) -> Unit,
    onOpenVideo: (String) -> Unit,
    onOpenRecommendation: (String, List<VideoMetadataResult>) -> Unit = { id, _ -> onOpenVideo(id) },
    hideShorts: Boolean = false,
    mutedKeywords: String = "",
    maxPerChannel: Int = 3
) {
    val counts by vm.counts.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val feed by discovery.state.collectAsStateWithLifecycle()
    var selectedTopic by remember { mutableStateOf("All") }
    val subscriptions = remember(feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(feed.videos, hideShorts, mutedKeywords, maxPerChannel)
    }
    val recommendations = remember(feed.discoveredVideos, feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(
            feed.discoveredVideos.filterNot { candidate -> feed.videos.any { it.videoId == candidate.videoId } },
            hideShorts, mutedKeywords, maxPerChannel
        ).take(24)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("For you", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Videos picked from discovery and your subscriptions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = discovery::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh feed")
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                discoveryTopics.forEach { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = {
                            selectedTopic = topic
                            if (topic != "All") onExploreTopic(topic.lowercase())
                        },
                        label = { Text(topic) }
                    )
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                Text("Continue watching", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 18.dp, top = 8.dp))
            }
            items(recent, key = { "recent-" + it.id }) { VideoRow(it, onOpenVideo) }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(feed.discoveryContext ?: "Recommended videos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Discover something new", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = discovery::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh recommendations") }
            }
        }
        when {
            feed.discoveryLoading && recommendations.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            recommendations.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(feed.discoveryError ?: "No recommendations are available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = discovery::refresh) { Text("Try again") }
                        OutlinedButton(onClick = onOpenSearch) { Text("Search videos") }
                    }
                }
            }
            else -> items(recommendations, key = { "discover-" + it.videoId }) { video ->
                VideoRow(
                    VideoEntity(
                        id = video.videoId,
                        title = video.title?.takeIf { it.isNotBlank() } ?: "Untitled video",
                        channelId = video.channelId,
                        thumbnailUrl = video.thumbnailUrl,
                        durationSeconds = video.durationSeconds,
                        source = "YOUTUBE"
                    ),
                    { id -> onOpenRecommendation(id, recommendations) }
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("From your subscriptions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Latest public uploads from followed channels", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = discovery::refresh) { Icon(Icons.Default.Refresh, contentDescription = "Refresh subscriptions") }
            }
        }
        when {
            feed.loading && subscriptions.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            subscriptions.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Your followed channels will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(onClick = onOpenChannels) { Text("Browse subscriptions") }
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
            Text(
                "${counts.videos} saved videos · ${counts.channels} subscriptions · Stored on this device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, top = 12.dp)
            )
        }
    }
}
