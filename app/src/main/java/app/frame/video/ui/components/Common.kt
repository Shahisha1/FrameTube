package app.frame.video.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
    AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.clip(RoundedCornerShape(10.dp)))
}

@Composable
fun VideoRow(video: VideoEntity, onOpen: (String) -> Unit) {
    val idOnly = video.title == video.id
    ListItem(
        headlineContent = { Text(if (idOnly) "Video ${video.id}" else video.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(if (idOnly) "Saved from Takeout · details not loaded" else video.source.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) },
        leadingContent = {
            if (!video.thumbnailUrl.isNullOrBlank()) VideoThumbnail(video.thumbnailUrl, Modifier.size(116.dp, 66.dp))
            else Box(Modifier.size(116.dp, 66.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        modifier = Modifier.clickable(onClickLabel = "Open video") { onOpen(video.id) }
    )
}

fun openOnYouTube(context: Context, videoId: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) { }
}
