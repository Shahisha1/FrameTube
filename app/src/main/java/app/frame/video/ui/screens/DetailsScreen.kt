package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.R
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
    val video by produceState<VideoEntity?>(initialValue = null, videoId) {
        value = repository.getVideo(videoId)
    }
    val online by produceState<VideoMetadataResult?>(initialValue = null, videoId, metadataProvider) {
        value = metadataProvider.getVideoMetadata(videoId)
    }
    val liked by remember(videoId) { repository.observeIsLiked(videoId) }.collectAsStateWithLifecycle(initialValue = false)
    val idOnly = video == null || video?.title == videoId

    /** Keeps a library row for this video, with the best title known right now, so history and likes can show it. */
    suspend fun saveToLibrary() {
        try {
            repository.ensureVideo(videoId, online?.title ?: video?.title, online?.thumbnailUrl ?: video?.thumbnailUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The library entry is a convenience. Playing still works without it.
        }
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val thumbnail = online?.thumbnailUrl ?: video?.thumbnailUrl
        if (!thumbnail.isNullOrBlank()) VideoThumbnail(thumbnail, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Text(
            if (idOnly) online?.title ?: "Video $videoId" else video?.title ?: "Video $videoId",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("Video ID: $videoId", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            when {
                online == null ->
                    "Loading live details from YouTube..."
                online?.availability == MetadataAvailability.AVAILABLE ->
                    "Details loaded from YouTube through NewPipe Extractor. Your saved library entry stays on this device."
                idOnly ->
                    "FrameTube could not load live details for this video, but the saved entry remains in your library."
                else ->
                    "Live details could not be loaded; the saved entry remains in your library."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { scope.launch { saveToLibrary(); onPlay() } }) { Text("Play") }
            IconToggleButton(
                checked = liked,
                onCheckedChange = { want ->
                    scope.launch {
                        saveToLibrary()
                        try {
                            repository.setLiked(videoId, want)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // The heart follows the database, so a failed write leaves it unchanged.
                        }
                    }
                }
            ) {
                Icon(
                    if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(if (liked) R.string.action_unlike else R.string.action_like),
                    tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        OutlinedButton(onClick = { openOnYouTube(context, videoId) }) { Text("Open on YouTube") }
    }
}
