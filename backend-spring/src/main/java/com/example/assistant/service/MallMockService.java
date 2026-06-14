package com.example.assistant.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;

/*
 * 간접재 몰 API에서 사용할 mock 데이터를 제공하는 서비스입니다.
 *
 * 현재 단계에서는 DB에 저장하거나 조회하지 않고, 고정된 샘플 데이터를 반환합니다.
 * 나중에 실제 DB를 붙일 때는 컨트롤러의 API 계약은 유지하고 이 서비스 내부만
 * Repository/JPA 호출로 교체하면 됩니다.
 */
@Service
public class MallMockService {

  /*
   * 간접재 몰의 카테고리 mock 데이터입니다.
   * parentId가 null이면 1depth 카테고리이고, 값이 있으면 해당 상위 카테고리의 하위 항목입니다.
   */
  private final List<Category> categories = List.of(
    new Category("CAT-OFFICE", "사무용품", null, 1),
    new Category("CAT-IT", "IT 소모품", null, 1),
    new Category("CAT-SAFETY", "안전용품", null, 1),
    new Category("CAT-CLEAN", "청소/위생용품", null, 1),
    new Category("CAT-MRO", "설비자재", null, 1),
    new Category("CAT-PACK", "포장자재", null, 1),
    new Category("CAT-OFFICE-PAPER", "복사용지/노트", "CAT-OFFICE", 2),
    new Category("CAT-IT-TONER", "토너/잉크", "CAT-IT", 2)
  );

  /*
   * 공급사 mock 데이터입니다.
   * 등급과 평균 납기일은 상품 상세/추천/구매 검토 화면에서 보여줄 수 있는 값입니다.
   */
  private final List<Supplier> suppliers = List.of(
    new Supplier("SUP-001", "오피스허브", "A", 1, List.of("사무용품", "청소/위생용품")),
    new Supplier("SUP-002", "테크소모품코리아", "A", 2, List.of("IT 소모품")),
    new Supplier("SUP-003", "세이프워크", "B", 3, List.of("안전용품")),
    new Supplier("SUP-004", "팩앤박스", "A", 1, List.of("포장자재")),
    new Supplier("SUP-005", "MRO파트너스", "B", 4, List.of("설비자재"))
  );

  /*
   * 상품 mock 데이터입니다.
   * 간접재 몰에서 자주 다루는 사무용품, IT 소모품, 안전용품, MRO, 포장자재를 섞어 둡니다.
   */
  private final List<Product> products = List.of(
    new Product("PRD-1001", "A4 복사용지 80g", "CAT-OFFICE-PAPER", "복사용지/노트", "SUP-001", "오피스허브", "BOX", "2500매/BOX", 23800, "KRW", 120, true, 1, List.of("재구매", "베스트")),
    new Product("PRD-1002", "검정 볼펜 0.5mm", "CAT-OFFICE", "사무용품", "SUP-001", "오피스허브", "BOX", "12자루/BOX", 7200, "KRW", 310, true, 1, List.of("사무실")),
    new Product("PRD-2001", "HP 호환 토너 CF276A", "CAT-IT-TONER", "토너/잉크", "SUP-002", "테크소모품코리아", "EA", "흑백/대용량", 88000, "KRW", 18, true, 2, List.of("재구매", "비용절감")),
    new Product("PRD-3001", "니트릴 장갑 M", "CAT-SAFETY", "안전용품", "SUP-003", "세이프워크", "BOX", "100매/BOX", 12500, "KRW", 42, true, 3, List.of("안전", "위생")),
    new Product("PRD-4001", "다목적 세정제 3.75L", "CAT-CLEAN", "청소/위생용품", "SUP-001", "오피스허브", "EA", "3.75L", 16400, "KRW", 0, false, 5, List.of("대체추천")),
    new Product("PRD-5001", "스테인리스 육각볼트 M8", "CAT-MRO", "설비자재", "SUP-005", "MRO파트너스", "PACK", "100개/PACK", 19600, "KRW", 65, true, 4, List.of("설비")),
    new Product("PRD-6001", "택배 박스 3호", "CAT-PACK", "포장자재", "SUP-004", "팩앤박스", "BUNDLE", "50매/BUNDLE", 31500, "KRW", 90, true, 1, List.of("포장", "베스트"))
  );

