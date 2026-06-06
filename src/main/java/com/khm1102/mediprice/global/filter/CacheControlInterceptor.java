package com.khm1102.mediprice.global.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

/**
 * 응답 method + status + URI를 모두 본 후 결정되는 Cache-Control 정책.
 * <p>
 * preHandle에서 헤더를 미리 박으면 컨트롤러가 4xx/5xx를 던져도 캐시 헤더가 남아
 * proxy/CDN/브라우저가 에러 응답을 캐싱하는 위험이 있다. 그래서 afterCompletion 시점에
 * 응답 status와 method를 확인한 뒤 헤더를 결정한다.
 * <p>
 * {@code ShallowEtagHeaderFilter}가 응답을 버퍼링하므로 이 시점에 response는 아직 commit되지 않아
 * 헤더 변경이 가능하다. 만약 어떤 이유로 이미 commit된 상태라면 정책 적용을 포기한다(부작용 없음).
 * <ul>
 *   <li>GET + 2xx + {@code /api/items[/**]} → {@code public, max-age=300}</li>
 *   <li>GET + 2xx + {@code /api/hospitals/{ykiho}/basics} → {@code public, max-age=300}</li>
 *   <li>GET + 2xx + {@code /api/hospitals/search} → {@code private, max-age=60}
 *       (좌표가 사용자 단말마다 다르므로 CDN/공유 캐시 금지)</li>
 *   <li>그 외 (다른 메서드, 에러 status, 회원 전용·쓰기·internal) → {@code no-store}</li>
 * </ul>
 */
public class CacheControlInterceptor implements HandlerInterceptor {

    static final String API_PREFIX = "/api/";
    static final String ITEMS_PATH = "/api/items";
    static final String SEARCH_PATH = "/api/hospitals/search";
    static final Pattern BASICS_PATTERN =
            Pattern.compile("^/api/hospitals/[^/]+/basics$");

    static final String CACHE_ITEMS = "public, max-age=300";
    /** 좌표 기반 결과라 공유 캐시 금지 — private. */
    static final String CACHE_SEARCH = "private, max-age=60";
    static final String CACHE_BASICS = "public, max-age=300";
    static final String CACHE_NONE = "no-store";

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                @Nullable Exception ex) {
        if (response.isCommitted()) {
            return;
        }
        String path = request.getRequestURI();
        if (path == null || !path.startsWith(API_PREFIX)) {
            return;
        }
        String policy = resolve(request.getMethod(), response.getStatus(), path);
        response.setHeader(HttpHeaders.CACHE_CONTROL, policy);
    }

    /**
     * method + status + path를 모두 본 정책 결정.
     * GET 외 메서드 또는 비2xx 응답이면 무조건 {@link #CACHE_NONE}.
     */
    static String resolve(@Nullable String method, int status, String path) {
        if (!"GET".equalsIgnoreCase(method)) {
            return CACHE_NONE;
        }
        if (status < 200 || status >= 300) {
            return CACHE_NONE;
        }
        if (path.equals(SEARCH_PATH)) {
            return CACHE_SEARCH;
        }
        if (BASICS_PATTERN.matcher(path).matches()) {
            return CACHE_BASICS;
        }
        if (path.equals(ITEMS_PATH) || path.startsWith(ITEMS_PATH + "/")) {
            return CACHE_ITEMS;
        }
        return CACHE_NONE;
    }
}
