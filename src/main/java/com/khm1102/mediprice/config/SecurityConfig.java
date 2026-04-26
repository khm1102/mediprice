package com.khm1102.mediprice.config;

import com.khm1102.mediprice.dto.ApiResponse;
import com.khm1102.mediprice.exception.ErrorCode;
import com.khm1102.mediprice.filter.AuthAttributeNames;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정.
 * <p>
 * 두 개의 SecurityFilterChain으로 분리:
 * <ul>
 *   <li>{@link #apiSecurityFilterChain} — {@code /api/**} 전용. 인증 실패 시 JSON 응답.</li>
 *   <li>{@link #pageSecurityFilterChain} — 그 외 페이지 흐름. P2(로그인 도입)에서 redirect 정책 추가 예정.</li>
 * </ul>
 * CORS는 {@link #corsConfigurationSource}에서 단일 정의 — WebMvcConfig에 중복 정의 금지.
 * <p>
 * <b>JwtAuthFilter 등록 방침 (P1):</b> {@code WebAppInitializer.getServletFilters()}가 아닌
 * {@code apiSecurityFilterChain}의 {@code .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)}로
 * 등록한다. 이유: Security 컨텍스트 통합, 빈 주입 자유, 인증 정책과 한 곳에서 관리.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JsonMapper jsonMapper;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /**
     * /api/** REST 엔드포인트 전용 체인. {@link Order} 1로 우선 평가 — page 체인이 먼저 매칭되면
     * /api/** 까지 page 흐름으로 처리되어 분리가 무의미해진다.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsSource) {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/token/guest",
                                "/api/hospitals/**",
                                "/api/health"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(apiAuthenticationEntryPoint())
                        .accessDeniedHandler(apiAccessDeniedHandler())
                );
        return http.build();
    }

    /**
     * 페이지 흐름 체인. securityMatcher 없음 = 모든 요청 매칭. {@link Order} 2로 api 체인 다음에 평가.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain pageSecurityFilterChain(HttpSecurity http, CorsConfigurationSource corsSource) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // 현재 corsSource는 /api/** 매핑만 등록 — 페이지 경로엔 자동 미적용.
                // 향후 페이지 흐름에 cross-origin이 필요한 경로가 생겨도 SecurityFilterChain은 이미 활성화됨.
                .cors(cors -> cors.configurationSource(corsSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 현재는 모든 페이지 permitAll. P2 로그인 도입 시 /auth/** 외엔 authenticated + formLogin redirect 추가.
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /**
     * CORS 정책 단일 정의. {@link Primary}는 Spring MVC의 {@code HandlerMappingIntrospector}가
     * 동일 인터페이스({@code CorsConfigurationSource})를 구현하여 자동 주입 모호성이 발생하기 때문에 필요.
     */
    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", buildApiCorsConfig());
        return source;
    }

    private CorsConfiguration buildApiCorsConfig() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException("cors.allowed-origins 설정이 비어있습니다 (CORS_ALLOWED_ORIGINS 환경변수 확인)");
        }

        return getCorsConfiguration(origins);
    }

    private static @NonNull CorsConfiguration getCorsConfiguration(List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        // allowCredentials=true에서는 와일드카드(*)가 Authorization 헤더와 매치 안 됨 → 명시 화이트리스트
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id", "X-Requested-With"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    @Bean
    public AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return (request, response, authException) -> writeJsonError(response, resolveAuthErrorCode(request));
    }

    @Bean
    public AccessDeniedHandler apiAccessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                writeJsonError(response, ErrorCode.ACCESS_DENIED);
    }

    /**
     * JwtAuthFilter 등이 request attribute에 담아둔 ErrorCode를 우선 사용. 없으면 일반 인증 누락으로 간주.
     */
    private ErrorCode resolveAuthErrorCode(HttpServletRequest request) {
        Object stored = request.getAttribute(AuthAttributeNames.ERROR_CODE);
        if (stored instanceof ErrorCode errorCode) {
            return errorCode;
        }
        return ErrorCode.UNAUTHORIZED;
    }

    private void writeJsonError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.error(errorCode)));
    }
}
