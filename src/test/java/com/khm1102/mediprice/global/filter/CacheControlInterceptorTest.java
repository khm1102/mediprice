package com.khm1102.mediprice.global.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CacheControlInterceptorTest {

    private final CacheControlInterceptor interceptor = new CacheControlInterceptor();

    // ── resolve(method, status, path) 단위 ───────────────────────────────────

    @Test
    void getOn200ItemsRootIsPublicFiveMinutes() {
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/items"))
                .isEqualTo(CacheControlInterceptor.CACHE_ITEMS);
    }

    @Test
    void getOn200ItemsSubpathIsPublicFiveMinutes() {
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/items/MX1220000"))
                .isEqualTo(CacheControlInterceptor.CACHE_ITEMS);
    }

    @Test
    void getOn200SearchIsPrivateSixtySeconds() {
        // 좌표가 사용자 단말마다 다르므로 CDN 공유 캐시 금지 → private.
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/hospitals/search"))
                .isEqualTo(CacheControlInterceptor.CACHE_SEARCH);
        assertThat(CacheControlInterceptor.CACHE_SEARCH)
                .isEqualTo("private, max-age=60");
    }

    @Test
    void getOn200BasicsIsPublicFiveMinutes() {
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/hospitals/YK123/basics"))
                .isEqualTo(CacheControlInterceptor.CACHE_BASICS);
    }

    @Test
    void favoritesAndAuthAreNoStoreEvenOnSuccess() {
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/favorites"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/auth/me"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("POST", 200, "/api/internal/batch/sync"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    @Test
    void hospitalDetailsAndExtrasAreNoStore() {
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/hospitals/YK123/extras"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("GET", 200, "/api/hospitals/YK123"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    // ── method/status 가드 (회귀 핵심) ────────────────────────────────────────

    @Test
    void nonGetMethodNeverGetsCacheEvenOnCachedPath() {
        assertThat(CacheControlInterceptor.resolve("POST", 200, "/api/items"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("DELETE", 200, "/api/hospitals/search"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("PUT", 200, "/api/hospitals/YK1/basics"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    @Test
    void errorStatusNeverGetsCacheEvenOnCachedPath() {
        // 400/404/500 같은 에러 응답이 캐시되면 사용자가 일시 에러를 영속 캐시로 보게 된다.
        assertThat(CacheControlInterceptor.resolve("GET", 400, "/api/hospitals/search"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("GET", 404, "/api/hospitals/YK1/basics"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("GET", 500, "/api/items"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    @Test
    void redirectStatusNeverGetsCache() {
        assertThat(CacheControlInterceptor.resolve("GET", 301, "/api/items"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
        assertThat(CacheControlInterceptor.resolve("GET", 304, "/api/items"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    @Test
    void nullMethodIsNoStore() {
        // 비정상 입력 방어 — null이면 GET이 아니므로 no-store.
        assertThat(CacheControlInterceptor.resolve(null, 200, "/api/items"))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    // ── afterCompletion() — 실제 인터셉터 동작 ────────────────────────────────

    @Test
    void afterCompletionAppliesPrivateForSearchOn200() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/hospitals/search");
        req.setRequestURI("/api/hospitals/search");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(200);

        interceptor.afterCompletion(req, res, new Object(), null);

        assertThat(res.getHeader(HttpHeaders.CACHE_CONTROL))
                .isEqualTo(CacheControlInterceptor.CACHE_SEARCH);
    }

    @Test
    void afterCompletionAppliesNoStoreOnError() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/hospitals/search");
        req.setRequestURI("/api/hospitals/search");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(500);

        interceptor.afterCompletion(req, res, new Object(), new RuntimeException("boom"));

        assertThat(res.getHeader(HttpHeaders.CACHE_CONTROL))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    @Test
    void afterCompletionSkipsNonApiPaths() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/hospitals");
        req.setRequestURI("/hospitals");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(200);

        interceptor.afterCompletion(req, res, new Object(), null);

        assertThat(res.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
    }

    @Test
    void afterCompletionDefaultsToNoStoreForUnclassifiedApi() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/favorites");
        req.setRequestURI("/api/favorites");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(200);

        interceptor.afterCompletion(req, res, new Object(), null);

        assertThat(res.getHeader(HttpHeaders.CACHE_CONTROL))
                .isEqualTo(CacheControlInterceptor.CACHE_NONE);
    }

    @Test
    void afterCompletionDoesNotTouchAlreadyCommittedResponse() {
        // 어떤 이유로 응답이 이미 commit됐다면 헤더 변경이 불가능 — 부작용 없이 종료해야 한다.
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/hospitals/search");
        req.setRequestURI("/api/hospitals/search");
        MockHttpServletResponse res = new MockHttpServletResponse();
        res.setStatus(200);
        res.setCommitted(true);

        interceptor.afterCompletion(req, res, new Object(), null);

        assertThat(res.getHeader(HttpHeaders.CACHE_CONTROL)).isNull();
    }

    // ── 설정 회귀 방지: 필터/인터셉터 등록 정적 검증 ─────────────────────────

    @Test
    void webAppInitializerRegistersApiOnlyShallowEtagFilter() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/com/khm1102/mediprice/global/config/WebAppInitializer.java"));
        assertThat(src)
                .as("WebAppInitializer에 ShallowEtagHeaderFilter import가 있어야 한다")
                .contains("import org.springframework.web.filter.ShallowEtagHeaderFilter");
        assertThat(src)
                .as("ShallowEtagHeaderFilter는 JSP 페이지가 아닌 /api/*에만 적용해야 한다")
                .contains("new ShallowEtagHeaderFilter()")
                .contains("addMappingForUrlPatterns")
                .contains("\"/api/*\"");
        assertThat(src)
                .as("JSP 페이지 응답 body를 삼키는 회귀 방지를 위해 servlet filter 배열에는 포함하지 않는다")
                .contains("return new Filter[]{traceIdFilter, encodingFilter};")
                .doesNotContain("return new Filter[]{traceIdFilter, encodingFilter, etagFilter};");
    }

    @Test
    void webMvcConfigRegistersCacheControlInterceptor() throws IOException {
        String src = Files.readString(Path.of(
                "src/main/java/com/khm1102/mediprice/global/config/WebMvcConfig.java"));
        assertThat(src)
                .as("WebMvcConfig가 addInterceptors로 CacheControlInterceptor를 등록해야 한다")
                .contains("addInterceptors")
                .contains("new CacheControlInterceptor()")
                .contains("/api/**");
    }
}
