import json
import re
from typing import Any, TypedDict

from langchain_core.messages import HumanMessage, SystemMessage
from langchain_core.tools import BaseTool, tool
from langchain_openai import ChatOpenAI
from langgraph.graph import END, START, StateGraph

from app.settings import Settings
from app.spring_mall_client import SpringApiResult, SpringMallClient


class ChatState(TypedDict, total=False):
    message: str
    session_id: str
    intent: str
    slots: dict[str, Any]
    missing_slots: list[str]
    confirmed: bool
    tool_result: SpringApiResult
    reply: str


PendingState = dict[str, Any]


"""
간단한 human-in-the-loop 상태 저장소입니다.

현재 프론트엔드 요청은 session_id를 보내지 않아도 동작하도록 default session을 사용합니다.
운영 환경에서는 Python 프로세스 메모리 대신 Redis, DB, LangGraph checkpoint 저장소로 옮기는 것이 안전합니다.
"""
PENDING_SESSIONS: dict[str, PendingState] = {}


TOOL_SPECS: dict[str, dict[str, Any]] = {
    "product_search": {
        "description": "상품 목록 조회",
        "required": [],
        "confirm": False,
    },
    "product_detail": {
        "description": "상품 상세 조회",
        "required": ["productId"],
        "confirm": False,
    },
    "purchase_search": {
        "description": "구매 내역 조회",
        "required": [],
        "confirm": False,
    },
    "purchase_detail": {
        "description": "구매 상세 조회",
        "required": ["purchaseId"],
        "confirm": False,
    },
    "product_request_search": {
        "description": "신규 상품 소싱 요청 조회",
        "required": [],
        "confirm": False,
    },
    "product_request_create": {
        "description": "신규 상품 소싱 요청 생성",
        "required": ["productName", "spec", "quantity", "purpose", "requesterName"],
        "confirm": True,
    },
    "cart_view": {
        "description": "장바구니 조회",
        "required": [],
        "confirm": False,
    },
    "cart_add": {
        "description": "장바구니 담기",
        "required": ["productId", "quantity"],
        "confirm": True,
    },
    "order_search": {
        "description": "주문 목록 조회",
        "required": [],
        "confirm": False,
    },
    "order_detail": {
        "description": "주문 상세 조회",
        "required": ["orderId"],
        "confirm": False,
    },
    "order_create": {
        "description": "주문 생성",
        "required": ["cartItemIds", "deliveryAddressId"],
        "confirm": True,
    },
    "product_recommendation": {
        "description": "상품 추천",
        "required": [],
        "confirm": False,
    },
    "reorder_recommendation": {
        "description": "재구매 추천",
        "required": [],
        "confirm": False,
    },
    "approval_pending": {
        "description": "승인 대기 목록 조회",
        "required": [],
        "confirm": False,
    },
    "approval_decision": {
        "description": "승인/반려 처리",
        "required": ["approvalId", "decision"],
        "confirm": True,
    },
    "smalltalk": {
        "description": "일반 대화",
        "required": [],
        "confirm": False,
    },
}


SLOT_LABELS = {
    "productId": "상품 ID",
    "purchaseId": "구매 ID",
    "productName": "상품명",
    "spec": "규격",
    "quantity": "수량",
    "purpose": "사용 목적",
    "requesterName": "요청자명",
    "orderId": "주문 ID",
    "cartItemIds": "장바구니 항목 ID",
    "deliveryAddressId": "배송지 ID",
    "approvalId": "승인 ID",
    "decision": "승인/반려 결정",
}


