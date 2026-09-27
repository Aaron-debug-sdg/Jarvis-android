import os
from typing import Any

import urllib.error
import urllib.request
import json

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="JARVIS AI Server", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5.6-luna")
JARVIS_API_KEY = os.getenv("JARVIS_API_KEY", "")

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
    return {"name": "JARVIS AI Server", "status": "online"}


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "openai_configured": bool(OPENAI_API_KEY),
        "model": OPENAI_MODEL,
    }


@app.post("/chat", response_model=ChatResponse)
def chat(
    request: ChatRequest,
    x_jarvis_key: str | None = Header(default=None),
) -> ChatResponse:
    if JARVIS_API_KEY and x_jarvis_key != JARVIS_API_KEY:
        raise HTTPException(status_code=401, detail="Invalid JARVIS API key")

    message = request.message.strip()
    if not message:
        raise HTTPException(status_code=400, detail="message cannot be empty")

    if not OPENAI_API_KEY:
        raise HTTPException(status_code=503, detail="OPENAI_API_KEY is not configured")

    payload = {
        "model": OPENAI_MODEL,
        "instructions": SYSTEM_PROMPT,
        "input": message,
    }

    request_data = json.dumps(payload).encode("utf-8")
    http_request = urllib.request.Request(
        "https://api.openai.com/v1/responses",
        data=request_data,
        headers={
            "Authorization": f"Bearer {OPENAI_API_KEY}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(http_request, timeout=60) as response:
            data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        body = error.read().decode("utf-8", errors="replace")
        raise HTTPException(status_code=502, detail=f"AI provider error: {body[:500]}")
    except urllib.error.URLError as error:
        raise HTTPException(status_code=502, detail=f"AI provider unavailable: {error.reason}")

    reply = data.get("output_text", "").strip()
    if not reply:
        for item in data.get("output", []):
            for content in item.get("content", []):
                if content.get("type") == "output_text":
                    reply += content.get("text", "")
        reply = reply.strip()

    if not reply:
        raise HTTPException(status_code=502, detail="AI provider returned no text")

    return ChatResponse(reply=reply)
