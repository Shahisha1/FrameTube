# FrameTube

FrameTube is a native Android video client built with Kotlin, Jetpack Compose, Room, and Media3. It uses NewPipe Extractor for public video and channel metadata and playback stream resolution. FrameTube is independent and is not affiliated with YouTube or Google.

## Current features

- Search public videos, channels and playlists with filters, suggestions, recent searches and next-page loading.
- Open video details and play supported streams with Media3.
- Like videos locally, resume playback, seek by 10 seconds and change playback speed.
- Skip community-submitted SponsorBlock segments with configurable category controls.
- Filter the subscribed-channel feed by muted title terms, short-form duration and per-channel repetition limits.
- Import supported Google Takeout archives after reviewing a preview.
- Browse imported channels, open channel pages, save subscriptions locally, and see public uploads from subscribed channels on Home.
- Create, edit and delete local playlists; add or remove videos; browse imported playlists, watch history and liked videos.
- Download direct HTTPS media files from sources that permit saving. YouTube pages and extracted YouTube streams are blocked.
- Optional playback fallback through a Piped instance chosen by the user.

Channel subscriptions and the personal library are stored on the device. They are not synced to a Google account. The home recommendations are a locally filtered view of recent public uploads from subscribed channels, not a personalized Google recommendation profile. When SponsorBlock is enabled, FrameTube sends the current video ID to the public SponsorBlock API to retrieve segment times; no account identity is sent.

## Build

Use JDK 17 or newer and Android SDK platform 35.

    ./gradlew testDebugUnitTest
    ./gradlew assembleDebug

The debug APK is generated under app/build/outputs/apk/debug/.

## CI and releases

Pushes to main and pull requests targeting main run Android CI. The workflow runs unit tests, builds a debug APK, checks that the APK is non-empty, and uploads FrameTube-debug as a downloadable Actions artifact for 7 days.

A signed release APK is built and attached to a GitHub Release only when a strict version tag such as v1.2.3 is pushed. Set these repository Actions secrets first:

- FRAME_RELEASE_KEYSTORE_BASE64
- FRAME_RELEASE_KEYSTORE_PASSWORD
- FRAME_RELEASE_KEY_ALIAS
- FRAME_RELEASE_KEY_PASSWORD

The keystore and passwords are not stored in the repository. Keep a secure backup of the signing keystore; future app updates must use the same signing key.

To start a release after reviewing the source and confirming the secrets:

    git checkout main
    git pull origin main
    git tag v1.2.3
    git push origin v1.2.3

No release tag is created by this repository setup alone.

## Architecture

See docs/ARCHITECTURE.md for the presentation, domain, data, extraction, and playback boundaries.
See docs/RELEASING.md for signed release setup.
See docs/OFFLINE_DOWNLOADS.md for offline download limits.
See docs/FEATURES.md for SponsorBlock, feed filtering and UI behavior.
See THIRD_PARTY_NOTICES.md for dependency and project notices.