def build_chat_graph(settings: Settings):
    model = ChatOpenAI(
        api_key=settings.openai_api_key,
        model=settings.openai_model,
        temperature=0.2,
    )
    spring_client = SpringMallClient(settings)
    tool_registry = build_tool_registry(spring_client)

    def understand_request(state: ChatState) -> ChatState:
        """
        사용자 메시지를 Agent가 실행할 intent와 slot으로 변환합니다.

        이전 턴에서 누락 slot이나 사용자 확인을 기다리던 작업이 있으면 그 상태를 먼저 반영합니다.
        """
        message = state["message"]
        session_id = state["session_id"]
        pending = PENDING_SESSIONS.get(session_id)

        if pending and pending.get("awaiting") == "confirmation":
            if is_confirm_message(message):
                return {
                    **state,
                    "intent": pending["intent"],
                    "slots": pending["slots"],
                    "confirmed": True,
                }

            if is_cancel_message(message):
                PENDING_SESSIONS.pop(session_id, None)
                return {
                    **state,
                    "reply": "요청을 취소했습니다. 필요한 작업이 있으면 다시 말씀해 주세요.",
                }

            return {
                **state,
                "reply": "이 작업은 실행 전 확인이 필요합니다. 진행하려면 '네', 취소하려면 '취소'라고 답해주세요.",
            }

        parsed = parse_user_request(model, message, pending)
        intent = parsed.get("intent", "smalltalk")
        slots = normalize_slots(parsed.get("slots", {}))

        if pending and pending.get("awaiting") == "slots":
            intent = pending["intent"]
            slots = {**pending.get("slots", {}), **slots}

        return {
            **state,
            "intent": intent if intent in TOOL_SPECS else "smalltalk",
            "slots": slots,
            "confirmed": False,
        }

    def inspect_slots(state: ChatState) -> ChatState:
        """
        intent 실행에 필요한 필수 slot이 모두 채워졌는지 검사합니다.
        """
        intent = state["intent"]
        required_slots = TOOL_SPECS[intent]["required"]
        missing_slots = [
            slot_name
            for slot_name in required_slots
            if is_empty_slot(state.get("slots", {}).get(slot_name))
        ]

        return {**state, "missing_slots": missing_slots}

    def ask_for_missing_slots(state: ChatState) -> ChatState:
        """
        필요한 slot이 부족하면 사용자에게 다시 물어보고 현재 작업을 pending 상태로 저장합니다.
        """
        intent = state["intent"]
        slots = state.get("slots", {})
        missing_slots = state.get("missing_slots", [])
        PENDING_SESSIONS[state["session_id"]] = {
            "awaiting": "slots",
            "intent": intent,
            "slots": slots,
            "missingSlots": missing_slots,
        }

        missing_labels = ", ".join(SLOT_LABELS.get(slot, slot) for slot in missing_slots)
        return {
            **state,
            "reply": f"{TOOL_SPECS[intent]['description']} 작업을 처리하려면 {missing_labels} 값이 필요합니다. 알려주시면 이어서 진행할게요.",
        }

    def ask_for_confirmation(state: ChatState) -> ChatState:
        """
        데이터 변경성 작업은 바로 실행하지 않고 사용자에게 최종 확인을 받습니다.
        """
        intent = state["intent"]
        slots = state.get("slots", {})
        PENDING_SESSIONS[state["session_id"]] = {
            "awaiting": "confirmation",
            "intent": intent,
            "slots": slots,
        }

        return {
            **state,
            "reply": build_confirmation_message(intent, slots),
        }

    def execute_tool(state: ChatState) -> ChatState:
        """
        Spring API wrapper를 실제 tool처럼 실행합니다.
        """
        PENDING_SESSIONS.pop(state["session_id"], None)
        selected_tool = tool_registry[state["intent"]]
        result = invoke_langchain_tool(selected_tool, state.get("slots", {}))
        return {**state, "tool_result": result}

    def generate_tool_reply(state: ChatState) -> ChatState:
        """
        Spring API의 JSON 결과를 사용자에게 읽기 쉬운 한국어 응답으로 요약합니다.
        """
        result = state["tool_result"]
        if not result.ok:
            return {
                **state,
                "reply": f"Spring API 호출 중 오류가 발생했습니다. 상태 코드: {result.status_code}, 응답: {result.data}",
            }

        response = model.invoke(
            [
                SystemMessage(
                    content=(
                        "너는 간접재 구매몰 업무를 돕는 한국어 Agent다. "
                        "아래 tool 실행 결과 JSON을 사용자가 바로 이해할 수 있게 간결하게 요약해라. "
                        "목록은 중요한 항목 3~5개만 보여주고, ID는 후속 요청에 필요하므로 보존해라."
                    )
                ),
                HumanMessage(
                    content=json.dumps(
                        {
                            "intent": state["intent"],
                            "slots": state.get("slots", {}),
                            "toolResult": result.data,
                        },
                        ensure_ascii=False,
                    )
                ),
            ]
        )

        return {**state, "reply": message_to_text(response.content)}

    def general_response(state: ChatState) -> ChatState:
        """
        간접재 몰 API 호출이 필요 없는 일반 대화나 모호한 요청을 처리합니다.
        """
        response = model.invoke(
            [
                SystemMessage(
                    content=(
                        "너는 간접재 구매몰 Agent다. "
                        "가능한 업무는 상품 조회, 구매 조회, 신규 상품 소싱 요청, 장바구니, 주문, 추천, 승인 처리다. "
                        "요청이 모호하면 가능한 API 예시를 짧게 안내해라."
                    )
                ),
                HumanMessage(content=state["message"]),
            ]
        )
        return {**state, "reply": message_to_text(response.content)}

    def route_after_understanding(state: ChatState) -> str:
        if state.get("reply"):
            return END
        if state.get("intent") == "smalltalk":
            return "general_response"
        return "inspect_slots"

    def route_after_slot_inspection(state: ChatState) -> str:
        if state.get("missing_slots"):
            return "ask_for_missing_slots"

        intent = state["intent"]
        if TOOL_SPECS[intent]["confirm"] and not state.get("confirmed"):
            return "ask_for_confirmation"

        return "execute_tool"

    graph = StateGraph(ChatState)
    graph.add_node("understand_request", understand_request)
    graph.add_node("inspect_slots", inspect_slots)
    graph.add_node("ask_for_missing_slots", ask_for_missing_slots)
    graph.add_node("ask_for_confirmation", ask_for_confirmation)
    graph.add_node("execute_tool", execute_tool)
    graph.add_node("generate_tool_reply", generate_tool_reply)
    graph.add_node("general_response", general_response)

    graph.add_edge(START, "understand_request")
    graph.add_conditional_edges("understand_request", route_after_understanding)
    graph.add_conditional_edges("inspect_slots", route_after_slot_inspection)
    graph.add_edge("ask_for_missing_slots", END)
    graph.add_edge("ask_for_confirmation", END)
    graph.add_edge("execute_tool", "generate_tool_reply")
    graph.add_edge("generate_tool_reply", END)
    graph.add_edge("general_response", END)

    return graph.compile()


