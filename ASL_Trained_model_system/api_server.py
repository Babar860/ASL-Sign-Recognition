"""HTTP API for SignAssist integration.

Run with:
    uvicorn api_server:app --host 0.0.0.0 --port 8000
"""

from __future__ import annotations

import shutil
import tempfile
import os
import re
import time
from collections import deque
from pathlib import Path
from threading import Lock
from typing import Any
from urllib.parse import quote

from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from src.config import load_config
from src.word_vocab import display_word_label, normalize_word_label


BASE_DIR = Path(__file__).resolve().parent
os.chdir(BASE_DIR)
CONFIG = load_config(str(BASE_DIR / "config.yaml"))
ASSETS_DIR = BASE_DIR / "assets"
WORD_MODEL_TYPES: tuple[type, ...] = ()

app = FastAPI(title="ASL Sign Language System API", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.mount("/assets", StaticFiles(directory=str(ASSETS_DIR)), name="assets")

_model: Any | None = None
_model_error: str | None = None
_live_sessions: dict[str, dict[str, Any]] = {}
_live_sessions_lock = Lock()
_live_session_ttl_seconds = 120


class TextToSignRequest(BaseModel):
    text: str


@app.on_event("startup")
def _warm_inference_model() -> None:
    try:
        _load_model_once()
    except Exception as exc:
        print(f"ASL model preload skipped: {exc}", flush=True)


def _asset_url(path: Path) -> str:
    relative = path.relative_to(ASSETS_DIR).as_posix()
    return f"/assets/{quote(relative)}"


def _load_model_once() -> Any:
    global _model, _model_error
    if _model is not None:
        return _model
    if _model_error is not None:
        raise RuntimeError(_model_error)
    try:
        word_checkpoint = BASE_DIR / CONFIG["model"]["word_checkpoint_path"]
        letter_checkpoint = BASE_DIR / CONFIG["model"]["checkpoint_path"]
        if not word_checkpoint.exists() and not letter_checkpoint.exists():
            raise FileNotFoundError(
                f"No checkpoint found. Expected {word_checkpoint} or {letter_checkpoint}."
            )

        global WORD_MODEL_TYPES
        from src.keypoint_model import KeypointWordNet, StatisticalKeypointMLP
        from src.recognizer import load_model_for_inference
        from src.word_model import LegacyWordSignNet, WordSignNet

        WORD_MODEL_TYPES = (WordSignNet, LegacyWordSignNet, KeypointWordNet, StatisticalKeypointMLP)
        _model = load_model_for_inference(CONFIG)
        return _model
    except Exception as exc:  # model files are optional until training is done
        _model_error = str(exc)
        raise


def _word_prediction(model: Any, video_path: Path) -> tuple[str, float]:
    import torch
    from src.keypoint_model import KeypointWordNet, MediaPipeKeypointExtractor, StatisticalKeypointMLP, keypoint_motion_features
    from src.preprocessor import Preprocessor

    frame_count = int(CONFIG["model"]["word_frame_count"])
    if isinstance(model, (KeypointWordNet, StatisticalKeypointMLP)):
        extractor = MediaPipeKeypointExtractor(frame_count=frame_count)
        try:
            sequence = extractor.video_keypoints(video_path)
        finally:
            extractor.close()
        features = keypoint_motion_features(sequence)
        tensor = torch.from_numpy(features).unsqueeze(0).to(next(model.parameters()).device)
    else:
        tensor = Preprocessor.prepare_word_video(
            video_path,
            frame_count=frame_count,
            image_size=int(CONFIG["model"]["word_image_size"]),
        ).unsqueeze(0).to(next(model.parameters()).device)

    class_index, confidence, margin = model.predict_with_margin(tensor)
    labels = getattr(model, "word_labels", [])
    label = str(labels[class_index]) if labels else str(class_index)
    min_confidence = float(CONFIG["model"]["word_confidence_threshold"])
    min_margin = float(CONFIG["model"]["word_margin_threshold"])
    if confidence < min_confidence or margin < min_margin:
        return "Low Confidence", confidence
    return display_word_label(label), confidence


def _word_label_from_tensor(model: Any, tensor: Any) -> tuple[str, float]:
    class_index, confidence, margin = model.predict_with_margin(tensor)
    labels = getattr(model, "word_labels", [])
    label = str(labels[class_index]) if labels else str(class_index)
    min_confidence = float(CONFIG["model"]["word_confidence_threshold"])
    min_margin = float(CONFIG["model"]["word_margin_threshold"])
    if confidence < min_confidence or margin < min_margin:
        return "Low Confidence", confidence
    return display_word_label(label), confidence


def _has_hand_keypoints(keypoints: Any) -> bool:
    import numpy as np
    from src.keypoint_model import HAND_LANDMARKS, POSE_LANDMARKS

    values = np.asarray(keypoints, dtype=np.float32)
    pose_end = POSE_LANDMARKS * 4
    hand_values = values[pose_end : pose_end + (HAND_LANDMARKS * 3 * 2)]
    return bool(np.any(np.abs(hand_values) > 1e-6))


def _resize_live_frame(frame: Any) -> Any:
    import cv2

    max_size = int(CONFIG["inference"].get("live_frame_max_size", 384))
    if max_size <= 0:
        return frame
    height, width = frame.shape[:2]
    longest = max(height, width)
    if longest <= max_size:
        return frame
    scale = max_size / float(longest)
    resized_width = max(1, int(width * scale))
    resized_height = max(1, int(height * scale))
    return cv2.resize(frame, (resized_width, resized_height), interpolation=cv2.INTER_AREA)


def _predict_live_frame(model: Any, frame: Any, session_id: str) -> tuple[str, float | None, str, int, int]:
    import torch
    from src.keypoint_model import KeypointWordNet, MediaPipeKeypointExtractor, StatisticalKeypointMLP, keypoint_motion_features
    from src.model import SignNet, index_to_letter
    from src.preprocessor import Preprocessor

    now = time.time()
    frame_count = int(CONFIG["model"]["word_frame_count"])
    live_min_frames = min(
        frame_count,
        max(1, int(CONFIG["inference"].get("live_min_word_frames", frame_count))),
    )
    frame = _resize_live_frame(frame)
    with _live_sessions_lock:
        expired = [
            key
            for key, value in _live_sessions.items()
            if now - float(value.get("updated_at", now)) > _live_session_ttl_seconds
        ]
        for key in expired:
            extractor = _live_sessions[key].get("extractor")
            if extractor is not None:
                extractor.close()
            _live_sessions.pop(key, None)

        session = _live_sessions.setdefault(
            session_id,
            {
                "frames": deque(maxlen=frame_count),
                "keypoints": deque(maxlen=frame_count),
                "extractor": None,
                "lock": Lock(),
                "updated_at": now,
            },
        )
        session["updated_at"] = now

    if isinstance(model, SignNet):
        size = min(int(CONFIG["inference"]["roi_size"]), frame.shape[1], frame.shape[0])
        x1 = (frame.shape[1] - size) // 2
        y1 = (frame.shape[0] - size) // 2
        roi = frame[y1 : y1 + size, x1 : x1 + size]
        tensor = Preprocessor.prepare_frame(roi).to(next(model.parameters()).device)
        class_index, confidence = model.predict(tensor)
        text = index_to_letter(class_index) if confidence >= float(CONFIG["inference"]["confidence_threshold"]) else "Low Confidence"
        return text, confidence, "letter", 1, 1

    if isinstance(model, (KeypointWordNet, StatisticalKeypointMLP)):
        with session["lock"]:
            extractor = session.get("extractor")
            if extractor is None:
                extractor = MediaPipeKeypointExtractor(frame_count=frame_count)
                session["extractor"] = extractor
            keypoints = extractor.frame_keypoints(frame)
            if not _has_hand_keypoints(keypoints):
                session["keypoints"].clear()
                return "Waiting for Hand Gesture", None, "word", 0, live_min_frames
            session["keypoints"].append(keypoints)
            buffered = len(session["keypoints"])
            sequence = list(session["keypoints"])
        if buffered < live_min_frames:
            return "Collecting...", None, "word", buffered, live_min_frames
        features = keypoint_motion_features(__import__("numpy").asarray(sequence, dtype=__import__("numpy").float32))
        tensor = torch.from_numpy(features).unsqueeze(0).to(next(model.parameters()).device)
        text, confidence = _word_label_from_tensor(model, tensor)
        return text, confidence, "word", min(buffered, live_min_frames), live_min_frames

    with _live_sessions_lock:
        session["frames"].append(frame.copy())
        buffered = len(session["frames"])
        frames = list(session["frames"])
    if buffered < live_min_frames:
        return "Collecting...", None, "word", buffered, live_min_frames
    tensor = Preprocessor.prepare_word_frames(
        frames,
        frame_count=frame_count,
        image_size=int(CONFIG["model"]["word_image_size"]),
    ).unsqueeze(0).to(next(model.parameters()).device)
    text, confidence = _word_label_from_tensor(model, tensor)
    return text, confidence, "word", min(buffered, live_min_frames), live_min_frames


def _static_letter_prediction(model: Any, video_path: Path) -> tuple[str, float]:
    import cv2
    from src.model import index_to_letter
    from src.preprocessor import Preprocessor

    capture = cv2.VideoCapture(str(video_path))
    if not capture.isOpened():
        raise ValueError("Could not open uploaded video")
    try:
        frame_count = int(capture.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
        if frame_count > 1:
            capture.set(cv2.CAP_PROP_POS_FRAMES, frame_count // 2)
        ok, frame = capture.read()
    finally:
        capture.release()
    if not ok or frame is None:
        raise ValueError("Uploaded video has no readable frames")

    size = min(int(CONFIG["inference"]["roi_size"]), frame.shape[1], frame.shape[0])
    x1 = (frame.shape[1] - size) // 2
    y1 = (frame.shape[0] - size) // 2
    roi = frame[y1 : y1 + size, x1 : x1 + size]
    tensor = Preprocessor.prepare_frame(roi).to(next(model.parameters()).device)
    class_index, confidence = model.predict(tensor)
    if confidence < float(CONFIG["inference"]["confidence_threshold"]):
        return "Low Confidence", confidence
    return index_to_letter(class_index), confidence


def _find_word_video(text: str) -> Path | None:
    words_dir = BASE_DIR / CONFIG["avatar"]["word_videos_dir"]
    normalized = normalize_word_label(text)
    compact = "".join(ch for ch in normalized if ch.isalnum())
    for path in words_dir.rglob("*.mp4"):
        stem = normalize_word_label(path.stem)
        if stem == normalized or "".join(ch for ch in stem if ch.isalnum()) == compact:
            return path
    return None


def _letters_for_text(text: str, unsupported: list[str]) -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    for char in text.upper():
        if char == " ":
            items.append({"type": "space", "label": "space", "durationMs": int(CONFIG["avatar"]["space_duration_ms"])})
            continue
        if not char.isalpha():
            unsupported.append(char)
            continue
        image_path = _find_letter_image(char)
        if image_path is None:
            unsupported.append(char)
            continue
        items.append(
            {
                "type": "image",
                "label": char,
                "url": _asset_url(image_path),
                "durationMs": int(CONFIG["avatar"]["display_duration_ms"]),
            }
        )
    return items


def _find_letter_image(letter: str) -> Path | None:
    signs_dir = BASE_DIR / CONFIG["avatar"]["signs_dir"]
    for suffix in (".png", ".jpg", ".jpeg"):
        candidate = signs_dir / f"{letter.upper()}{suffix}"
        if candidate.exists():
            return candidate
    return None


@app.get("/health")
def health() -> dict[str, Any]:
    word_checkpoint = BASE_DIR / CONFIG["model"]["word_checkpoint_path"]
    letter_checkpoint = BASE_DIR / CONFIG["model"]["checkpoint_path"]
    return {
        "status": "ok",
        "modelReady": word_checkpoint.exists() or letter_checkpoint.exists(),
        "wordCheckpoint": str(word_checkpoint),
        "letterCheckpoint": str(letter_checkpoint),
    }


@app.post("/api/sign-to-text")
async def sign_to_text(file: UploadFile = File(..., alias="File")) -> JSONResponse:
    suffix = Path(file.filename or "upload.mp4").suffix or ".mp4"
    try:
        model = _load_model_once()
    except Exception as exc:
        raise HTTPException(
            status_code=503,
            detail=f"Sign-to-text model is not ready. Train/copy a checkpoint into models/. Details: {exc}",
        ) from exc

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        temp_path = Path(tmp.name)
        shutil.copyfileobj(file.file, tmp)
    try:
        if isinstance(model, WORD_MODEL_TYPES):
            text, confidence = _word_prediction(model, temp_path)
            model_type = "word"
        else:
            text, confidence = _static_letter_prediction(model, temp_path)
            model_type = "letter"
        return JSONResponse(
            {
                "text": text,
                "predicted_text": text,
                "confidence": confidence,
                "modelType": model_type,
            }
        )
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    finally:
        temp_path.unlink(missing_ok=True)


@app.post("/api/sign-to-text/live-frame")
async def sign_to_text_live_frame(
    frame: UploadFile = File(..., alias="Frame"),
    session_id: str = Form(..., alias="SessionId"),
) -> JSONResponse:
    try:
        model = _load_model_once()
    except Exception as exc:
        raise HTTPException(
            status_code=503,
            detail=f"Sign-to-text model is not ready. Train/copy a checkpoint into models/. Details: {exc}",
        ) from exc

    try:
        import cv2
        import numpy as np

        content = await frame.read()
        image = cv2.imdecode(np.frombuffer(content, dtype=np.uint8), cv2.IMREAD_COLOR)
        if image is None:
            raise ValueError("Could not decode frame image")
        text, confidence, model_type, buffered, required = _predict_live_frame(model, image, session_id.strip())
        return JSONResponse(
            {
                "text": text,
                "predicted_text": text,
                "confidence": confidence,
                "modelType": model_type,
                "sessionId": session_id,
                "bufferedFrames": buffered,
                "requiredFrames": required,
                "isReady": buffered >= required,
            }
        )
    except Exception as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@app.post("/api/text-to-sign")
def text_to_sign(request: TextToSignRequest) -> dict[str, Any]:
    text = request.text[: int(CONFIG["avatar"]["max_input_chars"])].strip()
    if not text:
        raise HTTPException(status_code=400, detail="Text is required")

    word_video = _find_word_video(text)
    if word_video is not None:
        return {
            "inputText": request.text,
            "normalizedText": normalize_word_label(text),
            "mode": "video",
            "videoUrl": _asset_url(word_video),
            "items": [{"type": "video", "label": word_video.stem, "url": _asset_url(word_video)}],
            "unsupportedCharacters": [],
        }

    items: list[dict[str, Any]] = []
    unsupported: list[str] = []
    tokens = re.findall(r"[A-Za-z]+|[^A-Za-z]+", text)
    used_word_video = False
    for token in tokens:
        if token.isspace():
            items.append({"type": "space", "label": "space", "durationMs": int(CONFIG["avatar"]["space_duration_ms"])})
            continue
        if token.isalpha():
            word_video = _find_word_video(token)
            if word_video is not None:
                used_word_video = True
                items.append(
                    {
                        "type": "video",
                        "label": display_word_label(normalize_word_label(token)),
                        "url": _asset_url(word_video),
                        "durationMs": int(CONFIG["avatar"]["max_display_duration_ms"]),
                    }
                )
            else:
                items.extend(_letters_for_text(token, unsupported))
            continue
        items.extend(_letters_for_text(token, unsupported))

    return {
        "inputText": request.text,
        "normalizedText": normalize_word_label(text),
        "mode": "sequence" if used_word_video else "letters",
        "videoUrl": None,
        "items": items,
        "unsupportedCharacters": unsupported,
    }


@app.get("/api/learning/alphabet")
def learning_alphabet() -> list[dict[str, Any]]:
    return [_learning_letter_payload(chr(code)) for code in range(ord("A"), ord("Z") + 1)]


@app.get("/api/learning/alphabet/{letter}")
def learning_alphabet_letter(letter: str) -> dict[str, Any]:
    value = (letter or "").strip().upper()
    if len(value) != 1 or not value.isalpha():
        raise HTTPException(status_code=400, detail="Letter must be A-Z")
    return _learning_letter_payload(value)


def _learning_letter_payload(letter: str) -> dict[str, Any]:
    image_path = _find_letter_image(letter)
    return {
        "letter": letter,
        "label": f"Sign for {letter}",
        "imageUrl": _asset_url(image_path) if image_path is not None else None,
        "available": image_path is not None,
    }
