package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.domain.provider.MetadataAvailability
import app.frame.video.domain.provider.VideoMetadataResult
import app.frame.video.domain.provider.VideoProvider
import app.frame.video.data.local.VideoEntity
import app.frame.video.domain.LibraryRepository
import app.frame.video.ui.components.VideoThumbnail
import app.frame.video.ui.components.openOnYouTube
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text("YouTube video · $videoId", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(
                when {
                    online == null -> "Loading video details…"
                    online?.availability == MetadataAvailability.AVAILABLE -> "Details loaded through NewPipe Extractor. Your saved library entry stays on this device."
                    idOnly -> "This entry came from Takeout with no title. FrameTube could not load live details, but the saved entry remains in your library."
                    else -> "This entry came from Takeout. Live details could not be loaded; the saved entry remains in your library."
                },
                modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
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
