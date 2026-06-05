# SignLanguage .NET Backend

This backend handles SignAssist users, authentication, password reset, feedback, translation history, learning data, and admin APIs.

## Folder

`D:\FYP\API\SignLanguage`

## Requirements

- .NET SDK 8 or newer
- PostgreSQL running locally
- Database configured in `appsettings.json`

Current default connection string:

```json
"Host=localhost;Port=5432;Database=SIGN_LANGUAGE_DATABASE;Username=postgres;Password=12345"
```

Update `appsettings.json` if your PostgreSQL username, password, host, or database name is different.

## Run

From this folder:

```powershell
dotnet restore
dotnet run --urls http://0.0.0.0:5140
```

The app also adds this URL in `Program.cs`:

```text
http://0.0.0.0:5140
```

## Verify

Open:

```text
http://localhost:5140/swagger
```

Useful checks:

```powershell
Invoke-RestMethod http://127.0.0.1:5140/api/Auth/users
Invoke-RestMethod http://127.0.0.1:5140/api/Feedback/admin
Invoke-RestMethod "http://127.0.0.1:5140/api/TranslationHistory/admin?type=Text-to-Sign"
```

## Android Connection

The Android app currently points to:

```text
http://192.168.0.33:5140/
```

If your PC IP changes, update `BASE_URL` in:

```text
SignAssistAp/app/src/main/java/com/example/signassistap/network/ApiClient.kt
```

## Notes

- Database migrations are applied automatically on startup.
- `translation_history` and `user_feedback` tables are also created if missing.
- Admin login is handled in the Android app with:
  - Email: `admin@gmail.com`
  - Password: `admin123`
