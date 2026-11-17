package app.frame.video.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.data.importer.TakeoutImportResult
import app.frame.video.ui.viewmodel.ImportUiState
import app.frame.video.ui.viewmodel.ImportViewModel

@Composable
fun ImportScreen(vm: ImportViewModel, onOpenLibrary: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.onPicked(uri, uri.lastPathSegment ?: "Takeout.zip")
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Import Google Takeout", style = MaterialTheme.typography.headlineMedium)
        when (val s = state) {
            ImportUiState.Idle -> {
                Text("Choose the Takeout ZIP. Frame reads it on this device, shows what it found, and only saves after you confirm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Importing the same archive again", style = MaterialTheme.typography.titleMedium)
                        Text("No duplicates. Playlists are replaced by the new export. Subscriptions, history and videos are only added to; items you removed in YouTube stay until you delete library data in Settings.")
                    }
                }
                Button(onClick = { picker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) }) { Text("Choose Takeout ZIP") }
            }
            is ImportUiState.Reading -> {
                Text("Reading ${s.sourceName}…")
                LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedButton(onClick = vm::cancel) { Text("Cancel") }
            }
            is ImportUiState.Preview -> {
                Text("Found in ${s.sourceName}", style = MaterialTheme.typography.titleMedium)
                ResultCounts(s.parsed.result)
                if (s.parsed.result.filesFound.isNotEmpty()) {
                    Text("Files used", style = MaterialTheme.typography.titleSmall)
                    s.parsed.result.filesFound.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                Issues(s.parsed.result)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = vm::confirm) { Text("Import") }
                    OutlinedButton(onClick = vm::cancel) { Text("Cancel") }
                }
            }
            ImportUiState.Saving -> {
                Text("Saving to your library…")
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("This step is all or nothing, so it can't be half done.", style = MaterialTheme.typography.bodySmall)
            }
            is ImportUiState.Done -> {
                Text("Import finished", style = MaterialTheme.typography.titleMedium)
                ResultCounts(s.result)
                Issues(s.result)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { vm.reset(); onOpenLibrary() }) { Text("Open library") }
                    OutlinedButton(onClick = vm::reset) { Text("Import another") }
                }
            }
            is ImportUiState.Failed -> {
                Text("Import problem", style = MaterialTheme.typography.titleMedium)
                Text(s.message)
                OutlinedButton(onClick = vm::reset) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun ResultCounts(r: TakeoutImportResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Subscriptions: ${r.subscriptions}")
            Text("Playlists: ${r.playlists}")
            Text("Playlist entries: ${r.playlistEntries}")
            Text("Watched records: ${r.watchedVideos}")
            Text("Searches: ${r.searches}")
            Text("Liked videos: not imported (no verified source file)")
        }
    }
}

@Composable
private fun Issues(r: TakeoutImportResult) {
    if (r.issues.isEmpty()) return
    Text("Warnings (${r.issues.size})", style = MaterialTheme.typography.titleSmall)
    r.issues.take(10).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
}
