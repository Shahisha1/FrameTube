# Third-party notices

FrameTube is licensed under the GNU General Public License, version 3 or (at your option) any later
version (`GPL-3.0-or-later`). See [LICENSE](LICENSE).

FrameTube is an independent project. It is not affiliated with, endorsed by, or sponsored by YouTube
or Google. "YouTube" is a trademark of Google LLC and is used here only to describe the service the
app can read from.

## Libraries bundled in the app

| Library | License |
|---|---|
| [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) (`com.github.TeamNewPipe:NewPipeExtractor:0.27.1`) | GPL-3.0-or-later (per the project's published terms) |
| [jsoup](https://jsoup.org/) | MIT |
| AndroidX (Core, Activity, Lifecycle, Navigation, Room, Paging, DataStore, WorkManager) | Apache-2.0 |
| Jetpack Compose and Material 3 | Apache-2.0 |
| AndroidX Media3 (ExoPlayer) | Apache-2.0 |
| [Coil](https://coil-kt.github.io/coil/) | Apache-2.0 |
| [desugar_jdk_libs](https://github.com/google/desugar_jdk_libs) | See the upstream repository for its license terms |

NewPipe Extractor is the reason FrameTube is distributed under the GPL: an app that bundles it must be
distributable under GPL-3.0 terms, with its source available to recipients. The Apache-2.0 and MIT
libraries above are compatible with GPL-3.0.

Please re-check the exact license of every dependency version you ship before redistributing a build.
The table above was compiled from each project's published license, not from an automated scan.

## Reference projects (inspiration only, no code or assets used)

These projects were looked at for feature ideas and user flows only. No source code, graphics, icons,
strings or distinctive design from them is included in FrameTube.

| Project | License (as shown on its GitHub page, checked 2026-11-19) |
|---|---|
| [FreeTube](https://github.com/FreeTubeApp/FreeTube) | AGPL-3.0 |
| [SkyTube](https://github.com/SkyTubeTeam/SkyTube) | GPL-3.0 |
| [LibreTube](https://github.com/libre-tube/LibreTube) | GPL-3.0 |
| [FluxTube](https://github.com/mu-fazil-vk/FluxTube) | GPL-3.0 |

If code from any of these projects is ever adopted, it must go through a per-file review first:

- Keep the original copyright notices and license headers, and list the source here.
- GPL-3.0 code can be combined with FrameTube. If a file is licensed "GPL-3.0-only" (not "or later"),
  the combined app is effectively GPL-3.0-only for that part.
- AGPL-3.0 code (FreeTube) may be combined with GPL-3.0 code, but the AGPL network-use clause then
  applies to the combined work. Avoid this unless you have decided you want it.

## Optional network services

FrameTube can use a Piped instance chosen by the user as a playback fallback. It talks to that server
over HTTP only. No Piped or Invidious source code is included in the app. The server sees the IDs of the
videos you play and your IP address.
