package com.example.assistant.controller;

import com.example.assistant.service.MallMockService;
import com.example.assistant.service.MallMockService.ApprovalDecisionRequest;
import com.example.assistant.service.MallMockService.ApprovalDecisionResponse;
import com.example.assistant.service.MallMockService.ApprovalPending;
import com.example.assistant.service.MallMockService.Cart;
import com.example.assistant.service.MallMockService.CartItemCreateRequest;
import com.example.assistant.service.MallMockService.CartItemUpdateRequest;
import com.example.assistant.service.MallMockService.CartMutationResponse;
import com.example.assistant.service.MallMockService.Category;
import com.example.assistant.service.MallMockService.OrderCreateRequest;
import com.example.assistant.service.MallMockService.OrderCreated;
import com.example.assistant.service.MallMockService.OrderDetail;
import com.example.assistant.service.MallMockService.OrderSummary;
import com.example.assistant.service.MallMockService.Product;
import com.example.assistant.service.MallMockService.ProductRecommendation;
import com.example.assistant.service.MallMockService.ProductRequest;
import com.example.assistant.service.MallMockService.ProductRequestCreateRequest;
import com.example.assistant.service.MallMockService.ProductRequestCreated;
import com.example.assistant.service.MallMockService.PurchaseDetail;
import com.example.assistant.service.MallMockService.PurchaseSummary;
import com.example.assistant.service.MallMockService.ReorderRecommendation;
import com.example.assistant.service.MallMockService.Supplier;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * 간접재 몰에서 필요한 mock API 컨트롤러입니다.
 *
 * 모든 API는 SecurityConfig의 설정에 따라 Basic Auth 인증을 통과해야 호출됩니다.
 * 여기서는 실제 DB 저장 없이 MallMockService의 고정 데이터를 JSON으로 반환합니다.
 */
@RestController
public class MallController {

  private final MallMockService mallMockService;

  public MallController(MallMockService mallMockService) {
    this.mallMockService = mallMockService;
  }

  /*
   * 간접재 카테고리 트리를 조회합니다.
   * 프론트엔드의 좌측 카테고리 메뉴나 상품 검색 필터에 사용할 수 있습니다.
   */
  @GetMapping("/categories")
  public List<Category> categories() {
    return mallMockService.findCategories();
  }

  /*
   * 공급사 목록을 조회합니다.
   * 공급사 등급, 평균 납기, 취급 카테고리를 함께 내려줍니다.
   */
  @GetMapping("/suppliers")
  public List<Supplier> suppliers() {
    return mallMockService.findSuppliers();
  }

  /*
   * 상품 목록 조회 API입니다.
   * keyword/categoryId/supplierId/inStock 파라미터는 모두 선택 조건입니다.
   */
  @GetMapping("/products")
  public List<Product> products(
    @RequestParam(required = false) String keyword,
    @RequestParam(required = false) String categoryId,
    @RequestParam(required = false) String supplierId,
    @RequestParam(required = false) Boolean inStock
  ) {
    return mallMockService.findProducts(keyword, categoryId, supplierId, inStock);
  }

