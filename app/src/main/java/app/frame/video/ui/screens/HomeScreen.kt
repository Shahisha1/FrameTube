package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.ui.components.EmptyState
import app.frame.video.ui.components.VideoRow
import app.frame.video.ui.viewmodel.LibraryViewModel

@Composable
fun HomeScreen(vm: LibraryViewModel, onOpenImport: () -> Unit, onOpenVideo: (String) -> Unit) {
    val counts by vm.counts.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    if (counts.isEmpty) {
        EmptyState("Your library is empty", "Import your Google Takeout ZIP. Everything stays on this device.", "Import Takeout", onOpenImport)
        return
    }
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Your library", style = MaterialTheme.typography.titleLarge)
                    Text("${counts.videos} videos · ${counts.channels} channels · ${counts.playlists} playlists · ${counts.watched} watched")
                }
            }
        }
        item {
            Text("Recently watched", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp))
        }
        if (recent.isEmpty()) item {
            Text("No watch history imported yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
        items(recent, key = { it.id }) { VideoRow(it, onOpenVideo) }
        item { Button(onClick = onOpenImport, modifier = Modifier.padding(16.dp)) { Text("Import another archive") } }
    }
}
