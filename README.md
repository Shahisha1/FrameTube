<div align="center">

# Frame

### Your video library, brought back into focus.

A local-first Android app for exploring your YouTube library, importing your Google Takeout archive, and looking up videos without handing your archive to another service.

**Kotlin · Jetpack Compose · Room · Media3 · NewPipe Extractor**

</div>

---

## Overview

Frame gives your exported YouTube data a place of its own on Android. Import a Google Takeout ZIP, browse the information Frame can recognise, and search both your saved library and YouTube.

Your imported library is stored in a local Room database on your device. Frame does not upload your Takeout archive to a Frame server, and it does not require you to sign in to a Frame account.

> **Project status:** Early development. The current source includes the library, Takeout import flow, online search, and playback integrations, but a successful release build and live-device verification have not yet been confirmed. Expect incomplete features and breaking changes.

## What you can do

- **Bring in your library.** Select a Google Takeout ZIP, review what Frame found, then confirm the import.
- **Browse your history.** Explore imported watch history, subscriptions, playlists, and playlist entries.
- **Search in two places.** Search online YouTube results and your locally imported library from one screen.
- **Open video details.** Resolve online metadata for supported YouTube videos.
- **Play supported streams.** Frame uses NewPipe Extractor to resolve YouTube streams and AndroidX Media3 to play supported formats.
- **Choose a playback fallback.** Optionally configure a Piped instance as a secondary playback source. This is disabled by default.
- **Keep control of your data.** Delete the imported library stored on the device from Settings.
- **Use a dark theme.** Switch between the available light and dark appearances.

### Import coverage

Frame currently looks for these English Google Takeout export paths inside the ZIP:

| Takeout file | Imported information |
| --- | --- |
| `subscriptions/subscriptions.csv` | Channel subscriptions |
| `playlists/playlists.csv` | Playlist records |
| `playlists/<playlist-name>-videos.csv` | Playlist entries |
| `channels/channel.csv` | Channel information |
| `history/watch-history.html` | Watch history |
| `history/search-history.html` | Search history |

The importer reads CSV and HTML entries only. It enforces a 64 MB per-file limit and a 256 MB total read limit.

**Known import limitations**

- Liked videos are not imported yet because a supported source file has not been verified.
- Export layouts and field formats outside the paths above may not be recognised.
- A video ID in an archive is a reference to a video, not a guarantee that the video is still available or playable.
- Re-importing replaces playlist data. Subscriptions, history, searches, and videos are added to the existing library. Items removed from YouTube remain until you delete the local library data.
- Watch-history entries with dates Frame cannot parse are retained without a date and reported as warnings.

## Screens and navigation

- **Home** — library summary and recently watched videos.
- **Channels** — imported subscriptions.
- **Playlists** — imported playlists and their entries.
- **History** — imported watch activity.
- **Liked** — placeholder for future verified liked-video import support.
- **Search** — online YouTube search alongside local library search.
- **Video details and player** — metadata and supported playback.
- **Settings** — theme, optional Piped configuration, Takeout import, and local library deletion.

## Privacy and network behaviour

Frame is designed to keep your imported archive on your device. It has no Frame account system or Frame-hosted sync service.

Some features use the network:

| Feature | Network information |
| --- | --- |
| Takeout import and local library browsing | No upload; the selected archive is read on-device. |
| YouTube search, metadata, and stream resolution | Requests are made to YouTube endpoints through NewPipe Extractor. The remote service can see the request and your network IP address. |
| Optional Piped playback | The Piped instance you configure can see the requested video ID and your network IP address. |
| Local playback | Media is streamed from the resolved source; Frame does not save or download video files. |

YouTube extraction is unofficial and may stop working if YouTube changes its services. It is not an official YouTube feature, and use may be subject to YouTube's terms. Use **Open on YouTube** when you need the official experience.

Piped is optional, disabled by default, and only used as a fallback when enabled with an HTTPS instance address. The instance operator is a separate third party; review its privacy practices before using it.

## Technology

