package app.frame.video.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.dp
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
import app.frame.video.core.datastore.FramePreferences
import app.frame.video.core.datastore.PipedConfig
import app.frame.video.domain.playback.PlaybackQueueEntry
import app.frame.video.domain.playback.PlaybackQueueMode
import app.frame.video.ui.navigation.Routes
import app.frame.video.ui.screens.ChannelsScreen
import app.frame.video.ui.screens.SubscriptionsScreen
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
import app.frame.video.ui.screens.LibraryScreen
import app.frame.video.ui.screens.ShortsScreen
import app.frame.video.ui.viewmodel.ImportViewModel
import app.frame.video.ui.viewmodel.ChannelDetailViewModel
import app.frame.video.ui.viewmodel.LibraryViewModel
import app.frame.video.ui.viewmodel.PlayerViewModel
import app.frame.video.ui.viewmodel.SearchViewModel
import app.frame.video.ui.viewmodel.DiscoveryViewModel
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
    val discovery: DiscoveryViewModel = viewModel(factory = viewModelFactory { initializer { DiscoveryViewModel(container.library, container.channelProvider, container.providerManager.metadataProvider("newpipe")!!) } })
    val importVm: ImportViewModel = viewModel(factory = viewModelFactory { initializer { ImportViewModel(container.importer, container.contentResolver, container.metadataEnrichmentScheduler) } })
    val piped by container.preferences.pipedConfig.collectAsStateWithLifecycle(initialValue = PipedConfig(false, ""))
    val sponsorBlockEnabled by container.preferences.sponsorBlockEnabled.collectAsStateWithLifecycle(initialValue = false)
    val sponsorBlockCategories by container.preferences.sponsorBlockCategories.collectAsStateWithLifecycle(initialValue = FramePreferences.DEFAULT_SPONSORBLOCK_CATEGORIES)
    val hideShorts by container.preferences.hideShorts.collectAsStateWithLifecycle(initialValue = false)
    val mutedKeywords by container.preferences.mutedKeywords.collectAsStateWithLifecycle(initialValue = "")
    val maxPerChannel by container.preferences.maxPerChannel.collectAsStateWithLifecycle(initialValue = 3)
    val accentPalette by container.preferences.accentPalette.collectAsStateWithLifecycle(initialValue = "red")

    val items = listOf(
        NavItem(Routes.HOME, "Home", Icons.Default.Home),
        NavItem(Routes.SHORTS, "Shorts", Icons.Default.PlayArrow),
        NavItem(Routes.SUBSCRIPTIONS, "Subscriptions", Icons.Default.Subscriptions),
        NavItem(Routes.LIBRARY, "Library", Icons.Default.VideoLibrary)
    )
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showNavigation = items.any { destination?.hierarchy?.any { d -> d.route == it.route } == true }
    val isShortsDestination = destination?.hierarchy?.any { it.route == Routes.SHORTS } == true
    val isSearchDestination = destination?.hierarchy?.any { it.route == Routes.SEARCH } == true
    val isWatchDestination = destination?.hierarchy?.any { it.route == Routes.PLAYER || it.route == Routes.DETAILS || it.route == Routes.LOCAL_PLAYER } == true
    val openVideo = { id: String -> nav.navigate(Routes.player(id)) }
    val openImport = { nav.navigate(Routes.IMPORT) }
    val openSearch = { nav.navigate(Routes.SEARCH) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isShortsDestination && !isSearchDestination && !isWatchDestination) TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.padding(end = 6.dp)
                                .background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text("FrameTube", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = openSearch) { Icon(Icons.Default.Search, contentDescription = "Search videos") }
                    IconButton(onClick = { nav.navigate(Routes.DOWNLOADS) }) { Icon(Icons.Default.Download, contentDescription = "Downloads") }
                    IconButton(onClick = { nav.navigate(Routes.SETTINGS) }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (showNavigation && !isShortsDestination) NavigationBar(
                modifier = Modifier.height(64.dp),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
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
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }
    ) { padding ->
        Row(Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController = nav, startDestination = Routes.HOME, modifier = Modifier.weight(1f)) {
                composable(Routes.HOME) {
                    HomeScreen(
                        library, discovery,
                        onOpenImport = { openImport() },
                        onOpenChannels = { nav.navigate(Routes.SUBSCRIPTIONS) },
                        onOpenSearch = { openSearch() },
                        onExploreTopic = { topic ->
                            search.setQuery(topic)
                            search.searchNow()
                            openSearch()
                        },
                        onOpenVideo = openVideo,
                        onOpenRecommendation = { id, videos ->
                            val entries = videos.map { PlaybackQueueEntry(it.videoId, it.title.orEmpty(), it.thumbnailUrl) }
                            val startIndex = entries.indexOfFirst { it.videoId == id }.coerceAtLeast(0)
                            container.playbackSession.setQueue(entries, PlaybackQueueMode.AUTOPLAY, startIndex)
                            openVideo(id)
                        },
                        hideShorts = hideShorts,
                        mutedKeywords = mutedKeywords,
                        maxPerChannel = maxPerChannel
                    )
                }
                composable(Routes.SHORTS) {
                    ShortsScreen(
                        discovery = discovery,
                        providers = container.providerManager,
                        library = container.library,
                        session = container.playbackSession,
                        onOpenVideo = openVideo,
                        onClose = {
                            nav.navigate(Routes.HOME) {
                                popUpTo(nav.graph.findStartDestination().id) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        onOpenSearch = { openSearch() }
                    )
                }
                composable(Routes.SUBSCRIPTIONS) { SubscriptionsScreen(library, discovery, onImport = { openImport() }, onOpenChannel = { nav.navigate(Routes.channel(it)) }, onOpenVideo = openVideo) }
                composable(Routes.LIBRARY) {
                    LibraryScreen(
                        vm = library,
                        downloads = container.downloads,
                        onOpenVideo = openVideo,
                        onOpenChannel = { nav.navigate(Routes.channel(it)) },
                        onOpenPlaylist = { nav.navigate(Routes.playlist(it)) },
                        onPlayDownload = { id -> nav.navigate(Routes.localPlayer(id)) },
                        onImport = openImport
                    )
                }
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
                    PlaylistDetailScreen(entry.arguments?.getString("playlistId").orEmpty(), library, openVideo) { nav.popBackStack() }
                }
                composable(Routes.HISTORY) { HistoryScreen(library, openVideo, onImport = { openImport() }) }
                composable(Routes.LIKED) { LikedScreen(library, openVideo, onImport = { openImport() }) }
                composable(Routes.SEARCH) { SearchScreen(search, openVideo, onOpenChannel = { url -> nav.navigate(Routes.channel(url)) }, onBack = { nav.popBackStack() }) }
                composable(Routes.DOWNLOADS) { DownloadsScreen(container.downloads) { id -> nav.navigate(Routes.localPlayer(id)) } }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        darkTheme = darkTheme,
                        onToggleTheme = onToggleTheme,
                        dynamicColor = dynamicColor,
                        onToggleDynamicColor = onToggleDynamicColor,
                        accentPalette = accentPalette,
                        onSetAccentPalette = { palette -> scope.launch { container.preferences.setAccentPalette(palette) } },
                        piped = piped,
                        onSetPiped = { enabled, url -> scope.launch { container.preferences.setPiped(enabled, url.trim()) } },
                        sponsorBlockEnabled = sponsorBlockEnabled,
                        sponsorBlockCategories = sponsorBlockCategories,
                        onSetSponsorBlock = { enabled, categories -> scope.launch { container.preferences.setSponsorBlock(enabled, categories) } },
                        hideShorts = hideShorts,
                        mutedKeywords = mutedKeywords,
                        maxPerChannel = maxPerChannel,
                        onSetFeedQuality = { hide, keywords, limit -> scope.launch { container.preferences.setFeedQuality(hide, keywords, limit) } },
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
                    PlayerScreen(
                        id,
                        vm,
                        container.playbackSession,
                        container.preferences,
                        onBack = { nav.popBackStack() },
                        onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                        onQueueItemChanged = { nextId ->
                            nav.navigate(Routes.player(nextId)) {
                                popUpTo(Routes.PLAYER) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        library = container.library,
                        metadataProvider = container.providerManager.metadataProvider("newpipe")!!
                    )
                }
                composable(Routes.LOCAL_PLAYER) { entry ->
                    val id = entry.arguments?.getString("downloadId")?.toLongOrNull()
                    if (id != null) LocalDownloadPlayerScreen(id, container.downloads) { nav.popBackStack() } else Text("Invalid download")
                }
            }
        }
    }
}
