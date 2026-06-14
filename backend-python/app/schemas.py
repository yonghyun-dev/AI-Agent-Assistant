from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    status: str = "ok"


class ChatRequest(BaseModel):
    message: str = Field(default="", max_length=4000)
    session_id: str = Field(default="default", max_length=100)


class ChatResponse(BaseModel):
    reply: str
