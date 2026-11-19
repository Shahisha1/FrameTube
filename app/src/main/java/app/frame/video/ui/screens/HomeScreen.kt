package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.frame.video.ui.components.VideoRow
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (counts.isEmpty) "A HOME FOR YOUR WATCH HISTORY" else "WELCOME BACK",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (counts.isEmpty) "Your videos,\nyour space." else "Your library",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight
                )
                Text(
                    if (counts.isEmpty) "Bring your YouTube archive into Frame. Keep your history and playlists organised on this device."
                    else "A quieter place to pick up where you left off.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (counts.isEmpty) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Text("Start with your Takeout", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Import your Google Takeout ZIP to bring in subscriptions, playlists and watch history. Frame reviews the archive before saving anything.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onOpenImport, modifier = Modifier.fillMaxWidth()) {
                            Text("Import Google Takeout")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowForward, contentDescription = null)
                        }
                        OutlinedButton(onClick = onOpenSearch, modifier = Modifier.fillMaxWidth()) {
                            Text("Explore YouTube instead")
                        }
                    }
                }
            }
            item {
                Text(
                    "PRIVATE BY DESIGN  ·  Your imported library stays on this device",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LibraryStat(counts.videos.toString(), "VIDEOS")
                        LibraryStat(counts.channels.toString(), "CHANNELS")
                        LibraryStat(counts.playlists.toString(), "PLAYLISTS")
                        LibraryStat(counts.watched.toString(), "WATCHED")
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Recently watched", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("Pick up where you left off", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = onOpenSearch, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Text("Find videos")
                    }
                }
            }
            if (recent.isEmpty()) {
                item {
                    Text(
                        "No watch history imported yet. Search for something to watch or import another Takeout archive.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recent, key = { it.id }) { VideoRow(it, onOpenVideo) }
            }
            item {
                OutlinedButton(onClick = onOpenImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Import another archive")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
private fun LibraryStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
