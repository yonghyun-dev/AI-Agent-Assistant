import base64
import json
from dataclasses import dataclass
from typing import Any
from urllib import error, parse, request

from app.settings import Settings


@dataclass(frozen=True)
class SpringApiResult:
    ok: bool
    status_code: int
    data: Any


class SpringMallClient:
    """
    Spring에 만들어 둔 간접재 몰 mock API를 Python Agent의 tool처럼 호출하는 wrapper입니다.

    Python Agent는 DB를 직접 보지 않고, 반드시 Spring API를 통해 상품/구매/장바구니/승인 데이터를
    조회합니다. 이렇게 두면 나중에 Spring 쪽 mock API가 실제 DB API로 바뀌어도 Agent 코드는
    wrapper 계약만 유지하면 됩니다.
    """

    def __init__(self, settings: Settings):
        self.base_url = settings.spring_api_base_url.rstrip("/")
        token = f"{settings.app_auth_username}:{settings.app_auth_password}".encode()
        self.authorization = f"Basic {base64.b64encode(token).decode()}"

    def list_products(
        self,
        keyword: str | None = None,
        category_id: str | None = None,
        supplier_id: str | None = None,
        in_stock: bool | None = None,
    ) -> SpringApiResult:
        return self._get(
            "/products",
            {
                "keyword": keyword,
                "categoryId": category_id,
                "supplierId": supplier_id,
                "inStock": in_stock,
            },
        )

    def get_product(self, product_id: str) -> SpringApiResult:
        return self._get(f"/products/{parse.quote(product_id)}")

    def list_purchases(
        self,
        status: str | None = None,
        department: str | None = None,
        category_name: str | None = None,
    ) -> SpringApiResult:
        return self._get(
            "/purchases",
            {
                "status": status,
                "department": department,
                "categoryName": category_name,
            },
        )

    def get_purchase(self, purchase_id: str) -> SpringApiResult:
        return self._get(f"/purchases/{parse.quote(purchase_id)}")

    def list_product_requests(self, status: str | None = None) -> SpringApiResult:
        return self._get("/product-requests", {"status": status})

    def create_product_request(
        self,
        product_name: str,
        spec: str,
        quantity: int,
        purpose: str,
        requester_name: str,
    ) -> SpringApiResult:
        return self._post(
            "/product-requests",
            {
                "productName": product_name,
                "spec": spec,
                "quantity": quantity,
                "purpose": purpose,
                "requesterName": requester_name,
            },
        )

    def get_cart(self) -> SpringApiResult:
        return self._get("/cart")

    def add_cart_item(self, product_id: str, quantity: int) -> SpringApiResult:
        return self._post("/cart/items", {"productId": product_id, "quantity": quantity})

    def list_orders(self, status: str | None = None) -> SpringApiResult:
        return self._get("/orders", {"status": status})

    def get_order(self, order_id: str) -> SpringApiResult:
        return self._get(f"/orders/{parse.quote(order_id)}")

    def create_order(
        self,
        cart_item_ids: list[str],
        delivery_address_id: str,
        requester_note: str | None = None,
    ) -> SpringApiResult:
        return self._post(
            "/orders",
            {
                "cartItemIds": cart_item_ids,
                "deliveryAddressId": delivery_address_id,
                "requesterNote": requester_note,
            },
        )

    def product_recommendations(
        self,
        user_id: str | None = None,
        category_id: str | None = None,
    ) -> SpringApiResult:
        return self._get(
            "/recommendations/products",
            {
                "userId": user_id,
                "categoryId": category_id,
            },
        )

    def reorder_recommendations(self) -> SpringApiResult:
        return self._get("/recommendations/reorder")

    def pending_approvals(self) -> SpringApiResult:
        return self._get("/approvals/pending")

    def decide_approval(
        self,
        approval_id: str,
        decision: str,
        comment: str | None = None,
    ) -> SpringApiResult:
        return self._post(
            f"/approvals/{parse.quote(approval_id)}/decision",
            {
                "decision": decision,
                "comment": comment,
            },
        )

    def _get(self, path: str, params: dict[str, Any] | None = None) -> SpringApiResult:
        query = self._encode_query(params or {})
        url = f"{self.base_url}{path}{query}"
        return self._send("GET", url)

    def _post(self, path: str, body: dict[str, Any]) -> SpringApiResult:
        url = f"{self.base_url}{path}"
        return self._send("POST", url, body)

    def _send(
        self,
        method: str,
        url: str,
        body: dict[str, Any] | None = None,
    ) -> SpringApiResult:
        payload = None
        headers = {
            "Authorization": self.authorization,
            "Accept": "application/json",
        }

        if body is not None:
            payload = json.dumps(body, ensure_ascii=False).encode()
            headers["Content-Type"] = "application/json"

        http_request = request.Request(
            url,
            data=payload,
            headers=headers,
            method=method,
        )

        try:
            with request.urlopen(http_request, timeout=10) as response:
                return SpringApiResult(
                    ok=200 <= response.status < 300,
                    status_code=response.status,
                    data=self._decode(response.read()),
                )
        except error.HTTPError as exception:
            return SpringApiResult(
                ok=False,
                status_code=exception.code,
                data=self._decode(exception.read()),
            )
        except error.URLError as exception:
            return SpringApiResult(
                ok=False,
                status_code=503,
                data={"message": f"Spring API 연결 실패: {exception.reason}"},
            )

    def _encode_query(self, params: dict[str, Any]) -> str:
        clean_params = {
            key: value
            for key, value in params.items()
            if value is not None and value != ""
        }

        if not clean_params:
            return ""

        return "?" + parse.urlencode(clean_params)

    def _decode(self, raw_body: bytes) -> Any:
        if not raw_body:
            return None

        text = raw_body.decode()
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            return text
