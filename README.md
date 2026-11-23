# RICE Android 0.2.0 — local-only foundation

Clean rebuild for Android 8.1 (API 27) and newer. It installs alongside the old 0.1.x package so the previous app and its private data are not removed during testing.

## Included
- No cloud services, remote APIs, login, Firebase, or INTERNET permission.
- Local notes and task list stored on the device in SharedPreferences.
- Simple local research-task suggestions (not an AI language model).
- Crash logger initialized in Application before Activity/UI startup.
- Public log: `Documents/RICE/RICE_LOG.txt` after storage permission is granted.
- Manual export through Android's file picker if public storage is unavailable.
- Recovery screen if UI initialization fails.

## Build on GitHub
Upload the contents of this archive to the repository root, preserving folders. Open Actions and run **Build RICE APK**. Download artifact `RICE-0.2.0-debug-apk`.

## Compatibility
- minSdk 27 (Android 8.1)
- targetSdk 28 for legacy external-storage compatibility
- JDK 17, Gradle 8.7, Android Gradle Plugin 8.6.1

## Log location
`Общая память телефона/Documents/RICE/RICE_LOG.txt`

If permission is denied, use **Экспортировать журнал** and choose Downloads in the Android file picker. A private backup is also kept inside app storage.

## Signing note
The included keystore is a development-only debug key so APKs built by separate GitHub runners can update the same installation. Never use it for a public release.

This foundation does not yet contain a local LLM or a background autonomous reasoning engine.