  /*
   * 구매 내역 목록 mock 데이터입니다.
   * 목록 화면에서는 상세 품목보다 구매자, 부서, 상태, 금액 같은 요약 정보가 중요합니다.
   */
  private final List<PurchaseSummary> purchases = List.of(
    new PurchaseSummary("PUR-2026-0001", LocalDate.of(2026, 6, 3), "김유진", "총무팀", "배송완료", 118000, 3, "사무용품"),
    new PurchaseSummary("PUR-2026-0002", LocalDate.of(2026, 6, 7), "박민수", "IT운영팀", "승인대기", 176000, 2, "IT 소모품"),
    new PurchaseSummary("PUR-2026-0003", LocalDate.of(2026, 6, 11), "이서연", "물류팀", "발주완료", 94500, 1, "포장자재")
  );

  /*
   * 신규 상품 소싱 요청 mock 데이터입니다.
   * 몰에 없는 상품을 구매팀/공급사에 등록 요청하는 업무 흐름을 표현합니다.
   */
  private final List<ProductRequest> productRequests = List.of(
    new ProductRequest("REQ-001", LocalDate.of(2026, 6, 1), "김유진", "친환경 종이컵 13oz", "친환경 인증 제품", 3000, "탕비실 소모품 교체", "공급사 확인중", "유사 상품 2건 검토 중", LocalDate.of(2026, 6, 18)),
    new ProductRequest("REQ-002", LocalDate.of(2026, 6, 8), "박민수", "USB-C 멀티허브", "HDMI/PD 충전 포함", 20, "신규 입사자 장비", "등록 완료", "PRD-IT-9001로 등록 완료", LocalDate.of(2026, 6, 12))
  );

  /*
   * 장바구니 mock 데이터입니다.
   * 실제 저장소가 없으므로 추가/수정/삭제 API도 이 원본 리스트를 변경하지 않고 응답만 만들어 반환합니다.
   */
  private final List<CartItem> cartItems = List.of(
    new CartItem("CART-ITEM-001", "PRD-1001", "A4 복사용지 80g", "2500매/BOX", "오피스허브", 2, "BOX", 23800, 47600),
    new CartItem("CART-ITEM-002", "PRD-2001", "HP 호환 토너 CF276A", "흑백/대용량", "테크소모품코리아", 1, "EA", 88000, 88000)
  );

  /*
   * 주문 목록 mock 데이터입니다.
   * 장바구니에서 주문 생성 후 승인/발주/배송 상태로 이어지는 화면을 위한 샘플입니다.
   */
  private final List<OrderSummary> orders = List.of(
    new OrderSummary("ORD-2026-0001", LocalDate.of(2026, 6, 4), "김유진", "총무팀", "배송중", 47600, 1),
    new OrderSummary("ORD-2026-0002", LocalDate.of(2026, 6, 9), "박민수", "IT운영팀", "승인대기", 176000, 2)
  );

  /*
   * 승인 대기 목록 mock 데이터입니다.
   * 구매 승인자 관점에서 주문이나 신규 상품 요청을 한곳에서 볼 수 있게 합니다.
   */
  private final List<ApprovalPending> pendingApprovals = List.of(
    new ApprovalPending("APR-001", "ORD-2026-0002", "박민수", "IT운영팀", 176000, "토너 긴급 구매 요청", LocalDate.of(2026, 6, 9)),
    new ApprovalPending("APR-002", "REQ-001", "김유진", "총무팀", 385000, "신규 친환경 종이컵 소싱 요청", LocalDate.of(2026, 6, 10))
  );

