# SignAssist Android App

This is the Android frontend for SignAssist. It connects to:

- .NET backend for users, feedback, history, learning, and admin APIs
- Python ASL backend for sign-to-text, live prediction, and text-to-sign assets

## Folder

`D:\FYP\SignAssistAp`

## Requirements

- Android Studio
- JDK from Android Studio or Java 17
- Android SDK installed
- Both backend services running before testing on a phone

## Backend URLs

Current URLs are configured in:

```text
app/src/main/java/com/example/signassistap/network/ApiClient.kt
```

Current values:

```kotlin
private const val BASE_URL = "http://192.168.0.33:5140/"
private const val ASL_BASE_URL = "http://192.168.0.33:8000/"
```

Use your PC LAN IP when testing on a real phone. The phone and PC must be on the same Wi-Fi network.

For Android Emulator, use:

```text
http://10.0.2.2:5140/
http://10.0.2.2:8000/
```

## Build Debug APK

From this folder:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Run From Android Studio

1. Open `D:\FYP\SignAssistAp` in Android Studio.
2. Make sure both backend services are running.
3. Confirm `ApiClient.kt` uses the correct PC IP address.
4. Connect your phone with USB debugging or use an emulator.
5. Click Run.

## Required Backend Services

.NET backend:

```text
http://192.168.0.33:5140/
```

Python ASL backend:

```text
http://192.168.0.33:8000/
```

## Main Features

- User login/signup
- Admin login through same login screen
- Text-to-sign with video/image sequence playback
- Live sign-to-text camera detection
- Learning alphabet signs
- Feedback storage
- Translation history storage
- Admin dashboard with users, feedback, and Text-to-Sign data

## Admin Login

```text
Email: admin@gmail.com
Password: admin123
```

## Notes

- The user dashboard does not show Admin or Upload tabs.
- Live prediction depends on the Python backend model checkpoint.
- If the app shows connection errors, check the PC IP, backend ports, phone Wi-Fi, and firewall.
