package app.frame.video.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import app.frame.video.data.local.VideoEntity
import coil.compose.AsyncImage

@Composable
fun EmptyState(title: String, hint: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun <T : Any> PagedList(
    pagingItems: LazyPagingItems<T>,
    key: (T) -> Any,
    emptyTitle: String,
    emptyHint: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    row: @Composable (T) -> Unit
) {
    val refresh = pagingItems.loadState.refresh
    when {
        pagingItems.itemCount == 0 && refresh is LoadState.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        pagingItems.itemCount == 0 && refresh is LoadState.Error ->
            EmptyState("Could not load this list", refresh.error.message ?: "Unknown error", "Retry") { pagingItems.retry() }
        pagingItems.itemCount == 0 -> EmptyState(emptyTitle, emptyHint, actionLabel, onAction)
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 8.dp)) {
            items(count = pagingItems.itemCount, key = pagingItems.itemKey(key)) { index -> pagingItems[index]?.let { row(it) } }
        }
    }
}

@Composable
fun VideoThumbnail(url: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(3.dp))
    )
}

@Composable
fun VideoRow(video: VideoEntity, onOpen: (String) -> Unit, trailingContent: (@Composable () -> Unit)? = null) {
    val title = video.title.takeIf { it != video.id } ?: "Video ${video.id}"
    Row(
        Modifier.fillMaxWidth().clickable(onClickLabel = "Open video") { onOpen(video.id) }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(Modifier.size(144.dp, 81.dp)) {
            if (!video.thumbnailUrl.isNullOrBlank()) {
                VideoThumbnail(video.thumbnailUrl, Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            video.durationSeconds?.takeIf { it > 0L }?.let {
                val total = it
                val label = "%d:%02d".format(total / 60, total % 60)
                Text(
                    label,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(3.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.82f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Column(Modifier.weight(1f).padding(top = 1.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (video.channelId.isNullOrBlank()) "YouTube" else video.source.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        trailingContent?.invoke()
    }
}

fun openOnYouTube(context: Context, videoId: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) { }
}