  /*
   * 상품 상세 조회 API입니다.
   * mock 데이터에 없는 상품 ID면 404를 반환합니다.
   */
  @GetMapping("/products/{productId}")
  public ResponseEntity<Product> product(@PathVariable String productId) {
    return mallMockService.findProduct(productId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /*
   * 구매 내역 목록 조회 API입니다.
   * status/department/categoryName으로 mock 목록을 필터링할 수 있습니다.
   */
  @GetMapping("/purchases")
  public List<PurchaseSummary> purchases(
    @RequestParam(required = false) String status,
    @RequestParam(required = false) String department,
    @RequestParam(required = false) String categoryName
  ) {
    return mallMockService.findPurchases(status, department, categoryName);
  }

  /*
   * 구매 상세 API입니다.
   * 품목, 배송, 승인 정보를 한 번에 확인할 수 있는 형태로 반환합니다.
   */
  @GetMapping("/purchases/{purchaseId}")
  public ResponseEntity<PurchaseDetail> purchase(@PathVariable String purchaseId) {
    return mallMockService.findPurchase(purchaseId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /*
   * 신규 상품 소싱 요청 목록 API입니다.
   * status 파라미터로 접수/공급사 확인중/등록 완료 등의 상태를 필터링합니다.
   */
  @GetMapping("/product-requests")
  public List<ProductRequest> productRequests(@RequestParam(required = false) String status) {
    return mallMockService.findProductRequests(status);
  }

  /*
   * 신규 상품 소싱 요청 상세 API입니다.
   */
  @GetMapping("/product-requests/{requestId}")
  public ResponseEntity<ProductRequest> productRequest(@PathVariable String requestId) {
    return mallMockService.findProductRequest(requestId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /*
   * 신규 상품 소싱 요청 생성 API입니다.
   * 실제 저장 없이 접수 완료 mock 응답을 반환합니다.
   */
  @PostMapping("/product-requests")
  public ProductRequestCreated createProductRequest(@RequestBody ProductRequestCreateRequest request) {
    return mallMockService.createProductRequest(request);
  }

  /*
   * 장바구니 조회 API입니다.
   */
  @GetMapping("/cart")
  public Cart cart() {
    return mallMockService.findCart();
  }

  /*
   * 장바구니 담기 API입니다.
   * 실제 저장 없이 추가된 것처럼 mock 장바구니 응답을 반환합니다.
   */
  @PostMapping("/cart/items")
  public CartMutationResponse addCartItem(@RequestBody CartItemCreateRequest request) {
    return mallMockService.addCartItem(request);
  }

  /*
   * 장바구니 수량 변경 API입니다.
   */
  @PatchMapping("/cart/items/{cartItemId}")
  public CartMutationResponse updateCartItem(
    @PathVariable String cartItemId,
    @RequestBody CartItemUpdateRequest request
  ) {
    return mallMockService.updateCartItem(cartItemId, request);
  }

  /*
   * 장바구니 항목 삭제 API입니다.
   */
  @DeleteMapping("/cart/items/{cartItemId}")
  public CartMutationResponse deleteCartItem(@PathVariable String cartItemId) {
    return mallMockService.deleteCartItem(cartItemId);
  }

  /*
   * 장바구니 기반 주문 생성 API입니다.
   * mock 주문번호와 승인 필요 여부를 반환합니다.
   */
  @PostMapping("/orders")
  public OrderCreated createOrder(@RequestBody OrderCreateRequest request) {
    return mallMockService.createOrder(request);
  }

  /*
   * 주문 목록 조회 API입니다.
   */
  @GetMapping("/orders")
  public List<OrderSummary> orders(@RequestParam(required = false) String status) {
    return mallMockService.findOrders(status);
  }

  /*
   * 주문 상세 조회 API입니다.
   */
  @GetMapping("/orders/{orderId}")
  public ResponseEntity<OrderDetail> order(@PathVariable String orderId) {
    return mallMockService.findOrder(orderId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /*
   * 상품 추천 API입니다.
   * 사용자/카테고리 기반 추천처럼 보이도록 mock 추천 사유를 함께 반환합니다.
   */
  @GetMapping("/recommendations/products")
  public List<ProductRecommendation> productRecommendations(
    @RequestParam(required = false) String userId,
    @RequestParam(required = false) String categoryId
  ) {
    return mallMockService.findProductRecommendations(userId, categoryId);
  }

  /*
   * 과거 구매 주기 기반 재구매 추천 API입니다.
   */
  @GetMapping("/recommendations/reorder")
  public List<ReorderRecommendation> reorderRecommendations() {
    return mallMockService.findReorderRecommendations();
  }

  /*
   * 승인자가 확인해야 할 승인 대기 목록 API입니다.
   */
  @GetMapping("/approvals/pending")
  public List<ApprovalPending> pendingApprovals() {
    return mallMockService.findPendingApprovals();
  }

  /*
   * 승인/반려 처리 API입니다.
   * 실제 상태 변경은 하지 않고 처리 완료 mock 응답만 반환합니다.
   */
  @PostMapping("/approvals/{approvalId}/decision")
  public ApprovalDecisionResponse decideApproval(
    @PathVariable String approvalId,
    @RequestBody ApprovalDecisionRequest request
  ) {
    return mallMockService.decideApproval(approvalId, request);
  }
}