def run_chat_graph(message: str, settings: Settings, session_id: str = "default") -> str:
    graph = build_chat_graph(settings)
    result = graph.invoke(
        {
            "message": message,
            "session_id": session_id or "default",
            "reply": "",
        }
    )
    return result["reply"]


def build_tool_registry(
    spring_client: SpringMallClient,
) -> dict[str, BaseTool]:
    """
    intent 이름을 Spring API wrapper 함수에 연결합니다.

    각 함수는 @tool로 감싸진 LangChain Tool입니다.
    Tool 내부에서는 SpringMallClient를 사용하므로, 실제 데이터 조회/처리는 Spring mock API를 통해 일어납니다.
    """
    @tool("product_search")
    def product_search(
        keyword: str | None = None,
        categoryId: str | None = None,
        supplierId: str | None = None,
        inStock: bool | None = None,
    ) -> SpringApiResult:
        """상품 목록을 Spring /products API에서 조회한다."""
        return spring_client.list_products(
            keyword=keyword,
            category_id=categoryId,
            supplier_id=supplierId,
            in_stock=inStock,
        )

    @tool("product_detail")
    def product_detail(productId: str) -> SpringApiResult:
        """상품 ID로 Spring /products/{productId} API를 호출한다."""
        return spring_client.get_product(productId)

    @tool("purchase_search")
    def purchase_search(
        status: str | None = None,
        department: str | None = None,
        categoryName: str | None = None,
    ) -> SpringApiResult:
        """구매 내역을 Spring /purchases API에서 조회한다."""
        return spring_client.list_purchases(
            status=status,
            department=department,
            category_name=categoryName,
        )

    @tool("purchase_detail")
    def purchase_detail(purchaseId: str) -> SpringApiResult:
        """구매 ID로 Spring /purchases/{purchaseId} API를 호출한다."""
        return spring_client.get_purchase(purchaseId)

    @tool("product_request_search")
    def product_request_search(status: str | None = None) -> SpringApiResult:
        """신규 상품 소싱 요청 목록을 Spring /product-requests API에서 조회한다."""
        return spring_client.list_product_requests(status=status)

    @tool("product_request_create")
    def product_request_create(
        productName: str,
        spec: str,
        quantity: int,
        purpose: str,
        requesterName: str,
    ) -> SpringApiResult:
        """신규 상품 소싱 요청을 Spring /product-requests API로 생성한다."""
        return spring_client.create_product_request(
            product_name=productName,
            spec=spec,
            quantity=quantity,
            purpose=purpose,
            requester_name=requesterName,
        )

    @tool("cart_view")
    def cart_view() -> SpringApiResult:
        """장바구니를 Spring /cart API에서 조회한다."""
        return spring_client.get_cart()

    @tool("cart_add")
    def cart_add(productId: str, quantity: int) -> SpringApiResult:
        """상품을 Spring /cart/items API를 통해 장바구니에 담는다."""
        return spring_client.add_cart_item(product_id=productId, quantity=quantity)

    @tool("order_search")
    def order_search(status: str | None = None) -> SpringApiResult:
        """주문 목록을 Spring /orders API에서 조회한다."""
        return spring_client.list_orders(status=status)

    @tool("order_detail")
    def order_detail(orderId: str) -> SpringApiResult:
        """주문 ID로 Spring /orders/{orderId} API를 호출한다."""
        return spring_client.get_order(orderId)

    @tool("order_create")
    def order_create(
        cartItemIds: list[str],
        deliveryAddressId: str,
        requesterNote: str | None = None,
    ) -> SpringApiResult:
        """장바구니 항목으로 Spring /orders API를 호출해 주문을 생성한다."""
        return spring_client.create_order(
            cart_item_ids=ensure_string_list(cartItemIds),
            delivery_address_id=deliveryAddressId,
            requester_note=requesterNote,
        )

    @tool("product_recommendation")
    def product_recommendation(
        userId: str | None = None,
        categoryId: str | None = None,
    ) -> SpringApiResult:
        """Spring /recommendations/products API에서 상품 추천을 조회한다."""
        return spring_client.product_recommendations(
            user_id=userId,
            category_id=categoryId,
        )

    @tool("reorder_recommendation")
    def reorder_recommendation() -> SpringApiResult:
        """Spring /recommendations/reorder API에서 재구매 추천을 조회한다."""
        return spring_client.reorder_recommendations()

    @tool("approval_pending")
    def approval_pending() -> SpringApiResult:
        """Spring /approvals/pending API에서 승인 대기 목록을 조회한다."""
        return spring_client.pending_approvals()

    @tool("approval_decision")
    def approval_decision(
        approvalId: str,
        decision: str,
        comment: str | None = None,
    ) -> SpringApiResult:
        """Spring /approvals/{approvalId}/decision API로 승인/반려 결정을 처리한다."""
        return spring_client.decide_approval(
            approval_id=approvalId,
            decision=decision,
            comment=comment,
        )

    return {
        "product_search": product_search,
        "product_detail": product_detail,
        "purchase_search": purchase_search,
        "purchase_detail": purchase_detail,
        "product_request_search": product_request_search,
        "product_request_create": product_request_create,
        "cart_view": cart_view,
        "cart_add": cart_add,
        "order_search": order_search,
        "order_detail": order_detail,
        "order_create": order_create,
        "product_recommendation": product_recommendation,
        "reorder_recommendation": reorder_recommendation,
        "approval_pending": approval_pending,
        "approval_decision": approval_decision,
    }


