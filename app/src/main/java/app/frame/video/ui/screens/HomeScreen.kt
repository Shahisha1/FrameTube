package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.ui.components.VideoCard
import app.frame.video.ui.viewmodel.LibraryViewModel

@Composable
fun HomeScreen(
    vm: LibraryViewModel,
    onOpenImport: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenVideo: (String) -> Unit
) {
    val counts by vm.counts.collectAsStateWithLifecycle()
    val recent by vm.recent.collectAsStateWithLifecycle()
    if (counts.isEmpty) {
        WelcomeState(onOpenSearch, onOpenImport)
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
            Text("Recently watched", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
        }
        if (recent.isEmpty()) {
            item {
                Text("Nothing watched yet. Search for something to watch.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
            }
        } else {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent, key = { it.id }) { VideoCard(it, onOpenVideo) }
                }
            }
        }
        item {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onOpenSearch) { Text("Search videos") }
                OutlinedButton(onClick = onOpenImport) { Text("Import archive") }
            }
        }
    }
}

@Composable
private fun WelcomeState(onOpenSearch: () -> Unit, onOpenImport: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to FrameTube", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            "Search for videos to watch, or import your Google Takeout archive to bring in your subscriptions, playlists and history. Everything stays on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onOpenSearch) { Text("Search videos") }
        OutlinedButton(onClick = onOpenImport) { Text("Import Google Takeout") }
    }
}