  /*
   * 상품 목록은 키워드/카테고리/공급사/재고 조건으로 가볍게 필터링합니다.
   * 실제 검색 엔진이나 DB 쿼리가 아니라 mock 리스트에 대한 in-memory 필터입니다.
   */
  public List<Product> findProducts(String keyword, String categoryId, String supplierId, Boolean inStock) {
    return products.stream()
      .filter(product -> matchesKeyword(product, keyword))
      .filter(product -> categoryId == null || product.categoryId().equalsIgnoreCase(categoryId))
      .filter(product -> supplierId == null || product.supplierId().equalsIgnoreCase(supplierId))
      .filter(product -> inStock == null || product.inStock() == inStock)
      .toList();
  }

  public Optional<Product> findProduct(String productId) {
    return products.stream()
      .filter(product -> product.id().equalsIgnoreCase(productId))
      .findFirst();
  }

  /*
   * 전체 카테고리 mock 목록을 반환합니다.
   */
  public List<Category> findCategories() {
    return categories;
  }

  /*
   * 전체 공급사 mock 목록을 반환합니다.
   */
  public List<Supplier> findSuppliers() {
    return suppliers;
  }

  /*
   * 구매 목록을 상태/부서/카테고리명으로 필터링합니다.
   * 실제 DB 검색이 아니라 mock 리스트 stream 필터입니다.
   */
  public List<PurchaseSummary> findPurchases(String status, String department, String categoryName) {
    return purchases.stream()
      .filter(purchase -> status == null || purchase.status().equalsIgnoreCase(status))
      .filter(purchase -> department == null || purchase.department().contains(department))
      .filter(purchase -> categoryName == null || purchase.categoryName().contains(categoryName))
      .toList();
  }

  /*
   * 구매 상세를 조회합니다.
   * 목록 mock 데이터에 상세 품목, 승인, 배송 정보를 덧붙여 상세 응답 형태로 만듭니다.
   */
  public Optional<PurchaseDetail> findPurchase(String purchaseId) {
    return purchases.stream()
      .filter(purchase -> purchase.id().equalsIgnoreCase(purchaseId))
      .findFirst()
      .map(purchase -> new PurchaseDetail(
        purchase.id(),
        purchase.purchasedAt(),
        purchase.requesterName(),
        purchase.department(),
        purchase.status(),
        "서울시 강남구 테헤란로 10, 8층 총무팀",
        List.of(
          new PurchaseItem("PRD-1001", "A4 복사용지 80g", "2500매/BOX", 2, "BOX", 23800, 47600, "오피스허브"),
          new PurchaseItem("PRD-1002", "검정 볼펜 0.5mm", "12자루/BOX", 5, "BOX", 7200, 36000, "오피스허브")
        ),
        new ApprovalSnapshot("승인완료", "한지훈", LocalDateTime.of(2026, 6, 3, 14, 20), "월간 소모품 예산 내 구매"),
        new DeliverySnapshot("배송완료", "CJ대한통운", "584912345001", LocalDate.of(2026, 6, 5)),
        purchase.totalAmount()
      ));
  }

  /*
   * 신규 상품 소싱 요청 목록을 조회합니다.
   */
  public List<ProductRequest> findProductRequests(String status) {
    return productRequests.stream()
      .filter(request -> status == null || request.status().equalsIgnoreCase(status))
      .toList();
  }

  /*
   * 신규 상품 소싱 요청 단건을 조회합니다.
   */
  public Optional<ProductRequest> findProductRequest(String requestId) {
    return productRequests.stream()
      .filter(request -> request.id().equalsIgnoreCase(requestId))
      .findFirst();
  }

  public ProductRequestCreated createProductRequest(ProductRequestCreateRequest request) {
    /*
     * 실제 저장은 하지 않고, 요청이 접수되었다는 mock 응답만 반환합니다.
     */
    return new ProductRequestCreated(
      "REQ-MOCK-" + LocalDate.now().toString().replace("-", ""),
      "접수",
      request.productName(),
      "신규 상품 소싱 요청이 접수되었습니다."
    );
  }

  /*
   * 장바구니 현재 상태를 조회합니다.
   */
  public Cart findCart() {
    return buildCart(cartItems);
  }

