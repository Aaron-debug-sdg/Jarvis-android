import json
import os
import urllib.error
import urllib.request
from typing import Any

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="JARVIS AI Server", version="0.3.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["POST", "GET"],
    allow_headers=["Content-Type", "Accept", "X-Jarvis-Key"],
)

ANTHROPIC_API_KEY = os.getenv("ANTHROPIC_API_KEY", "").strip()
ANTHROPIC_MODEL = os.getenv("ANTHROPIC_MODEL", "claude-opus-5-5").strip()
JARVIS_API_KEY = os.getenv("JARVIS_API_KEY", "").strip()

SYSTEM_PROMPT = os.getenv(
    "JARVIS_SYSTEM_PROMPT",
    "Eres JARVIS, un asistente de IA personal. Responde en español salvo que el usuario pida otro idioma. "
    "Sé claro, útil, natural y conciso. No inventes acciones que no hayas realizado.",
)


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    reply: str


@app.get("/")
def root() -> dict[str, str]:
    return {"name": "JARVIS AI Server", "status": "online", "provider": "anthropic"}


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "anthropic_configured": bool(ANTHROPIC_API_KEY),
        "jarvis_key_configured": bool(JARVIS_API_KEY),
        "model": ANTHROPIC_MODEL,
    }


@app.post("/chat", response_model=ChatResponse)
def chat(
    request: ChatRequest,
    x_jarvis_key: str | None = Header(default=None),
) -> ChatResponse:
    if not JARVIS_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="JARVIS_API_KEY is not configured. Protect the public endpoint before using it.",
        )

    if x_jarvis_key != JARVIS_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid JARVIS API key")

    message = request.message.strip()
    if not message:
        raise HTTPException(status_code=400, detail="message cannot be empty")

    if not ANTHROPIC_API_KEY:
        raise HTTPException(status_code=503, detail="ANTHROPIC_API_KEY is not configured")

    payload = {
        "model": ANTHROPIC_MODEL,
        "max_tokens": 2048,
        "system": SYSTEM_PROMPT,
        "messages": [{"role": "user", "content": message}],
    }

    request_data = json.dumps(payload).encode("utf-8")
    http_request = urllib.request.Request(
        "https://api.anthropic.com/v1/messages",
        data=request_data,
        headers={
            "x-api-key": ANTHROPIC_API_KEY,
            "anthropic-version": "2023-06-01",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(http_request, timeout=60) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise HTTPException(
            status_code=502,
            detail=f"Claude API error: {body[:500]}",
        ) from error
    except urllib.error.URLError as error:
        raise HTTPException(
            status_code=502,
            detail=f"Claude API unavailable: {error.reason}",
        ) from error
    except TimeoutError as error:
        raise HTTPException(
            status_code=504,
            detail="Claude API request timed out",
        ) from error

    reply_parts = []
    for block in data.get("content", []):
        if block.get("type") == "text":
            reply_parts.append(block.get("text", ""))

    reply = "".join(reply_parts).strip()
    if not reply:
        raise HTTPException(status_code=502, detail="Claude API returned no text")

    return ChatResponse(reply=reply)