| Component | Role |
| --- | --- |
| Kotlin | Application language |
| Jetpack Compose + Material 3 | UI |
| Room | On-device library database |
| DataStore | User preferences |
| Paging 3 | Large local library lists |
| Kotlin Coroutines and Flow | Asynchronous work and state |
| NewPipe Extractor | YouTube search, metadata, and stream extraction |
| AndroidX Media3 | Media playback |
| Jsoup | HTML parsing for Takeout history |
| KSP | Room code generation |

## Build from source

### Requirements

- Android Studio with Android SDK Platform 35 installed, or an equivalent Android SDK setup
- JDK 17
- Internet access for Gradle dependency resolution

The app's current Android configuration is:

- `minSdk`: 26 (Android 8.0)
- `targetSdk`: 35
- `compileSdk`: 35
- Java/Kotlin JVM target: 17

### Clone

```bash
git clone https://github.com/Shahisha1/FrameTube.git
cd FrameTube
```

### Build a debug APK

```bash
./gradlew :app:assembleDebug
```

The APK is normally written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Run unit tests

```bash
./gradlew :app:testDebugUnitTest
```

Open the project in Android Studio and allow Gradle sync to finish before running it on a device or emulator.

> **Build verification note:** The current implementation has not yet been confirmed by a successful build and test run in a complete Android SDK environment. Treat the commands above as the intended build workflow, not as a claim that the current revision is release-ready.

## Project structure

```text
app/src/main/java/app/frame/video/
├── core/
│   ├── datastore/       # Preferences
│   └── provider/        # Playback and metadata provider contracts
│       ├── newpipe/     # NewPipe downloader and adapters
│       └── piped/       # Optional Piped playback adapter
├── data/
│   ├── importer/        # Takeout ZIP parsing and import models
│   ├── local/           # Room entities, database, and DAO
│   └── repository/      # Local library repository
├── domain/              # Repository contracts
└── ui/
    ├── navigation/      # App routes
    ├── screens/         # Compose screens
    ├── theme/           # Material theme
    └── viewmodel/       # Screen state and application logic
```

## Third-party software and licensing

Frame integrates **NewPipe Extractor** (`com.github.teamnewpipe:NewPipeExtractor:0.27.1`), an independently maintained project distributed under **GNU General Public License v3.0 or later**. See the [NewPipe Extractor repository](https://github.com/TeamNewPipe/NewPipeExtractor) and the included [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

The NewPipe Extractor license may affect how a combined application can be distributed. Review the full license and obtain appropriate legal guidance before publishing binaries or distributing modified versions.

Frame does not include source code from FluxTube. FluxTube was consulted as an architectural reference only.

**Frame's own project license has not yet been declared.** Until a `LICENSE` file is added, do not assume that the Frame source is available for unrestricted reuse, modification, or redistribution.

## Roadmap

The following are potential next steps, not promises of an upcoming release:

- Verify and stabilise the NewPipe integration on real devices.
- Improve search result paging and network error feedback.
- Add tests for the Takeout parser, database, and import transaction flow.
- Investigate a reliable source for importing liked videos.
- Support additional Takeout export layouts and extracted-folder imports.
- Review release signing, distribution, and licensing requirements.

## Contributing

Frame is under active development. Before opening a pull request:

1. Check existing issues and discussions so work does not overlap.
2. Keep changes focused and explain the user-facing impact.
3. Add or update tests when changing parsing, persistence, or provider behaviour.
4. Do not include personal Takeout archives, account data, API credentials, signing keys, or local machine configuration in commits.
5. Include build and test results in the pull request description. If a check could not be run, say so clearly.

## Acknowledgements

- [TeamNewPipe](https://github.com/TeamNewPipe) for NewPipe Extractor.
- [Piped](https://github.com/TeamPiped) for the optional alternative playback ecosystem.
- The AndroidX, Kotlin, and Jetpack Compose teams for the libraries used by Frame.

---

<div align="center">

**Frame** — a home for the videos and history you want to keep close.

</div>
