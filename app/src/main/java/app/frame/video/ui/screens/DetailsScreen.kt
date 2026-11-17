package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.frame.video.core.provider.MetadataAvailability
import app.frame.video.core.provider.VideoMetadataResult
import app.frame.video.core.provider.VideoProvider
import app.frame.video.data.local.VideoEntity
import app.frame.video.domain.LibraryRepository
import app.frame.video.ui.components.openOnYouTube

@Composable
fun DetailsScreen(
    videoId: String,
    repository: LibraryRepository,
    metadataProvider: VideoProvider,
    onPlay: () -> Unit
) {
    val context = LocalContext.current
    val video by produceState<VideoEntity?>(initialValue = null, videoId) {
        value = repository.getVideo(videoId)
    }
    val online by produceState<VideoMetadataResult?>(initialValue = null, videoId, metadataProvider) {
        value = metadataProvider.getVideoMetadata(videoId)
    }
    val idOnly = video == null || video?.title == videoId

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            if (idOnly) online?.title ?: "Video $videoId" else video!!.title,
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
                    "This entry came from your Takeout file with no title. Frame could not load live details, but the saved entry remains in your library."
                else ->
                    "This entry came from your Takeout file. Live details could not be loaded; the saved entry remains in your library."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onPlay) { Text("Try unofficial playback") }
        OutlinedButton(onClick = { openOnYouTube(context, videoId) }) { Text("Open on YouTube") }
    }
}