def invoke_langchain_tool(selected_tool: BaseTool, slots: dict[str, Any]) -> SpringApiResult:
    """
    LangChain Tool에 필요한 인자만 골라 invoke합니다.

    LLM이 추출한 slots에는 tool이 쓰지 않는 값이 섞일 수 있으므로,
    tool schema에 등록된 인자만 넘겨서 Pydantic validation 오류를 줄입니다.
    """
    allowed_args = set(selected_tool.args.keys())
    tool_input = {
        key: value
        for key, value in slots.items()
        if key in allowed_args
    }

    result = selected_tool.invoke(tool_input)
    if isinstance(result, SpringApiResult):
        return result

    return SpringApiResult(ok=True, status_code=200, data=result)


def parse_user_request(
    model: ChatOpenAI,
    message: str,
    pending: PendingState | None,
) -> dict[str, Any]:
    """
    LLM을 사용해 사용자 발화를 intent와 slot JSON으로 변환합니다.
    JSON 파싱에 실패하면 간단한 규칙 기반 fallback으로 최소한의 intent를 추정합니다.
    """
    pending_context = pending or {}
    response = model.invoke(
        [
            SystemMessage(
                content=(
                    "너는 간접재 구매몰 Agent의 intent/slot parser다. "
                    "반드시 JSON만 반환한다. 설명 문장은 쓰지 마라. "
                    "가능한 intent: "
                    + ", ".join(TOOL_SPECS.keys())
                    + ". "
                    "slot 이름은 다음 camelCase만 사용한다: "
                    "keyword, categoryId, supplierId, inStock, productId, purchaseId, status, department, "
                    "categoryName, productName, spec, quantity, purpose, requesterName, orderId, cartItemIds, "
                    "deliveryAddressId, requesterNote, userId, approvalId, decision, comment. "
                    "ID가 없고 상품명만 있으면 product_search로 분류하고 keyword에 상품명을 넣어라. "
                    "신규 상품 요청/소싱은 product_request_create로 분류한다. "
                    "장바구니에 담기는 cart_add, 주문 생성은 order_create, 승인/반려는 approval_decision이다. "
                    "응답 형식: {\"intent\":\"product_search\",\"slots\":{\"keyword\":\"복사용지\"}}"
                )
            ),
            HumanMessage(
                content=json.dumps(
                    {
                        "message": message,
                        "pending": pending_context,
                    },
                    ensure_ascii=False,
                )
            ),
        ]
    )

    try:
        return json.loads(extract_json_object(message_to_text(response.content)))
    except (json.JSONDecodeError, ValueError):
        return fallback_parse(message)


