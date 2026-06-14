package com.example.assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Spring Boot 애플리케이션의 시작점입니다.
 *
 * 현재 Spring 서버는 React와 FastAPI 사이에 위치한 API 게이트웨이 역할을 합니다.
 * React/Nginx에서 들어온 요청을 먼저 Spring Security로 인증한 뒤,
 * 인증된 요청만 ChatController를 통해 Python FastAPI 백엔드로 전달합니다.
 */
@SpringBootApplication
public class AssistantApplication {

  /*
   * JVM 프로세스가 시작될 때 호출되는 main 메서드입니다.
   * SpringApplication.run(...)이 내장 Tomcat 서버와 Spring Bean들을 초기화합니다.
   */
  public static void main(String[] args) {
    SpringApplication.run(AssistantApplication.class, args);
  }
}
