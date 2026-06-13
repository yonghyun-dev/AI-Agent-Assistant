from typing import TypedDict

from langchain_core.messages import AIMessage, BaseMessage, HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph

from app.settings import Settings


class ChatState(TypedDict):
    messages: list[BaseMessage]
    reply: str


def build_chat_graph(settings: Settings):
    def assistant_node(state: ChatState) -> ChatState:
        model = ChatOpenAI(
            api_key=settings.openai_api_key,
            model=settings.openai_model,
            temperature=0.3,
        )
        response = model.invoke(
            [
                SystemMessage(
                    content="You are a concise, helpful Korean chatbot assistant."
                ),
                *state["messages"],
            ]
        )
        reply = response.content if isinstance(response.content, str) else str(response.content)

        return {
            "messages": [*state["messages"], AIMessage(content=reply)],
            "reply": reply,
        }

    graph = StateGraph(ChatState)
    graph.add_node("assistant", assistant_node)
    graph.add_edge(START, "assistant")
    graph.add_edge("assistant", END)
    return graph.compile()


def run_chat_graph(message: str, settings: Settings) -> str:
    graph = build_chat_graph(settings)
    result = graph.invoke(
        {
            "messages": [HumanMessage(content=message)],
            "reply": "",
        }
    )
    return result["reply"]
