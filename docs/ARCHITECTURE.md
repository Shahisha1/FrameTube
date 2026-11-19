# FrameTube architecture

FrameTube keeps its Kotlin, Jetpack Compose, Room, and Media3 stack. FluxTube is an architectural and visual reference, not a source-code dependency.

## Boundaries

- Presentation: Compose screens and ViewModels render state and forward user actions. They do not call NewPipe or HTTP APIs directly.
- Domain: app-owned contracts and normalized models. VideoProvider is the boundary for video metadata, search pages and suggestions; ChannelProvider is the boundary for channel profiles and channel video lists.
- Data: Room persistence, Takeout parsing/import, background enrichment, download policy and remote provider implementations.
- Remote extraction: NewPipe initialization, Android HTTP transport, and mapping extractor objects into domain models.
- Playback: stream resolution and NewPipe-first/Piped-fallback behavior stay separate from the UI.
- Composition root: AppContainer wires repositories and providers once.

## Data flows

Search: Compose input -> SearchViewModel -> domain VideoProvider -> NewPipe remote implementation -> normalized video/channel/playlist results. Search suggestions use the service suggestion extractor. Search page cursors stay in the provider layer; recent queries are stored in Room.

Channels: imported or saved channel -> channel detail route -> ChannelDetailViewModel -> domain ChannelProvider -> NewPipe ChannelInfo and ChannelTabInfo -> normalized channel profile and video metadata. Channel metadata is cached in Room; subscribe/unsubscribe state is stored locally and does not require a Google account.

Subscription feed: Room joins subscribed channel IDs to cached channel URLs -> DiscoveryViewModel -> bounded concurrent ChannelProvider requests -> merged, de-duplicated public video list -> local feed-quality filter -> Home. Requests are capped to a small channel count and run with a per-channel timeout. The filter can hide short uploads, muted title terms and repeated uploads from the same channel. These are local settings; no Google account or personalized recommendation profile is used.

Playlists: Compose actions -> LibraryViewModel -> LibraryRepository -> Room playlist and entry operations. Local playlist changes do not mutate remote YouTube playlists. Imported playlists remain editable as local copies.

Takeout metadata: parser -> one Room transaction -> WorkManager enrichment -> NewPipe metadata provider -> Room update. Import does not wait for network access. IDs remain intact when metadata cannot be resolved.

Likes and playback progress: details screen -> LibraryRepository -> Room likes. PlayerViewModel resolves a playable source, restores the last saved position, records one history entry per player visit, and periodically saves progress. Media3 controls support seeking and playback speed. SponsorBlock segment lookup is an optional data-layer HTTP client; the player requests only selected categories and seeks over returned skip ranges. API failures never interrupt playback. Progress persistence is best-effort and never blocks playback.

Playback: details route -> ProviderManager -> NewPipe playback resolver -> configured Piped fallback -> Media3 player. Fallback isolates provider exceptions but still propagates caller cancellation.

Offline downloads: direct HTTPS URL -> DirectDownloadPolicy -> Android DownloadManager -> app-private external Movies directory -> Media3 local player. The policy rejects YouTube pages and known YouTube CDN hosts, and accepts only supported direct video file extensions.

Release: version tag -> signed Gradle release build -> APK verification -> GitHub Release asset. Signing material is read only from Actions secrets.

The structure follows FluxTube's separation of presentation, domain/application contracts, infrastructure, and playback orchestration while preserving FrameTube's native Android stack. UI navigation takes cues from Metrolist's compact mobile navigation and wider-screen rail patterns, adapted with Frame's own Material 3 colors, labels and layouts.
