package app.frame.video.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.data.local.VideoEntity
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.ui.viewmodel.DiscoveryViewModel
import app.frame.video.ui.viewmodel.LibraryViewModel
import coil.compose.AsyncImage
import android.content.Intent

private data class HomeFeedVideo(val video: VideoMetadataResult, val fromSubscriptions: Boolean)

private val discoveryTopics = listOf("Home", "Subs", "All", "Music", "Gaming", "Mixes", "Live", "Technology", "Learning", "Recently uploaded")

fun videoMetadataLine(video: VideoMetadataResult): String? {
    val parts = buildList {
        video.viewCount?.takeIf { it >= 0L }?.let { add("${compactVideoCount(it)} views") }
        video.textualUploadDate?.takeIf { it.isNotBlank() }?.let(::add)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun compactVideoCount(value: Long): String = when {
    value >= 1_000_000_000L -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}

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
    onOpenShorts: (String?) -> Unit = {},
    hideShorts: Boolean = false,
    mutedKeywords: String = "",
    maxPerChannel: Int = 3
) {
    val counts by vm.counts.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    val feed by discovery.state.collectAsStateWithLifecycle()
    var selectedTopic by remember { mutableStateOf("Home") }
    val subscriptions = remember(feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(feed.videos, hideShorts, mutedKeywords, maxPerChannel)
    }
    val recommendations = remember(feed.discoveredVideos, feed.videos, hideShorts, mutedKeywords, maxPerChannel) {
        app.frame.video.domain.filterRecommendations(
            feed.discoveredVideos.filterNot { candidate -> feed.videos.any { it.videoId == candidate.videoId } },
            hideShorts, mutedKeywords, maxPerChannel
        ).take(24)
    }
    val homeFeed = remember(recommendations, subscriptions) {
        buildList {
            repeat(maxOf(recommendations.size, subscriptions.size)) { index ->
                recommendations.getOrNull(index)?.let { add(HomeFeedVideo(it, false)) }
                subscriptions.getOrNull(index)?.let { add(HomeFeedVideo(it, true)) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                discoveryTopics.forEach { topic ->
                    FilterChip(
                        selected = selectedTopic == topic,
                        onClick = {
                            selectedTopic = topic
                            when (topic) {
                                "Subs" -> onOpenChannels()
                                "Home", "All" -> discovery.refresh()
                                else -> onExploreTopic(topic.lowercase())
                            }
                        },
                        label = { Text(topic, maxLines = 1) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                            selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                            selectedLabelColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }

        if (!hideShorts && feed.shortsVideos.isNotEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Shorts", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { onOpenShorts(null) }) { Text("See all") }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(feed.shortsVideos.take(10), key = { "short-" + it.videoId }) { short ->
                            Column(
                                Modifier.width(148.dp).clickable { onOpenShorts(short.videoId) },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(0.58f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    if (!short.thumbnailUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = short.thumbnailUrl,
                                            contentDescription = short.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Text(
                                    short.title?.takeIf { it.isNotBlank() } ?: "Short video",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            item {
                SectionHeading("Continue watching", onRefresh = null)
            }
            items(recent.take(4), key = { "recent-" + it.id }) { video ->
                HomeVideoItem(
                    videoId = video.id,
                    title = video.title,
                    thumbnail = video.thumbnailUrl,
                    channel = "Continue watching",
                    duration = video.durationSeconds,
                    onClick = { onOpenVideo(video.id) }
                )
            }
        }

        item {
            SectionHeading(
                feed.discoveryContext ?: "Recommended for you",
                onRefresh = discovery::refresh
            )
        }
        when {
            feed.discoveryLoading && homeFeed.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(vertical = 44.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            homeFeed.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(feed.discoveryError ?: "No videos are available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = discovery::refresh) { Text("Try again") }
                        OutlinedButton(onClick = onOpenSearch) { Text("Search videos") }
                    }
                    if (subscriptions.isEmpty()) {
                        OutlinedButton(onClick = onOpenChannels) { Text("Browse subscriptions") }
                    }
                }
            }
            else -> items(homeFeed, key = { "feed-" + it.video.videoId }) { item ->
                val video = item.video
                HomeVideoItem(
                    videoId = video.videoId,
                    title = video.title?.takeIf { it.isNotBlank() } ?: "Untitled video",
                    thumbnail = video.thumbnailUrl,
                    channel = video.channelName ?: if (item.fromSubscriptions) "Latest upload" else "YouTube",
                    duration = video.durationSeconds,
                    channelAvatar = video.channelAvatarUrl,
                    metadataLine = videoMetadataLine(video),
                    onClick = {
                        if (item.fromSubscriptions) onOpenVideo(video.videoId)
                        else onOpenRecommendation(video.videoId, recommendations)
                    }
                )
            }
        }

        item {
            Text(
                "${counts.videos} saved videos · ${counts.channels} subscriptions · Stored on this device",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, onRefresh: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 17.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (onRefresh != null) {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

@Composable
fun HomeVideoItem(
    videoId: String,
    title: String,
    thumbnail: String?,
    channel: String,
    duration: Long?,
    channelAvatar: String? = null,
    metadataLine: String? = null,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 13.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClickLabel = "Play video", onClick = onClick)
        ) {
            if (!thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            duration?.takeIf { it > 0L }?.let {
                val label = if (it >= 3600) "%d:%02d:%02d".format(it / 3600, (it % 3600) / 60, it % 60)
                    else "%d:%02d".format(it / 60, it % 60)
                Text(
                    label,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp)
                        .background(Color.Black.copy(alpha = 0.88f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            if (!channelAvatar.isNullOrBlank()) {
                AsyncImage(
                    model = channelAvatar,
                    contentDescription = "$channel channel",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.padding(top = 1.dp).size(38.dp).clip(CircleShape)
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(top = 1.dp).size(38.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    channel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!metadataLine.isNullOrBlank()) {
                    Text(
                        metadataLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Video options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Open video") },
                        onClick = { menuExpanded = false; onClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            menuExpanded = false
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://www.youtube.com/watch?v=$videoId")
                            }
                            context.startActivity(Intent.createChooser(intent, "Share video"))
                        }
                    )
                }
            }
        }
    }
}