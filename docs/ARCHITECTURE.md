# FrameTube architecture

FrameTube keeps its Kotlin, Jetpack Compose, Room, and Media3 stack. FluxTube is an architectural and visual reference, not a source-code dependency.

## Boundaries

- Presentation: Compose screens and ViewModels render state and forward user actions. They do not call NewPipe or HTTP APIs directly.
- Domain: app-owned contracts and normalized models. VideoProvider is the boundary for video metadata and search; ChannelProvider is the boundary for channel profiles and channel video lists.
- Data: Room persistence, Takeout parsing/import, background enrichment, and remote provider implementations.
- Remote extraction: NewPipe initialization, Android HTTP transport, and mapping extractor objects into domain models.
- Playback: stream resolution and NewPipe-first/Piped-fallback behavior stay separate from the UI.
- Composition root: AppContainer wires repositories and providers once.

## Data flows

Search: Compose input -> SearchViewModel -> domain VideoProvider -> NewPipe remote implementation -> normalized VideoSearchResult -> Compose results. Search errors are explicit state and can be retried.

Channels: imported or saved channel -> channel detail route -> ChannelDetailViewModel -> domain ChannelProvider -> NewPipe ChannelInfo and ChannelTabInfo -> normalized channel profile and video metadata. Channel metadata is cached in Room; subscribe/unsubscribe state is stored locally and does not require a Google account.

Takeout metadata: parser -> one Room transaction -> WorkManager enrichment -> NewPipe metadata provider -> Room update. Import does not wait for network access. IDs remain intact when metadata cannot be resolved.

Likes and playback progress: details screen -> LibraryRepository -> Room likes. PlayerViewModel resolves a playable source, restores the last saved position, records one history entry per player visit, and periodically saves progress. Progress persistence is best-effort and never blocks playback.

Playback: details route -> ProviderManager -> NewPipe playback resolver -> configured Piped fallback -> Media3 player.

The structure follows FluxTube's separation of presentation, domain/application contracts, infrastructure, and playback orchestration while preserving FrameTube's native Android stack.
