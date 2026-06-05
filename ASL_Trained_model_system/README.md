# ASL Sign Language Python Backend

This backend provides the AI/sign-language services used by SignAssist:

- Sign-to-text video prediction
- Live camera frame prediction
- Text-to-sign avatar/video assets
- Learning alphabet sign assets

## Folder

`D:\FYP\ASL_Sign_Language_System`

## Requirements

- Python virtual environment in `.venv`
- Dependencies from `requirements.txt`
- Trained model checkpoints in `models/`

Expected checkpoint paths:

```text
models/word_signnet_best.pth
models/signnet_best.pth
```

## Install Dependencies

From this folder:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

If `.venv` does not exist:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install --upgrade pip
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

## Run API Server

```powershell
.\.venv\Scripts\python.exe -m uvicorn api_server:app --host 0.0.0.0 --port 8000
```

Server URL:

```text
http://0.0.0.0:8000
```

## Verify

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

Expected response includes:

```json
{
  "status": "ok",
  "modelReady": true
}
```

## Main APIs

```text
GET  /health
POST /api/sign-to-text
POST /api/sign-to-text/live-frame
POST /api/text-to-sign
GET  /api/learning/alphabet
GET  /api/learning/alphabet/{letter}
```

## Live Prediction Settings

Current fast live prediction settings are in `config.yaml`:

```yaml
inference:
  live_min_word_frames: 10
  live_frame_max_size: 384
```

The Android app sends live frames every `220ms` at reduced size/quality for faster prediction.

## Training

To train the word model:

```powershell
.\.venv\Scripts\python.exe train_words.py
```

The current app uses the checkpoint at:

```text
models/word_signnet_best.pth
```

Model accuracy depends on the trained checkpoint and dataset quality. Runtime optimizations improve speed, but they do not increase checkpoint accuracy.

## Android Connection

The Android app currently points to:

```text
http://192.168.0.33:8000/
```

If your PC IP changes, update `ASL_BASE_URL` in:

```text
SignAssistAp/app/src/main/java/com/example/signassistap/network/ApiClient.kt
```
