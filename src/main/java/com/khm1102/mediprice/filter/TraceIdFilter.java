package com.khm1102.mediprice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 요청마다 traceId를 발급/전파하는 필터.
 * <ul>
 *   <li>요청에 {@code X-Trace-Id} 헤더가 있고 형식이 안전하면 그 값을 사용 (게이트웨이/Cloudflare에서 전파).</li>
 *   <li>없거나 형식 불일치면 UUID 앞 8자로 생성.</li>
 *   <li>MDC {@code traceId} 키에 세팅 → logback 패턴 {@code %X{traceId}}로 출력.</li>
 *   <li>응답 헤더 {@code X-Trace-Id}로도 노출 → 클라이언트가 장애 추적 시 사용.</li>
 * </ul>
 * 외부 입력은 정규식으로 검증하여 로그 인젝션/CRLF/HTTP response splitting을 차단.
 * 인증 필터 등 다른 필터보다 먼저 실행되어야 모든 로그에 traceId가 잡힘.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    /** logback.xml의 {@code %X{traceId}} 패턴과 동일해야 함. 양쪽 동기화 필수. */
    public static final String MDC_KEY = "traceId";
    public static final String HEADER_NAME = "X-Trace-Id";

    /** W3C trace-id(32 hex) + UUID(36자 hyphen 포함) 모두 허용하는 안전 문자셋. */
    private static final Pattern VALID_TRACE_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = sanitize(request.getHeader(HEADER_NAME));
        if (traceId == null) {
            // 32-bit(8자)는 birthday paradox로 ~65K 요청에서 충돌 → UUID 전체(32 hex) 사용
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_NAME, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String sanitize(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        return VALID_TRACE_ID.matcher(candidate).matches() ? candidate : null;
    }
}