  /*
   * 장바구니 담기 mock 처리입니다.
   * DB 저장 없이 요청 상품으로 구성된 임시 장바구니 응답을 만들어 반환합니다.
   */
  public CartMutationResponse addCartItem(CartItemCreateRequest request) {
    Product product = findProduct(request.productId())
      .orElse(products.getFirst());
    CartItem addedItem = new CartItem(
      "CART-ITEM-MOCK",
      product.id(),
      product.name(),
      product.spec(),
      product.supplierName(),
      request.quantity(),
      product.unit(),
      product.unitPrice(),
      product.unitPrice() * request.quantity()
    );

    return new CartMutationResponse("장바구니에 상품을 담았습니다.", buildCart(List.of(addedItem)));
  }

  /*
   * 장바구니 수량 변경 mock 처리입니다.
   * 실제 원본 리스트를 바꾸지 않고 변경된 수량이 반영된 응답만 생성합니다.
   */
  public CartMutationResponse updateCartItem(String cartItemId, CartItemUpdateRequest request) {
    CartItem baseItem = cartItems.stream()
      .filter(item -> item.id().equalsIgnoreCase(cartItemId))
      .findFirst()
      .orElse(cartItems.getFirst());
    CartItem updatedItem = new CartItem(
      baseItem.id(),
      baseItem.productId(),
      baseItem.productName(),
      baseItem.spec(),
      baseItem.supplierName(),
      request.quantity(),
      baseItem.unit(),
      baseItem.unitPrice(),
      baseItem.unitPrice() * request.quantity()
    );

    return new CartMutationResponse("장바구니 수량을 변경했습니다.", buildCart(List.of(updatedItem)));
  }

  /*
   * 장바구니 삭제 mock 처리입니다.
   */
  public CartMutationResponse deleteCartItem(String cartItemId) {
    return new CartMutationResponse(
      cartItemId + " 항목을 장바구니에서 삭제했습니다.",
      buildCart(List.of())
    );
  }

  /*
   * 주문 생성 mock 처리입니다.
   * 주문번호, 승인 필요 여부, 총액 샘플 값을 반환하지만 DB에는 저장하지 않습니다.
   */
  public OrderCreated createOrder(OrderCreateRequest request) {
    return new OrderCreated(
      "ORD-MOCK-" + LocalDate.now().toString().replace("-", ""),
      "승인대기",
      135600,
      true,
      "mock 주문이 생성되었습니다. 실제 DB에는 저장되지 않습니다."
    );
  }

  /*
   * 주문 목록 조회 mock 처리입니다.
   */
  public List<OrderSummary> findOrders(String status) {
    return orders.stream()
      .filter(order -> status == null || order.status().equalsIgnoreCase(status))
      .toList();
  }

  /*
   * 주문 상세 조회 mock 처리입니다.
   * 주문 요약에 배송지, 품목, 승인 라인을 붙여 상세 화면용 응답을 구성합니다.
   */
  public Optional<OrderDetail> findOrder(String orderId) {
    return orders.stream()
      .filter(order -> order.id().equalsIgnoreCase(orderId))
      .findFirst()
      .map(order -> new OrderDetail(
        order.id(),
        order.orderedAt(),
        order.requesterName(),
        order.department(),
        order.status(),
        "서울시 강남구 테헤란로 10, 8층",
        cartItems,
        List.of(
          new ApprovalLine("1차 승인", "팀장", "한지훈", "승인완료"),
          new ApprovalLine("2차 승인", "구매팀", "구매담당자", "승인대기")
        ),
        order.totalAmount()
      ));
  }

  /*
   * 상품 추천 mock 처리입니다.
   * 구매 이력, 비용 절감, 부서 사용 패턴처럼 보이는 추천 사유를 함께 반환합니다.
   */
  public List<ProductRecommendation> findProductRecommendations(String userId, String categoryId) {
    return List.of(
      new ProductRecommendation("PRD-1001", "A4 복사용지 80g", "최근 3개월 반복 구매 품목", 23800, 0, "재구매 추천"),
      new ProductRecommendation("PRD-2001", "HP 호환 토너 CF276A", "정품 대비 예상 절감 가능", 88000, 22000, "비용 절감"),
      new ProductRecommendation("PRD-6001", "택배 박스 3호", "물류팀 구매 빈도 증가", 31500, 0, "부서 추천")
    ).stream()
      .filter(recommendation -> categoryId == null || findProduct(recommendation.productId())
        .map(product -> product.categoryId().equalsIgnoreCase(categoryId))
        .orElse(false))
      .toList();
  }

