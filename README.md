# FrameTube

FrameTube is a native Android video client built with Kotlin, Jetpack Compose, Room, and Media3. It uses NewPipe Extractor for public video and channel metadata and playback stream resolution. FrameTube is independent and is not affiliated with YouTube or Google.

## Current features

- Search public YouTube videos without signing in.
- Open video details and play supported streams with Media3.
- Like videos locally and resume playback from the last saved position.
- Import supported Google Takeout archives after reviewing a preview.
- Browse imported channels, open channel pages, view their public videos, and save subscriptions locally.
- Browse imported playlists, watch history, and liked videos.
- Download videos only when the source exposes a downloadable stream; manage downloads in the app.
- Optional playback fallback through a Piped instance chosen by the user.

Channel subscriptions and the personal library are stored on the device. They are not synced to a Google account.

## Build

Use JDK 17 or newer and Android SDK platform 35.

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the presentation, domain, data, extraction, and playback boundaries.
See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for dependency and project notices.
