# ASL System

This folder contains the complete SignAssist/ASL demo system prepared for a clean GitHub repository.

## Folder Structure

```text
ASL_System/
  ASL_backend_Api_System/       .NET backend API
  ASL_Trained_model_system/     Python ASL model/API backend
  ASL_Mobile_Application_system Android mobile app frontend
```

The original project folders are still kept in `D:\FYP`; this folder is a copied, renamed package for pushing to a new repo.

## Services

Run the services in this order:

1. PostgreSQL database
2. .NET backend API on port `5140`
3. Python ASL backend on port `8000`
4. Android app on phone/emulator

## 1. .NET Backend API

Folder:

```text
ASL_backend_Api_System
```

Purpose:

- User signup/login/password reset
- Feedback API
- Translation history API
- Learning API
- Admin APIs for users, feedback, and Text-to-Sign history
- Database migrations/table setup

Run:

```powershell
cd ASL_backend_Api_System
dotnet restore
dotnet run --urls http://0.0.0.0:5140
```

Verify:

```text
http://localhost:5140/swagger
```

Database:

The default PostgreSQL connection is in `appsettings.json`:

```json
"Host=localhost;Port=5432;Database=SIGN_LANGUAGE_DATABASE;Username=postgres;Password=12345"
```

Update it on another system if the PostgreSQL username/password/database is different.

## 2. Python ASL Backend

Folder:

```text
ASL_Trained_model_system
```

Purpose:

- Sign-to-text API
- Live camera frame prediction API
- Text-to-sign video/image asset API
- Learning alphabet sign API
- Model loading and inference

Install:

```powershell
cd ASL_Trained_model_system
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

Run:

```powershell
.\.venv\Scripts\python.exe -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

Verify:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Current model file included:

```text
models/word_signnet_best.pth
```

Current missing optional fallback file:

```text
models/signnet_best.pth
```

That missing file is only needed for static-letter fallback. The current live word prediction uses `word_signnet_best.pth`.

After you provide the Sign-MNIST CSV files, place them here before training static letters:

```text
ASL_Trained_model_system/archive/sign_mnist_train/sign_mnist_train.csv
ASL_Trained_model_system/archive/sign_mnist_test/sign_mnist_test.csv
```

Then train:

```powershell
.\.venv\Scripts\python.exe train.py
```

Word model training:

```powershell
.\.venv\Scripts\python.exe train_words.py
```

## 3. Android Mobile Application

Folder:

```text
ASL_Mobile_Application_system
```

Purpose:

- User login/signup
- Admin login from the same login page
- Text-to-sign playback
- Live sign-to-text camera prediction
- Learning alphabet
- Feedback
- Translation history
- Admin dashboard

Admin login:

```text
Email: admin@gmail.com
Password: admin123
```

Backend URLs are configured in:

```text
ASL_Mobile_Application_system/app/src/main/java/com/example/signassistap/network/ApiClient.kt
```

Current values:

```kotlin
private const val BASE_URL = "http://192.168.0.33:5140/"
private const val ASL_BASE_URL = "http://192.168.0.33:8000/"
```

When running on another computer, update `192.168.0.33` to that computer's LAN IP address.

For Android Emulator, use:

```text
http://10.0.2.2:5140/
http://10.0.2.2:8000/
```

Build APK:

```powershell
cd ASL_Mobile_Application_system
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Main Completed Changes

Backend API:

- Added translation history storage per user.
- Added feedback storage per user as an array.
- Added admin APIs for users, feedback, and Text-to-Sign history.
- Added learning APIs for alphabet signs.
- Added last-24-hours history handling.
- Added database table creation for history and feedback.

Python ASL backend:

- Added sign-to-text and live-frame APIs.
- Added text-to-sign sequence handling for sentence input.
- Added word-video plus letter-image fallback behavior.
- Added learning alphabet asset APIs.
- Added model preload at API startup.
- Improved live prediction latency:
  - Android sends frames every `220ms`.
  - Backend requires `10` live frames instead of `24`.
  - Backend resizes live frames for faster MediaPipe processing.
  - Android sends smaller/lower-quality JPEG frames.

Android app:

- Integrated .NET backend APIs.
- Integrated Python ASL backend APIs.
- Added live sign-to-text camera flow.
- Added sentence paragraph output for detected text.
- Added Text-to-Sign sequence playback.
- Added pause/resume, clear, and speed dropdown for Text-to-Sign.
- Removed admin and upload tabs from user dashboard.
- Added admin dashboard through same login screen.
- Added admin feedback and Text-to-Sign data screens with show more/show less.
- Added live dashboard counts for users, feedback, and translations.
- Added history and feedback persistence.

## Demo Checklist

Before demo on another system:

1. Clone repo.
2. Install PostgreSQL and create/update database connection.
3. Install Python dependencies.
4. Confirm `models/word_signnet_best.pth` exists.
5. Start .NET API on `5140`.
6. Start Python ASL API on `8000`.
7. Update Android `ApiClient.kt` IP address.
8. Build/run Android app.
9. Phone and PC must be on the same Wi-Fi network.

## Notes

- `.gitignore` files are included inside each project folder.
- Generated files such as builds, caches, virtual environments, logs, and local IDE state were excluded from this copied package.
- Runtime source, required config, model checkpoint, and assets are intended to remain trackable for the new repository.
