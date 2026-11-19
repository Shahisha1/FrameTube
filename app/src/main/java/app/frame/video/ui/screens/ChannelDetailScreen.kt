package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.ui.viewmodel.ChannelDetailViewModel
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun ChannelDetailScreen(
    vm: ChannelDetailViewModel,
    onOpenVideo: (String) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val subscribed by vm.subscribed.collectAsStateWithLifecycle()

    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.error != null -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Channel unavailable", style = MaterialTheme.typography.titleLarge)
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = vm::load) { Text("Try again") }
        }
        state.profile != null -> {
            val profile = state.profile!!
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!profile.bannerUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.bannerUrl,
                            contentDescription = "Channel banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!profile.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = profile.avatarUrl,
                                contentDescription = "${profile.name} avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(76.dp).clip(CircleShape)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(profile.name, style = MaterialTheme.typography.headlineSmall)
                            if (profile.verified) Text("Verified channel", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            profile.subscriberCount?.let {
                                Text("${formatCount(it)} subscribers", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Button(onClick = vm::toggleSubscription, modifier = Modifier.fillMaxWidth()) {
                        Text(if (subscribed) "Subscribed · Tap to unsubscribe" else "Subscribe")
                    }
                    if (!profile.description.isNullOrBlank()) {
                        Text(profile.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("Videos", style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (state.videos.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No public videos were returned for this channel.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f)) {
                        items(state.videos.size, key = { state.videos[it].videoId }) { index ->
                            val video = state.videos[index]
                            HomeVideoItem(
                                videoId = video.videoId,
                                title = video.title?.takeIf { it.isNotBlank() } ?: "Video ${video.videoId}",
                                thumbnail = video.thumbnailUrl,
                                channel = profile.name,
                                duration = video.durationSeconds,
                                channelAvatar = profile.avatarUrl,
                                metadataLine = videoMetadataLine(video),
                                onClick = { onOpenVideo(video.videoId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCount(count: Long): String = when {
    count >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
    count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
    count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
    else -> count.toString()
}
