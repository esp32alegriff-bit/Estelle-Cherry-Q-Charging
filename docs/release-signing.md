# Permanent Android release signing

EF_1.2 establishes the permanent release key for package `za.co.farrusco.estellecherryq`.
Keep the original keystore and passwords in the protected local secrets store, with an offline backup. Never regenerate or rotate the key for routine releases. A lost key prevents normal in-place updates.

GitHub Actions repository secrets:

- `ANDROID_RELEASE_KEYSTORE_BASE64`: base64 of the release keystore file
- `ANDROID_RELEASE_STORE_PASSWORD`: keystore password
- `ANDROID_RELEASE_KEY_ALIAS`: release key alias
- `ANDROID_RELEASE_KEY_PASSWORD`: key password

On main, Actions builds and uploads a signed release APK. A tag named `EF_1.2` publishes that same build to GitHub Releases. Future version tags should follow `EF_*` after updating the app version and release notes. Pull requests build a debug APK without access to the signing secrets.

For a local signed build, set `FARRUSCO_SIGNING_STORE_FILE`, `FARRUSCO_SIGNING_STORE_PASSWORD`, `FARRUSCO_SIGNING_KEY_ALIAS`, and `FARRUSCO_SIGNING_KEY_PASSWORD` in the shell environment, then run `gradle :app:assembleRelease`. Do not add these values to source or logs.
