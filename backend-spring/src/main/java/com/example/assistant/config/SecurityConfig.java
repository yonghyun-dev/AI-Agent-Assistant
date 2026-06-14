package com.example.assistant.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/*
 * Spring 게이트웨이의 보안 설정입니다.
 *
 * 이 프로젝트에서는 브라우저가 FastAPI를 직접 호출하지 않고 Spring을 먼저 호출합니다.
 * 따라서 사용자 인증/권한 확인은 이 SecurityConfig에서 처리하고,
 * 인증을 통과한 요청만 ChatController가 Python FastAPI로 넘깁니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /*
   * HTTP 요청별 보안 규칙을 정의합니다.
   *
   * - CSRF는 비활성화합니다. 현재 API는 세션 쿠키 기반 폼 로그인이 아니라
   *   Authorization 헤더의 Basic Auth를 사용하는 stateless API에 가깝기 때문입니다.
   * - CORS는 아래 corsConfigurationSource() Bean의 설정을 사용합니다.
   * - /health는 컨테이너 상태 확인용이므로 인증 없이 허용합니다.
   * - 그 외 모든 요청, 특히 /chat은 반드시 인증을 통과해야 합니다.
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(Customizer.withDefaults())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/health").permitAll()
        .anyRequest().authenticated()
      )
      .httpBasic(Customizer.withDefaults())
      .build();
  }

  /*
   * Basic Auth에서 사용할 사용자를 메모리에 등록합니다.
   *
   * username/password는 application.yml의 app.auth.* 값을 사용하고,
   * Docker Compose 실행 시에는 APP_AUTH_USERNAME, APP_AUTH_PASSWORD 환경변수로 주입됩니다.
   * 간단한 개발/데모 구조라 InMemoryUserDetailsManager를 사용합니다.
   */
  @Bean
  UserDetailsService userDetailsService(
    @Value("${app.auth.username:admin}") String username,
    @Value("${app.auth.password:admin1234}") String password,
    PasswordEncoder passwordEncoder
  ) {
    return new InMemoryUserDetailsManager(
      User.withUsername(username)
        /*
         * Spring Security는 저장된 비밀번호가 인코딩되어 있기를 기대합니다.
         * 평문 비밀번호를 그대로 저장하지 않고 BCrypt 해시로 변환해서 등록합니다.
         */
        .password(passwordEncoder.encode(password))
        .roles("USER")
        .build()
    );
  }

  /*
   * 비밀번호 인코딩 전략입니다.
   * BCrypt는 매번 salt가 달라지는 단방향 해시라 기본 인증용 비밀번호 저장에 적합합니다.
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /*
   * React 개발 서버 또는 Nginx 프론트엔드에서 Spring API를 호출할 수 있도록 CORS를 설정합니다.
   *
   * allowedOrigins는 쉼표로 여러 도메인을 받을 수 있습니다.
   * 예: CORS_ALLOWED_ORIGINS=http://localhost:5173,https://example.com
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
    @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins
  ) {
    CorsConfiguration configuration = new CorsConfiguration();

    /*
     * 환경변수 문자열을 Spring CORS 설정이 요구하는 List<String> 형태로 변환합니다.
     * 공백이나 빈 값은 제거해서 잘못된 origin이 등록되지 않게 합니다.
     */
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
      .map(String::trim)
      .filter(origin -> !origin.isEmpty())
      .toList();

    /*
     * Authorization 헤더를 허용해야 브라우저가 Basic Auth 값을 Spring으로 보낼 수 있습니다.
     * allowCredentials(true)는 인증 정보를 포함한 CORS 요청을 허용한다는 뜻입니다.
     */
    configuration.setAllowedOrigins(origins);
    configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setAllowCredentials(true);

    /*
     * 모든 API 경로에 동일한 CORS 정책을 적용합니다.
     */
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
