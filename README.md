# FrameTube

FrameTube is a native Android app for browsing and watching videos from YouTube without an account.
Metadata and playback streams come from [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor).
Your library lives only on your device.

FrameTube is not affiliated with YouTube or Google.

## What it does today

- Search YouTube and open a video details page.
- Play videos with Media3 (DASH, HLS or progressive streams).
- Remember what you watch, and resume where you stopped.
- Like videos from the details page.
- Import a Google Takeout archive (subscriptions, playlists, watch history). The import shows a preview
  before anything is saved.
- Browse your library: channels, playlists, history, liked videos.
- Optional playback fallback through a Piped instance you choose (off by default).
- Dark and light themes.

Planned work is tracked in the project plan: channel pages, a subscription feed, user playlists, a richer
player, a configurable multi-instance fallback (Invidious and Piped) and release hardening.

## Build

You need JDK 17 or newer and the Android SDK (platform 35).

```sh
./gradlew testDebugUnitTest      # unit tests
./gradlew assembleDebug          # builds the APK
```

The APK is written to `app/build/outputs/apk/debug/FrameTube.apk`.

CI runs the same steps for every push and pull request (`.github/workflows/build.yml`) and uploads
`FrameTube.apk` as a build artifact.

## Project layout

`app/src/main/java/app/frame/video/` holds the app. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
for the layers: UI (Compose), domain contracts, data (Room, importer, workers) and remote extraction.

## License

FrameTube is free software under the GNU General Public License, version 3 or later. See
[LICENSE](LICENSE). Third-party libraries and reference projects are listed in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
