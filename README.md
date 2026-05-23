# Local Medical Vault (V1)

Local-first Android app for families to organize medical documents for children/parents: patients, visits, photos, PDFs and notes.

## Privacy
- 100% local storage (Room + app-private files).
- No backend, no login, no cloud sync.
- No internet permission required by app features.

## Medical disclaimer
This app is only for organizing medical documents. It does not provide medical advice, diagnosis or interpretation.

## Build locally
Requirements:
- JDK 17
- Android SDK installed and configured (`ANDROID_HOME` or `local.properties` with `sdk.dir=...`)

Commands (either wrapper, if available, or installed Gradle):
```bash
gradle test
gradle assembleDebug
```

If you regenerate wrapper locally, you can also use:
```bash
./gradlew test
./gradlew assembleDebug
```

## Install APK
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK path:
- `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions
CI workflow at `.github/workflows/android.yml` runs tests/build and uploads debug APK artifact.

## Backup/Export
From Settings, choose **Export backup** and a folder via SAF.
App creates:
- `MedicalVaultBackup/metadata.json`
- `MedicalVaultBackup/files/`


## Note about this Codex PR
- `gradle/wrapper/gradle-wrapper.jar` is intentionally omitted because the Codex PR tool does not support binary files.
- You can build with Android Studio or an installed Gradle:

```bash
gradle test
gradle assembleDebug
```

- Optional future step (local): regenerate wrapper

```bash
gradle wrapper
```

Then you can also run:

```bash
./gradlew test
./gradlew assembleDebug
```