  /*
   * 재구매 추천 mock 처리입니다.
   * 과거 구매 주기나 예상 소진일 기반 추천처럼 화면에 보여줄 데이터를 반환합니다.
   */
  public List<ReorderRecommendation> findReorderRecommendations() {
    return List.of(
      new ReorderRecommendation("PRD-1001", "A4 복사용지 80g", LocalDate.of(2026, 6, 20), 4, "평균 21일 주기로 재구매"),
      new ReorderRecommendation("PRD-2001", "HP 호환 토너 CF276A", LocalDate.of(2026, 6, 24), 2, "프린터 사용량 기준 부족 예상"),
      new ReorderRecommendation("PRD-3001", "니트릴 장갑 M", LocalDate.of(2026, 6, 28), 5, "안전용품 월말 보충 추천")
    );
  }

  /*
   * 승인 대기 목록 mock 처리입니다.
   */
  public List<ApprovalPending> findPendingApprovals() {
    return pendingApprovals;
  }

  /*
   * 승인/반려 결정 mock 처리입니다.
   */
  public ApprovalDecisionResponse decideApproval(String approvalId, ApprovalDecisionRequest request) {
    return new ApprovalDecisionResponse(
      approvalId,
      request.decision(),
      "mock 승인 처리가 완료되었습니다.",
      LocalDateTime.now()
    );
  }

  /*
   * 상품 검색어가 상품명, 규격, 공급사명 중 하나에 포함되는지 확인합니다.
   */
  private boolean matchesKeyword(Product product, String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return true;
    }

