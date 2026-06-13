package com.example.assistant.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

  @GetMapping("/health")
  public ResponseEntity<HealthResponse> health() {
    return ResponseEntity.ok(new HealthResponse("ok"));
  }

  @PostMapping("/chat")
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    String message = request.message() == null ? "" : request.message().trim();
    String reply = message.isEmpty()
      ? "메시지를 입력해 주세요."
      : "\"" + message + "\"에 대해 조금 더 알려주시면 이어서 도와드릴게요.";

    return ResponseEntity.ok(new ChatResponse(reply));
  }

  public record ChatRequest(String message) {}

  public record ChatResponse(String reply) {}

  public record HealthResponse(String status) {}
}
