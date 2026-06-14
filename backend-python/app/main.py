from fastapi import Depends, FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.chat_graph import run_chat_graph
from app.schemas import ChatRequest, ChatResponse, HealthResponse
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
    current_settings: Settings = Depends(get_settings),
) -> ChatResponse:
    # 외부 사용자 인증은 Spring 게이트웨이에서 담당합니다.
    # FastAPI는 Docker 내부 네트워크에서 Spring이 넘겨준 메시지를 실제 AI 처리로 연결합니다.
    reply = run_chat_graph(request.message, current_settings)
    return ChatResponse(reply=reply)
