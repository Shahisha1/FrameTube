package app.frame.video.ui.screens

import android.os.Build
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
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

private val sponsorCategories = listOf(
    "sponsor" to "Sponsor", "selfpromo" to "Self-promotion",
    "interaction" to "Interactions", "intro" to "Intros",
    "outro" to "Outros", "preview" to "Previews",
    "music_offtopic" to "Off-topic music", "filler" to "Filler"
)

@Composable
fun SettingsScreen(
    darkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    dynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    accentPalette: String,
    onSetAccentPalette: (String) -> Unit,
    piped: PipedConfig,
    onSetPiped: (enabled: Boolean, url: String) -> Unit,
    sponsorBlockEnabled: Boolean,
    sponsorBlockCategories: Set<String>,
    onSetSponsorBlock: (Boolean, Set<String>) -> Unit,
    hideShorts: Boolean,
    mutedKeywords: String,
    maxPerChannel: Int,
    onSetFeedQuality: (Boolean, String, Int) -> Unit,
    onImport: () -> Unit,
    onDeleteData: () -> Unit
) {
    var urlText by remember(piped.instanceUrl) { mutableStateOf(piped.instanceUrl) }
    var keywordText by remember(mutedKeywords) { mutableStateOf(mutedKeywords) }
    var showConsent by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val urlValid = PipedApi.normalizeInstanceUrl(urlText) != null
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SettingsSection("Appearance", "Frame's look and color behavior") {
            SettingSwitch("Dark theme", "Use Frame's charcoal surfaces and red accent.", darkTheme, onCheckedChange = onToggleTheme)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingSwitch(
                "Dynamic color",
                if (supportsDynamicColor) "Use colors from your device wallpaper." else "Available on Android 12 and newer.",
                dynamicColor, enabled = supportsDynamicColor, onCheckedChange = onToggleDynamicColor
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Accent palette", style = MaterialTheme.typography.titleSmall)
            Text(
                if (dynamicColor && supportsDynamicColor) "Turn off dynamic color to use a Frame palette."
                else "Choose a subtle accent while keeping Frame's layout and identity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("red" to "Frame red", "blue" to "Blue", "violet" to "Violet", "green" to "Green").forEach { (key, label) ->
                    FilterChip(
                        selected = accentPalette == key,
                        onClick = { onSetAccentPalette(key) },
                        label = { Text(label) },
                        enabled = !(dynamicColor && supportsDynamicColor)
                    )
                }
            }
        }

        SettingsSection("SponsorBlock", "Skip community-submitted segments during playback") {
            SettingSwitch(
                "Enable automatic skipping",
                "Frame checks the public SponsorBlock database for selected categories.",
                sponsorBlockEnabled,
                onCheckedChange = { onSetSponsorBlock(it, sponsorBlockCategories) }
            )
            Text("Skip these categories", style = MaterialTheme.typography.titleSmall)
            Text("Changes apply to the next video lookup.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sponsorCategories.forEach { (key, label) ->
                    FilterChip(
                        selected = key in sponsorBlockCategories,
                        onClick = {
                            val next = sponsorBlockCategories.toMutableSet().apply {
                                if (contains(key)) remove(key) else add(key)
                            }
                            onSetSponsorBlock(sponsorBlockEnabled, next)
                        },
                        label = { Text(label) },
                        enabled = sponsorBlockEnabled
                    )
                }
            }
            Text("When enabled, the SponsorBlock service receives the current video ID and your network IP address. No FrameTube account or library data is sent.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Segment timing is community-submitted and may be inaccurate.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SettingsSection("Feed quality", "Reduce repeats and content you do not want to see") {
            SettingSwitch(
                "Hide Shorts",
                "Hide channel uploads that are one minute or shorter when duration is available.",
                hideShorts,
                onCheckedChange = { onSetFeedQuality(it, keywordText, maxPerChannel) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Muted words", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = keywordText,
                onValueChange = { keywordText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Words or phrases, comma-separated") },
                supportingText = { Text("Titles containing these terms are hidden from the channel feed.") },
                minLines = 2,
                maxLines = 3
            )
            OutlinedButton(onClick = { onSetFeedQuality(hideShorts, keywordText, maxPerChannel) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save feed filters")
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Videos per channel", style = MaterialTheme.typography.titleSmall)
            Text("Limit repeated uploads from one channel in the home feed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2, 3, 5).forEach { limit ->
                    FilterChip(
                        selected = maxPerChannel == limit,
                        onClick = { onSetFeedQuality(hideShorts, keywordText, limit) },
                        label = { Text(limit.toString()) }
                    )
                }
            }
        }

        SettingsSection("Playback source", "Optional third-party playback") {
            Text("Piped", style = MaterialTheme.typography.titleMedium)
            Text("Use a Piped server you choose as an alternate playback source. Off by default.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = urlText, onValueChange = { urlText = it }, singleLine = true,
                label = { Text("Piped API address, https://…") },
                isError = urlText.isNotBlank() && !urlValid,
                supportingText = { if (urlText.isNotBlank() && !urlValid) Text("Enter a valid HTTPS address.") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (piped.enabled) "Enabled" else "Disabled", style = MaterialTheme.typography.titleSmall)
                Switch(
                    checked = piped.enabled, enabled = piped.enabled || urlValid,
                    onCheckedChange = { want -> if (want) showConsent = true else onSetPiped(false, urlText) }
                )
            }
            if (piped.enabled && urlText != piped.instanceUrl && urlValid) {
                OutlinedButton(onClick = { onSetPiped(true, urlText) }) { Text("Save address") }
            }
        }

        SettingsSection("Your library", "Your saved activity stays on this device") {
            ListItem(
                headlineContent = { Text("Import Google Takeout") },
                supportingContent = { Text("Review an archive before anything is saved.") },
                trailingContent = { TextButton(onClick = onImport) { Text("Open") } }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ListItem(
                headlineContent = { Text("Delete library data") },
                supportingContent = { Text("Remove subscriptions, playlists, history and import reports.") },
                trailingContent = { TextButton(onClick = { showDelete = true }) { Text("Delete") } }
            )
        }
        Text("FrameTube · Independent video client", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showConsent) AlertDialog(
        onDismissRequest = { showConsent = false },
        title = { Text("Use Piped playback?") },
        text = { Text("This is an unofficial third-party service and may stop working. The server you choose can see video IDs and your IP address. Frame does not send your account or Takeout files.") },
        confirmButton = { Button(onClick = { showConsent = false; onSetPiped(true, urlText) }) { Text("I understand") } },
        dismissButton = { TextButton(onClick = { showConsent = false }) { Text("Cancel") } }
    )
    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Delete all library data?") },
        text = { Text("Subscriptions, playlists, watch history, searches and import reports will be removed from this device. This can't be undone.") },
        confirmButton = { Button(onClick = { showDelete = false; onDeleteData() }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    detail: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}
