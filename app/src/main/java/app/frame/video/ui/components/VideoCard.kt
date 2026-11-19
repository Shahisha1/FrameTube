package app.frame.video.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.frame.video.data.local.VideoEntity

/** Large 16:9 thumbnail with a title below it, for horizontal rows and grids. */
@Composable
fun VideoCard(video: VideoEntity, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val idOnly = video.title == video.id
    Column(
        modifier
            .width(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = "Open video") { onOpen(video.id) }
    ) {
        if (!video.thumbnailUrl.isNullOrBlank()) {
            VideoThumbnail(video.thumbnailUrl, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            if (idOnly) "Video ${video.id}" else video.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, top = 8.dp, bottom = 4.dp)
        )
    }
}
