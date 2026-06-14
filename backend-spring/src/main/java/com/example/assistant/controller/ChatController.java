package com.example.assistant.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/*
 * 채팅 API를 담당하는 Spring 컨트롤러입니다.
 *
 * 전체 요청 흐름:
 * 1. React가 /api/chat으로 요청합니다.
 * 2. Nginx가 /api 요청을 Spring backend 서비스로 프록시합니다.
 * 3. Spring Security가 Basic Auth 인증을 먼저 검사합니다.
 * 4. 인증에 성공하면 이 컨트롤러가 메시지를 FastAPI /chat으로 전달합니다.
 * 5. FastAPI의 AI 응답을 다시 React가 이해하는 JSON 형태로 반환합니다.
 */
@RestController
public class ChatController {

  /*
   * Spring에서 Python FastAPI 백엔드를 호출하기 위한 HTTP 클라이언트입니다.
   * baseUrl은 application.yml의 app.python-backend.base-url 값으로 설정됩니다.
   */
  private final RestClient pythonBackendClient;

  /*
   * ChatController 생성 시 FastAPI 호출용 RestClient를 준비합니다.
   *
   * Docker Compose에서는 PYTHON_BACKEND_BASE_URL=http://backend-python:8080 으로 들어오며,
   * backend-python은 Docker 네트워크 안에서 FastAPI 컨테이너를 가리키는 서비스 이름입니다.
   */
  public ChatController(
    @Value("${app.python-backend.base-url}") String pythonBackendBaseUrl
  ) {
    /*
     * Spring은 외부 요청의 인증/권한을 확인하는 게이트웨이 역할을 맡습니다.
     * 인증을 통과한 요청만 이 RestClient를 통해 내부 FastAPI 서비스로 전달됩니다.
     */
    this.pythonBackendClient = RestClient
      .builder()
      /*
       * 기본 HTTP 클라이언트가 HTTP/2 업그레이드를 시도하면 Uvicorn이
       * "Unsupported upgrade request"를 남기고 본문을 제대로 받지 못할 수 있습니다.
       * SimpleClientHttpRequestFactory는 단순한 HTTP/1.1 요청을 사용하므로
       * Spring -> FastAPI 내부 호출에 더 예측 가능합니다.
       */
      .requestFactory(new SimpleClientHttpRequestFactory())
      .baseUrl(pythonBackendBaseUrl)
      .build();
  }

  /*
   * Spring 게이트웨이 자체의 상태 확인 API입니다.
   * Docker나 운영 환경에서 Spring 컨테이너가 살아 있는지 확인할 때 사용합니다.
   */
  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("ok"));
  }

  /*
   * React에서 들어온 채팅 메시지를 처리하는 API입니다.
   *
   * 실제 AI 응답 생성은 Python FastAPI가 담당하고,
   * 이 메서드는 인증된 요청만 내부 백엔드로 넘기는 중간 관문 역할을 합니다.
   */
  @PostMapping("/chat")
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    /*
     * SecurityConfig의 Basic Auth 필터가 먼저 실행되므로,
     * 이 메서드에 도달했다는 것은 이미 Spring에서 인증이 끝났다는 뜻입니다.
     */
    String message = request.message() == null ? "" : request.message().trim();

    if (message.isEmpty()) {
      return ResponseEntity.badRequest().body(new ChatResponse("메시지를 입력해 주세요."));
    }

    try {
      /*
       * 브라우저가 보낸 Authorization 헤더를 그대로 넘기지 않습니다.
       * 사용자 인증은 Spring에서 끝내고, FastAPI에는 처리에 필요한 payload만 전달합니다.
       */
      ChatResponse response = pythonBackendClient
        .post()
        .uri("/chat")
        /*
         * FastAPI는 application/json 본문에서 { "message": "..." } 형태를 기대합니다.
         * Content-Type과 Accept를 명시해서 Spring의 HTTP 변환기가 JSON으로 직렬화하도록 고정합니다.
         */
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(new ChatRequest(message))
        .retrieve()
        .body(ChatResponse.class);

      if (response == null || response.reply() == null) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
          .body(new ChatResponse("Python 백엔드에서 응답을 받지 못했습니다."));
      }

      return ResponseEntity.ok(response);
    } catch (RestClientResponseException exception) {
      /*
       * FastAPI가 4xx/5xx를 반환한 경우입니다.
       * 내부 구현 상세나 Python 오류 본문을 그대로 노출하지 않도록 일반 메시지로 감쌉니다.
       * 대신 서버 로그에는 원인 파악이 가능하도록 상태 코드와 응답 본문을 남깁니다.
       */
      System.err.printf(
        "Python backend returned %s: %s%n",
        exception.getStatusCode(),
        exception.getResponseBodyAsString()
      );

      return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ChatResponse("Python 백엔드 처리 중 오류가 발생했습니다."));
    } catch (RestClientException exception) {
      /*
       * 네트워크 연결 실패, 컨테이너 미기동, DNS 문제처럼 FastAPI까지 도달하지 못한 경우입니다.
       */
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ChatResponse("Python 백엔드에 연결할 수 없습니다."));
    }
  }

  /*
   * 컨트롤러에서 처리하지 못한 예외를 마지막으로 받아주는 안전망입니다.
   * 프론트엔드가 예상 가능한 { "reply": "..." } 형태를 계속 받을 수 있게 합니다.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ChatResponse> handleUnexpectedException(Exception exception) {
    /*
     * 예상하지 못한 예외가 발생해도 프론트엔드가 동일한 응답 형태를 받을 수 있게 합니다.
     */
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(new ChatResponse("요청 처리 중 오류가 발생했습니다."));
  }

  /*
   * React와 Spring, Spring과 FastAPI 사이에서 공통으로 사용하는 요청 DTO입니다.
   * 현재는 사용자가 입력한 message 하나만 전달합니다.
   */
  public record ChatRequest(String message) {}

  /*
   * 프론트엔드로 반환하는 응답 DTO입니다.
   * reply에는 챗봇이 사용자에게 보여줄 최종 문장이 들어갑니다.
   */
  public record ChatResponse(String reply) {}

  /*
   * /health 응답 DTO입니다.
   * status가 "ok"이면 Spring 서버가 정상 응답 중이라는 뜻입니다.
   */
  public record HealthResponse(String status) {}
}