def fallback_parse(message: str) -> dict[str, Any]:
    """
    LLM JSON 파싱이 실패했을 때 사용하는 보수적인 fallback입니다.
    """
    lowered = message.lower()
    slots: dict[str, Any] = {}

    product_id = find_identifier(message, "PRD")
    purchase_id = find_identifier(message, "PUR")
    order_id = find_identifier(message, "ORD")
    approval_id = find_identifier(message, "APR")

    if product_id:
        return {"intent": "product_detail", "slots": {"productId": product_id}}
    if purchase_id:
        return {"intent": "purchase_detail", "slots": {"purchaseId": purchase_id}}
    if order_id:
        return {"intent": "order_detail", "slots": {"orderId": order_id}}
    if approval_id:
        slots["approvalId"] = approval_id

    if "장바구니" in message and ("담" in message or "추가" in message):
        if product_id:
            slots["productId"] = product_id
        quantity = find_quantity(message)
        if quantity:
            slots["quantity"] = quantity
        return {"intent": "cart_add", "slots": slots}

    if "장바구니" in message:
        return {"intent": "cart_view", "slots": {}}
    if "추천" in message and "재구매" in message:
        return {"intent": "reorder_recommendation", "slots": {}}
    if "추천" in message:
        return {"intent": "product_recommendation", "slots": {}}
    if "구매" in message:
        return {"intent": "purchase_search", "slots": {}}
    if "승인" in message or "반려" in message:
        if "반려" in message:
            slots["decision"] = "반려"
        elif "승인" in message:
            slots["decision"] = "승인"
        return {"intent": "approval_decision" if slots else "approval_pending", "slots": slots}
    if "상품" in message or "토너" in message or "복사용지" in message:
        return {"intent": "product_search", "slots": {"keyword": message.strip()}}
    if "order" in lowered or "주문" in message:
        return {"intent": "order_search", "slots": {}}

    return {"intent": "smalltalk", "slots": {}}


