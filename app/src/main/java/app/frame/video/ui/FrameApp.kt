package app.frame.video.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.frame.video.AppContainer
import app.frame.video.core.datastore.PipedConfig
import app.frame.video.ui.navigation.Routes
import app.frame.video.ui.screens.ChannelsScreen
import app.frame.video.ui.screens.ChannelDetailScreen
import app.frame.video.ui.screens.DetailsScreen
import app.frame.video.ui.screens.DownloadsScreen
import app.frame.video.ui.screens.HistoryScreen
import app.frame.video.ui.screens.HomeScreen
import app.frame.video.ui.screens.ImportScreen
import app.frame.video.ui.screens.LikedScreen
import app.frame.video.ui.screens.LocalDownloadPlayerScreen
import app.frame.video.ui.screens.PlayerScreen
import app.frame.video.ui.screens.PlaylistDetailScreen
import app.frame.video.ui.screens.PlaylistsScreen
import app.frame.video.ui.screens.SearchScreen
import app.frame.video.ui.screens.SettingsScreen
import app.frame.video.ui.viewmodel.ImportViewModel
import app.frame.video.ui.viewmodel.ChannelDetailViewModel
import app.frame.video.ui.viewmodel.LibraryViewModel
import app.frame.video.ui.viewmodel.PlayerViewModel
import app.frame.video.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameApp(
    container: AppContainer,
    darkTheme: Boolean,
    dynamicColor: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val library: LibraryViewModel = viewModel(factory = viewModelFactory { initializer { LibraryViewModel(container.library) } })
    val search: SearchViewModel = viewModel(factory = viewModelFactory { initializer { SearchViewModel(container.library, container.providerManager.metadataProvider("newpipe")!!) } })
    val importVm: ImportViewModel = viewModel(factory = viewModelFactory { initializer { ImportViewModel(container.importer, container.contentResolver, container.metadataEnrichmentScheduler) } })
    val piped by container.preferences.pipedConfig.collectAsStateWithLifecycle(initialValue = PipedConfig(false, ""))

    val items = listOf(
        NavItem(Routes.HOME, "Home", Icons.Default.Home),
        NavItem(Routes.SUBSCRIPTIONS, "Channels", Icons.Default.Subscriptions),
        NavItem(Routes.PLAYLISTS, "Playlists", Icons.Default.List),
        NavItem(Routes.HISTORY, "History", Icons.Default.History),
        NavItem(Routes.LIKED, "Liked", Icons.Default.Favorite)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBottomBar = items.any { destination?.hierarchy?.any { d -> d.route == it.route } == true }
    val openVideo = { id: String -> nav.navigate(Routes.details(id)) }
    val openImport = { nav.navigate(Routes.IMPORT) }
    val openSearch = { nav.navigate(Routes.SEARCH) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FRAME", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        Text("YOUR VIDEO LIBRARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate(Routes.DOWNLOADS) }) {
                        Icon(Icons.Default.Download, contentDescription = "Downloads")
                    }
                    IconButton(onClick = openSearch) { Icon(Icons.Default.Search, contentDescription = "Search videos") }
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (showBottomBar) NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = destination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            nav.navigate(item.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { HomeScreen(library, onOpenImport = { openImport() }, onOpenSearch = { openSearch() }, onOpenVideo = openVideo) }
            composable(Routes.SUBSCRIPTIONS) { ChannelsScreen(library, onImport = { openImport() }, onOpenChannel = { nav.navigate(Routes.channel(it)) }) }
            composable(Routes.PLAYLISTS) { PlaylistsScreen(library, onOpenPlaylist = { nav.navigate(Routes.playlist(it)) }, onImport = { openImport() }) }
            composable(Routes.CHANNEL) { entry ->
                val id = entry.arguments?.getString("channelId").orEmpty()
                val channelVm: ChannelDetailViewModel = viewModel(
                    key = "channel-$id",
                    factory = viewModelFactory { initializer { ChannelDetailViewModel(id, container.library, container.channelProvider) } }
                )
                ChannelDetailScreen(channelVm, openVideo)
            }
            composable(Routes.PLAYLIST) { entry ->
                PlaylistDetailScreen(entry.arguments?.getString("playlistId").orEmpty(), library, openVideo)
            }
            composable(Routes.HISTORY) { HistoryScreen(library, openVideo, onImport = { openImport() }) }
            composable(Routes.LIKED) { LikedScreen(library, openVideo, onImport = { openImport() }) }
            composable(Routes.SEARCH) { SearchScreen(search, openVideo) { url -> nav.navigate(Routes.channel(url)) } }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(container.downloads) { id -> nav.navigate(Routes.localPlayer(id)) }
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    darkTheme = darkTheme,
                    onToggleTheme = onToggleTheme,
                    dynamicColor = dynamicColor,
                    onToggleDynamicColor = onToggleDynamicColor,
                    piped = piped,
                    onSetPiped = { enabled, url -> scope.launch { container.preferences.setPiped(enabled, url.trim()) } },
                    onImport = { openImport() },
                    onDeleteData = { library.deleteAll { } }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(importVm, onOpenLibrary = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } })
            }
            composable(Routes.DETAILS) { entry ->
                val id = entry.arguments?.getString("videoId").orEmpty()
                DetailsScreen(id, container.library, container.providerManager.metadataProvider("newpipe")!!, onPlay = { nav.navigate(Routes.player(id)) })
            }
            composable(Routes.PLAYER) { entry ->
                val id = entry.arguments?.getString("videoId").orEmpty()
                val vm: PlayerViewModel = viewModel(key = "player-$id", factory = viewModelFactory { initializer { PlayerViewModel(id, container.providerManager, container.library) } })
                PlayerScreen(id, vm, onBack = { nav.popBackStack() }, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.LOCAL_PLAYER) { entry ->
                val id = entry.arguments?.getString("downloadId")?.toLongOrNull()
                if (id != null) LocalDownloadPlayerScreen(id, container.downloads) { nav.popBackStack() }
                else Text("Invalid download")
            }
        }
    }
}
