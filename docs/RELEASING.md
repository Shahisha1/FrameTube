# Signed APK releases

The release workflow is .github/workflows/release-apk.yml.

## Required Actions secrets

Set these repository secrets in Settings -> Secrets and variables -> Actions:

- FRAME_RELEASE_KEYSTORE_BASE64: base64 text for the permanent signing keystore
- FRAME_RELEASE_KEYSTORE_PASSWORD: keystore password
- FRAME_RELEASE_KEY_ALIAS: signing key alias
- FRAME_RELEASE_KEY_PASSWORD: key password

Never commit the keystore, passwords, or decoded key file. Keep a secure offline backup of the keystore. Losing it can prevent signed upgrades to existing installs.

## Trigger

After the changes are reviewed and the secrets are set, push a semantic version tag:

    git checkout main
    git pull origin main
    git tag v1.2.3
    git push origin v1.2.3

The workflow accepts only vMAJOR.MINOR.PATCH tags. It runs unit tests, assembles the signed release APK, checks that exactly one non-empty APK exists, then creates a GitHub Release and attaches the APK. The build job has read-only contents access; only the publish job has contents: write and uses the automatically scoped GITHUB_TOKEN.

No tag or release is created during setup. The first tag run is the first end-to-end check of the configured signing secrets.
