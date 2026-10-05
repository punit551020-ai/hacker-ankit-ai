# HACKER ANKIT AI

Android-only futuristic voice assistant project. Package ID: `com.hackerankit.ai`.

## Features
- Cinematic HACKER ANKIT intro with dark neon HUD styling.
- AI ASSISTANT dashboard with status, orb, command input and voice controls.
- Android SpeechRecognizer for user-triggered voice commands.
- Android TextToSpeech with device voice detection/selection.
- Custom assistant name stored locally.
- Command router for supported app launches: YouTube, Instagram, WhatsApp, Chrome, GitHub, Settings, Gallery and Camera.
- Torch control through Android CameraManager.
- User-visible foreground-service mode with a persistent notification.
- Permission-aware microphone and notification UX.
- Local-first: no mandatory paid API and no embedded secret keys.
- GitHub Actions debug-build workflow.

## Requirements
- Android Studio with a recent Android SDK.
- JDK 17 for the project configuration.
- Android SDK Platform 35.
- Android 8.0 (API 26) or newer device/emulator.
- Internet is needed by Gradle the first time dependencies are downloaded.

## Android Studio setup
1. Open Android Studio.
2. Choose **Open** and select the `Hacker-Ankit-AI` folder.
3. Let Gradle sync.
4. Install Android SDK Platform 35 if Android Studio asks.
5. Connect an Android device with USB debugging enabled, or start an Android emulator.
6. Run the `app` configuration.

This project contains no Mac/iOS target. It is Android-only.

## Build APK
```bash
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

On Windows:
```bat
gradlew.bat assembleDebug
```

## Build AAB
```bash
./gradlew bundleRelease
```
A release build should be signed with your own private signing key. Never commit the keystore or passwords to GitHub.

## Install APK
Copy `app-debug.apk` to your Android phone and install it. Android may ask you to allow installation from the source used to open the APK.

## Permissions
- `RECORD_AUDIO`: user-started voice recognition.
- `POST_NOTIFICATIONS`: active-assistant notification on Android 13+.
- `FOREGROUND_SERVICE` + microphone foreground-service declaration: explicit active-service mode.
- Camera/flash capability is queried for torch control.
- Contacts/phone permissions are reserved for extended calling flows; the current call command opens the Android dialer rather than silently placing a call.

Denying permissions should not crash the app.

## Voice setup
Open **SELECT VOICE**. The app lists compatible Android TTS voices present on the device. If a requested voice does not exist, available device voices are shown instead.

Speech recognition depends on the device's installed recognition service and language support.

## Assistant name
Use **Assistant name** to save a custom name such as Ankit, Nova or another name. It is stored locally in Android SharedPreferences.

## Foreground service
Turning **ASSISTANT ACTIVE** on starts a visible Android foreground service and notification. Turning it off stops the service.

The service does **not** secretly record or continuously capture microphone audio. Voice recognition is explicitly started by the user.

True always-on wake-word detection is not included. A future wake-word model can be added behind explicit opt-in and Android-compliant foreground-service design.

## GitHub publishing
Create a public GitHub repository, for example `Hacker-Ankit-AI`, then:
```bash
git init
git add .
git commit -m "Initial Android Hacker Ankit AI project"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/Hacker-Ankit-AI.git
git push -u origin main
```
Do not commit API keys, passwords, OAuth tokens, signing keys, `.jks`/`.keystore`, `local.properties`, or `.env`.

## GitHub Actions
`.github/workflows/android.yml` runs a debug build on pushes and pull requests. It installs Android SDK Platform 35 and uses JDK 17.

The generated environment did not have an Android SDK or Gradle installation and could not download the Gradle wrapper JAR, so an Android build was **not executed here**. If needed, regenerate the official wrapper from a trusted Gradle installation:
```bash
gradle wrapper --gradle-version 8.10.2
```

## Troubleshooting
### Gradle sync fails
Install JDK 17 and Android SDK Platform 35, then sync again.
### Speech recognition unavailable
Check for a supported speech recognition service and microphone permission.
### Supported app not installed
The assistant reports that the app is unavailable instead of crashing.
### Torch unavailable
Some devices/emulators do not expose a camera flash; the app reports this gracefully.
### Notification does not appear
On Android 13+, grant notification permission and keep foreground mode explicitly enabled.

## Privacy
The project is designed for transparent, user-controlled assistant features. It does not implement hidden microphone recording, password/cookie/session theft, permission bypass, notification hiding, or unauthorized remote device control.

## Limitations
- Natural-language understanding is a lightweight local command router, not a general-purpose cloud LLM.
- Android SpeechRecognizer and TTS capabilities vary by device.
- Third-party app package names can vary by vendor/version.
- Calling is intentionally conservative: the current command opens the system dialer rather than placing an unsolicited call.
- Always-on hotword detection is not implemented.
- Android SDK/Gradle were unavailable in the generation environment, so compilation/lint could not be truthfully reported as passed.
