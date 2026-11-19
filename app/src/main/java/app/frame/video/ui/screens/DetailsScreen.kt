package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.domain.provider.VideoProvider
import app.frame.video.data.local.VideoEntity
import app.frame.video.domain.LibraryRepository
import app.frame.video.ui.components.VideoThumbnail
import coil.compose.AsyncImage
import app.frame.video.ui.components.openOnYouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun compactVideoCount(value: Long): String = when {
    value >= 1_000_000_000L -> "%.1fB".format(value / 1_000_000_000.0)
    value >= 1_000_000L -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000L -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}

@Composable
fun DetailsScreen(
    videoId: String,
    repository: LibraryRepository,
    metadataProvider: VideoProvider,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val video by produceState<VideoEntity?>(initialValue = null, videoId) { value = repository.getVideo(videoId) }
    val online by produceState<VideoMetadataResult?>(initialValue = null, videoId, metadataProvider) { value = metadataProvider.getVideoMetadata(videoId) }
    val liked by remember(videoId) { repository.observeIsLiked(videoId) }.collectAsStateWithLifecycle(initialValue = false)
    val idOnly = video == null || video?.title == videoId
    val title = if (idOnly) online?.title ?: "Video $videoId" else video!!.title
    val thumbnail = online?.thumbnailUrl ?: video?.thumbnailUrl

    suspend fun saveToLibrary() {
        try { repository.ensureVideo(videoId, online?.title ?: video?.title, online?.thumbnailUrl ?: video?.thumbnailUrl) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Library metadata is optional; playback must remain available. */ }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("VIDEO DETAILS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        if (!thumbnail.isNullOrBlank()) VideoThumbnail(thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(14.dp)))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            if (!online?.channelName.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!online?.channelAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = online?.channelAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(online?.channelName.orEmpty(), style = MaterialTheme.typography.titleSmall)
                        val details = listOfNotNull(
                            online?.viewCount?.takeIf { it >= 0L }?.let { compactVideoCount(it) + " views" },
                            online?.textualUploadDate?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (online?.availability != null && online?.availability != MetadataAvailability.AVAILABLE) {
                Text(
                    if (idOnly) "This saved entry has no title and live details could not be loaded."
                    else "Live video details are temporarily unavailable. Your saved entry remains in your library.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { scope.launch { saveToLibrary(); onPlay() } }, modifier = Modifier.weight(1f)) { Text("Play video") }
            IconToggleButton(
                checked = liked,
                onCheckedChange = { requested ->
                    scope.launch {
                        saveToLibrary()
                        try { repository.setLiked(videoId, requested) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* State remains database-backed if persistence fails. */ }
                    }
                }
            ) {
                Icon(
                    if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (liked) "Unlike video" else "Like video",
                    tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedButton(onClick = { openOnYouTube(context, videoId) }, modifier = Modifier.fillMaxWidth()) { Text("Open on YouTube") }
    }
}
