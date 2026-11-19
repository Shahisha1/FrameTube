# Offline downloads

FrameTube has an in-app download list for direct video files that the user is allowed to save.

## Supported files

- MP4 and M4V
- WebM
- MKV
- MOV
- AVI

Paste a direct HTTPS file URL in the Downloads screen. DirectDownloadPolicy validates the URL scheme, host and file extension before Android DownloadManager starts the transfer. The app tracks the download ID, shows progress, lets the user remove a download, and plays completed files with Media3 while offline.

Files are stored in the app's external Movies directory. They are removed when the app is uninstalled or when the user removes the download.

## YouTube links

FrameTube does not download YouTube pages, extracted YouTube streams, DASH/HLS manifests, or Google Video CDN URLs. The offline feature is for direct media files from sources that permit downloading, including the user's own hosted videos. It does not bypass DRM, Premium restrictions, or access controls.

## Limits

- HTTPS is required.
- Only direct file URLs with a supported video extension are accepted.
- Streaming playlists and DRM-protected media are not supported.
- The app cannot prove that a user has permission to save a third-party file; only download media the user is allowed to keep.
- The download list is local to this app install.
