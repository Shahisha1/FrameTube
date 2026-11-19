package app.frame.video.ui.screens

import android.app.DownloadManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import app.frame.video.data.download.DownloadItem
import app.frame.video.data.download.DownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DownloadsScreen(store: DownloadStore, onPlay: (Long) -> Unit) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var downloads by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(store) {
        while (isActive) {
            downloads = withContext(Dispatchers.IO) { store.list() }
            delay(1200)
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Direct video files saved on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                    Text("Add a video file", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = url, onValueChange = { url = it; error = null },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Direct HTTPS file link") },
                        placeholder = { Text("https://site.com/video.mp4") }
                    )
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Name (optional)") }
                    )
                    if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Button(
                        onClick = {
                            adding = true
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) { store.enqueue(url, title) }
                                    url = ""; title = ""; error = null
                                    downloads = withContext(Dispatchers.IO) { store.list() }
                                } catch (e: Exception) {
                                    error = e.message ?: "Could not start download."
                                } finally { adding = false }
                            }
                        },
                        enabled = url.isNotBlank() && !adding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (adding) CircularProgressIndicator(strokeWidth = 2.dp)
                        else Icon(Icons.Default.Download, contentDescription = null)
                        Text(if (adding) "Adding…" else "Download file", modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(
                        "MP4 · WebM · MKV · MOV · AVI. YouTube links and streams are not supported.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        item { Text("On this device", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
        if (downloads.isEmpty()) {
            item {
                Text("No downloads yet. Add a direct video file link above.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(downloads, key = { it.id }) { item ->
                DownloadRow(item, onPlay, onRemove = {
                    scope.launch {
                        withContext(Dispatchers.IO) { store.remove(item.id) }
                        downloads = withContext(Dispatchers.IO) { store.list() }
                    }
                })
            }
        }
    }
}

@Composable
private fun DownloadRow(item: DownloadItem, onPlay: (Long) -> Unit, onRemove: () -> Unit) {
    val status = when (item.status) {
        DownloadManager.STATUS_SUCCESSFUL -> "Ready offline"
        DownloadManager.STATUS_RUNNING -> "Downloading"
        DownloadManager.STATUS_PENDING -> "Waiting"
        DownloadManager.STATUS_PAUSED -> "Paused"
        else -> "Download failed · code ${item.reason}"
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                Modifier.size(104.dp, 68.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.status == DownloadManager.STATUS_RUNNING || item.status == DownloadManager.STATUS_PENDING || item.status == DownloadManager.STATUS_PAUSED) {
                    LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (item.totalBytes > 0) "${item.downloadedBytes / 1_048_576} / ${item.totalBytes / 1_048_576} MB"
                        else "${item.downloadedBytes / 1_048_576} MB downloaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.complete) {
                    TextButton(onClick = { onPlay(item.id) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Text("Play offline", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            IconButton(onClick = onRemove) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove download") }
        }
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}
