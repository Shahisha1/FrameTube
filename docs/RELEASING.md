# Signed APK releases

The release workflow is `.github/workflows/release-apk.yml`.

## Required Actions secrets

Set these repository secrets in Settings -> Secrets and variables -> Actions:

- FRAME_RELEASE_KEYSTORE_BASE64: base64 text for the permanent signing keystore
- FRAME_RELEASE_KEYSTORE_PASSWORD: keystore password
- FRAME_RELEASE_KEY_ALIAS: signing key alias
- FRAME_RELEASE_KEY_PASSWORD: key password

Never commit the keystore, passwords, or decoded key file. Keep a secure offline backup of the keystore. Losing it can prevent signed upgrades to existing installs.

## Create a release from GitHub

1. Open the repository's **Actions** tab.
2. Select **Release APK**.
3. Click **Run workflow** and select the `main` branch.
4. Enter the version without the `v` prefix, for example `1.2.3`.
5. Click **Run workflow**.

The workflow validates the version, creates and pushes the annotated tag `v1.2.3`, runs unit tests, builds and verifies the signed release APK, then creates a GitHub Release with the APK attached. The tag points to the exact `main` commit selected when the workflow was started. Existing tags are never overwritten.

## Create a release from the terminal

You can still create and push a tag yourself:

    git checkout main
    git pull origin main
    git tag v1.2.3
    git push origin v1.2.3

Pushing a valid `vMAJOR.MINOR.PATCH` tag starts the same signed APK build and GitHub Release process.

## Security and signing

The tag-preparation job has write access only to create the version tag. The build job has read-only repository access. Only the publish job has `contents: write` permission to create the GitHub Release.

The signing keystore and passwords are read from Actions secrets and are not stored in the repository. The workflow fails early with a clear error if any required secret is missing or invalid. Keep the same signing keystore for future app updates.
