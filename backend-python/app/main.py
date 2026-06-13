from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.chat_graph import run_chat_graph
from app.schemas import ChatRequest, ChatResponse, HealthResponse
from app.security import require_basic_auth
from app.settings import Settings, get_settings


settings = get_settings()

app = FastAPI(title="AI Agent Assistant Python Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.allowed_origins,
    allow_credentials=True,
    allow_methods=["GET", "POST", "OPTIONS"],
    allow_headers=["Authorization", "Content-Type"],
)


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse()


@app.post("/chat", response_model=ChatResponse)
def chat(
    request: ChatRequest,
    _: str = Depends(require_basic_auth),
    current_settings: Settings = Depends(get_settings),
) -> ChatResponse:
    reply = run_chat_graph(request.message, current_settings)
    return ChatResponse(reply=reply)