def normalize_slots(slots: dict[str, Any]) -> dict[str, Any]:
    """
    비어 있는 값은 제거하고 수량처럼 숫자로 쓸 값은 가능한 int로 변환합니다.
    """
    normalized = {
        key: value
        for key, value in slots.items()
        if not is_empty_slot(value)
    }

    if "quantity" in normalized:
        try:
            normalized["quantity"] = int(normalized["quantity"])
        except (TypeError, ValueError):
            pass

    if "cartItemIds" in normalized:
        normalized["cartItemIds"] = ensure_string_list(normalized["cartItemIds"])

    return normalized


def build_confirmation_message(intent: str, slots: dict[str, Any]) -> str:
    """
    human-in-the-loop 확인 메시지를 생성합니다.
    """
    slot_summary = ", ".join(
        f"{SLOT_LABELS.get(key, key)}={value}"
        for key, value in slots.items()
    )
    return (
        f"{TOOL_SPECS[intent]['description']} 작업을 진행할까요?\n"
        f"확인 값: {slot_summary}\n"
        "진행하려면 '네', 취소하려면 '취소'라고 답해주세요."
    )


def is_confirm_message(message: str) -> bool:
    return message.strip().lower() in {"네", "예", "응", "ㅇㅇ", "확인", "진행", "yes", "y"}


def is_cancel_message(message: str) -> bool:
    return message.strip().lower() in {"아니오", "아니", "ㄴㄴ", "취소", "중단", "no", "n"}


def is_empty_slot(value: Any) -> bool:
    return value is None or value == "" or value == [] or value == {}


def ensure_string_list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item) for item in value if str(item)]
    return [part.strip() for part in str(value).split(",") if part.strip()]


def message_to_text(content: Any) -> str:
    if isinstance(content, str):
        return content
    return str(content)


def extract_json_object(text: str) -> str:
    """
    모델이 실수로 ```json code fence를 붙여도 JSON 객체만 추출합니다.
    """
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = re.sub(r"^```(?:json)?", "", cleaned).strip()
        cleaned = re.sub(r"```$", "", cleaned).strip()

    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start == -1 or end == -1:
        raise ValueError("JSON object not found")
    return cleaned[start : end + 1]


def find_identifier(message: str, prefix: str) -> str | None:
    match = re.search(rf"{prefix}-[A-Z0-9-]+", message, re.IGNORECASE)
    return match.group(0).upper() if match else None


def find_quantity(message: str) -> int | None:
    match = re.search(r"(\d+)\s*(개|박스|box|ea|팩|묶음)?", message, re.IGNORECASE)
    return int(match.group(1)) if match else None
