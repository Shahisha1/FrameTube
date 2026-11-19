package app.frame.video.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.frame.video.core.datastore.PipedConfig
import app.frame.video.core.provider.piped.PipedApi

@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    piped: PipedConfig,
    onSetPiped: (enabled: Boolean, url: String) -> Unit,
    onImport: () -> Unit,
    onDeleteData: () -> Unit
) {
    var urlText by remember(piped.instanceUrl) { mutableStateOf(piped.instanceUrl) }
    var showConsent by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val urlValid = PipedApi.normalizeInstanceUrl(urlText) != null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Dark theme", style = MaterialTheme.typography.titleMedium)
                Text("Use a dark look for the app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = darkTheme, onCheckedChange = onToggleTheme)
        }
        HorizontalDivider()

        Text("Unofficial playback (Piped)", style = MaterialTheme.typography.titleMedium)
        Text("Plays videos without YouTube's player and its ads, through a Piped server you choose. Off by default.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = urlText, onValueChange = { urlText = it }, singleLine = true,
            label = { Text("Piped API address, https://…") },
            isError = urlText.isNotBlank() && !urlValid,
            supportingText = { if (urlText.isNotBlank() && !urlValid) Text("Use an https:// address, for example your own Piped server.") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (piped.enabled) "Enabled" else "Disabled", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Switch(
                checked = piped.enabled,
                enabled = piped.enabled || urlValid,
                onCheckedChange = { want ->
                    if (want) showConsent = true else onSetPiped(false, urlText)
                }
            )
        }
        if (piped.enabled && urlText != piped.instanceUrl && urlValid) {
            OutlinedButton(onClick = { onSetPiped(true, urlText) }) { Text("Save address") }
        }
        HorizontalDivider()

        ListItem(headlineContent = { Text("Import Google Takeout") }, supportingContent = { Text("Review an archive before anything is saved.") }, trailingContent = { TextButton(onClick = onImport) { Text("Open") } })
        ListItem(headlineContent = { Text("Delete library data") }, supportingContent = { Text("Removes imported data, watch history, likes and saved videos from this device.") }, trailingContent = { TextButton(onClick = { showDelete = true }) { Text("Delete") } })
        ListItem(headlineContent = { Text("About FrameTube") }, supportingContent = { Text("Version 0.1.0 · Free software, GPL-3.0-or-later · Your library stays on this device.") })
    }

    if (showConsent) AlertDialog(
        onDismissRequest = { showConsent = false },
        title = { Text("Use unofficial playback?") },
        text = {
            Text("This is not an official YouTube feature and may go against YouTube's terms. It can stop working at any time.\n\nThe Piped server you chose will see the IDs of videos you play and your IP address. FrameTube sends nothing else: no account, no Takeout files.\n\nFrame does not save or download video.")
        },
        confirmButton = { Button(onClick = { showConsent = false; onSetPiped(true, urlText) }) { Text("I understand, enable") } },
        dismissButton = { TextButton(onClick = { showConsent = false }) { Text("Cancel") } }
    )
    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Delete all library data?") },
        text = { Text("Subscriptions, playlists, watch history, likes, saved videos, searches and import reports will be removed from this device. This can't be undone.") },
        confirmButton = { Button(onClick = { showDelete = false; onDeleteData() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
    )
}
