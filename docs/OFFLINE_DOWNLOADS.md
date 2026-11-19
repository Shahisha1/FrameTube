# Offline downloads

Frame has an in-app download list for direct video files that the user is allowed to save.

## Supported files

- MP4
- WebM
- MKV
- MOV
- AVI

Paste a direct HTTPS file URL in the Downloads screen. Android DownloadManager handles the transfer and notification. Frame tracks the download ID, shows progress, lets the user remove a download, and plays completed files with Media3 while offline.

Files are stored in the app's external Movies directory. They are removed when the app is uninstalled or when the user removes the download.

## YouTube links

Frame does not download YouTube pages, extracted YouTube streams, DASH/HLS manifests, or Google Video CDN URLs. The offline feature is for direct media files from sources that permit downloading, including the user's own hosted videos. It does not bypass DRM, Premium restrictions, or access controls.

## Limits

- HTTPS is required.
- Only direct file URLs with a supported video extension are accepted.
- Streaming playlists and DRM-protected media are not supported.
- The download list is local to this app install.
