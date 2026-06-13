from pydantic import BaseModel, Field


class HealthResponse(BaseModel):
    status: str = "ok"


class ChatRequest(BaseModel):
    message: str = Field(default="", max_length=4000)


class ChatResponse(BaseModel):
    reply: str