    String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
    return product.name().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
      || product.spec().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
      || product.supplierName().toLowerCase(Locale.ROOT).contains(normalizedKeyword);
  }

  /*
   * 장바구니 항목 목록으로 총액과 항목 수를 계산해 Cart 응답 객체를 만듭니다.
   */
  private Cart buildCart(List<CartItem> items) {
    int totalAmount = items.stream()
      .mapToInt(CartItem::amount)
      .sum();

    return new Cart("CART-MOCK-001", "admin", items, items.size(), totalAmount, "KRW");
  }

  /*
   * 카테고리 응답 DTO입니다.
   */
  public record Category(String id, String name, String parentId, int depth) {}

  /*
   * 공급사 응답 DTO입니다.
   */
  public record Supplier(String id, String name, String grade, int leadTimeDays, List<String> categories) {}

  /*
   * 상품 목록/상세 응답 DTO입니다.
   */
  public record Product(
    String id,
    String name,
    String categoryId,
    String categoryName,
    String supplierId,
    String supplierName,
    String unit,
    String spec,
    int unitPrice,
    String currency,
    int stockQuantity,
    boolean inStock,
    int deliveryEtaDays,
    List<String> tags
  ) {}

  /*
   * 구매 목록 응답 DTO입니다.
   */
  public record PurchaseSummary(
    String id,
    LocalDate purchasedAt,
    String requesterName,
    String department,
    String status,
    int totalAmount,
    int itemCount,
    String categoryName
  ) {}

  /*
   * 구매 상세 응답 DTO입니다.
   */
  public record PurchaseDetail(
    String id,
    LocalDate purchasedAt,
    String requesterName,
    String department,
    String status,
    String deliveryAddress,
    List<PurchaseItem> items,
    ApprovalSnapshot approval,
    DeliverySnapshot delivery,
    int totalAmount
  ) {}

  /*
   * 구매 상세에 포함되는 개별 품목 DTO입니다.
   */
  public record PurchaseItem(
    String productId,
    String productName,
    String spec,
    int quantity,
    String unit,
    int unitPrice,
    int amount,
    String supplierName
  ) {}

  /*
   * 구매 승인 상태 DTO입니다.
   */
  public record ApprovalSnapshot(String status, String approverName, LocalDateTime approvedAt, String comment) {}

  /*
   * 구매 배송 상태 DTO입니다.
   */
  public record DeliverySnapshot(String status, String carrier, String trackingNumber, LocalDate deliveredAt) {}

  /*
   * 신규 상품 소싱 요청 응답 DTO입니다.
   */
  public record ProductRequest(
    String id,
    LocalDate requestedAt,
    String requesterName,
    String productName,
    String spec,
    int quantity,
    String purpose,
    String status,
    String reviewComment,
    LocalDate expectedCompletedAt
  ) {}

  /*
   * 신규 상품 소싱 요청 생성 API의 요청 DTO입니다.
   */
  public record ProductRequestCreateRequest(
    String productName,
    String spec,
    int quantity,
    String purpose,
    String requesterName
  ) {}

  /*
   * 신규 상품 소싱 요청 생성 API의 응답 DTO입니다.
   */
  public record ProductRequestCreated(String requestId, String status, String productName, String message) {}

  /*
   * 장바구니 응답 DTO입니다.
   */
  public record Cart(String id, String userId, List<CartItem> items, int itemCount, int totalAmount, String currency) {}

  /*
   * 장바구니 항목 DTO입니다.
   */
  public record CartItem(
    String id,
    String productId,
    String productName,
    String spec,
    String supplierName,
    int quantity,
    String unit,
    int unitPrice,
    int amount
  ) {}

  /*
   * 장바구니 담기 요청 DTO입니다.
   */
  public record CartItemCreateRequest(String productId, int quantity) {}

  /*
   * 장바구니 수량 변경 요청 DTO입니다.
   */
  public record CartItemUpdateRequest(int quantity) {}

  /*
   * 장바구니 변경 API 공통 응답 DTO입니다.
   */
  public record CartMutationResponse(String message, Cart cart) {}

  /*
   * 주문 생성 요청 DTO입니다.
   */
  public record OrderCreateRequest(List<String> cartItemIds, String deliveryAddressId, String requesterNote) {}

  /*
   * 주문 생성 응답 DTO입니다.
   */
  public record OrderCreated(String orderId, String status, int totalAmount, boolean approvalRequired, String message) {}

  /*
   * 주문 목록 응답 DTO입니다.
   */
  public record OrderSummary(
    String id,
    LocalDate orderedAt,
    String requesterName,
    String department,
    String status,
    int totalAmount,
    int itemCount
  ) {}

  /*
   * 주문 상세 응답 DTO입니다.
   */
  public record OrderDetail(
    String id,
    LocalDate orderedAt,
    String requesterName,
    String department,
    String status,
    String deliveryAddress,
    List<CartItem> items,
    List<ApprovalLine> approvalLines,
    int totalAmount
  ) {}

  /*
   * 주문 상세에 포함되는 승인 라인 DTO입니다.
   */
  public record ApprovalLine(String stepName, String roleName, String approverName, String status) {}

  /*
   * 상품 추천 응답 DTO입니다.
   */
  public record ProductRecommendation(
    String productId,
    String productName,
    String reason,
    int unitPrice,
    int expectedSavingAmount,
    String recommendationType
  ) {}

  /*
   * 재구매 추천 응답 DTO입니다.
   */
  public record ReorderRecommendation(
    String productId,
    String productName,
    LocalDate recommendedOrderDate,
    int recommendedQuantity,
    String reason
  ) {}

  /*
   * 승인 대기 목록 응답 DTO입니다.
   */
  public record ApprovalPending(
    String approvalId,
    String targetId,
    String requesterName,
    String department,
    int amount,
    String title,
    LocalDate requestedAt
  ) {}

  /*
   * 승인/반려 처리 요청 DTO입니다.
   */
  public record ApprovalDecisionRequest(String decision, String comment) {}

  /*
   * 승인/반려 처리 응답 DTO입니다.
   */
  public record ApprovalDecisionResponse(String approvalId, String decision, String message, LocalDateTime decidedAt) {}
}
