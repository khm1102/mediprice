package com.khm1102.mediprice.global.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * mp_token JWT 쿠키 발급/삭제 공통 헬퍼.
 * <p>
 * {@code Cookie} 클래스는 {@code SameSite} 속성을 직접 지원하지 않으므로
 * Spring의 {@code ResponseCookie}를 통해 Set-Cookie 헤더를 직접 작성한다.
 * <ul>
 *   <li>HttpOnly: 항상 true — JS에서 토큰 접근을 막아 XSS 토큰 탈취 위험을 줄인다.</li>
 *   <li>Secure: {@code security.cookie.secure} 환경변수로 토글. 운영 HTTPS에서는 true 권장.</li>
 *   <li>SameSite: {@code security.cookie.same-site} 환경변수. 기본 {@code Lax}.</li>
 *   <li>Path=/, Max-Age는 호출처가 지정 (로그인/탈퇴/로그아웃 모두 helper 사용).</li>
 * </ul>
 */
@Component
public class MpTokenCookieFactory {

    public static final String COOKIE_NAME = "mp_token";

    private final boolean secure;
    private final String sameSite;

    public MpTokenCookieFactory(
            @Value("${security.cookie.secure:false}") boolean secure,
            @Value("${security.cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    /** JWT 토큰을 mp_token 쿠키로 응답에 첨부. */
    public void writeToken(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = baseBuilder(token)
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** mp_token 쿠키 삭제 (Max-Age=0). 속성은 발급 시와 같아야 브라우저가 정확히 매치해 제거한다. */
    public void clearToken(HttpServletResponse response) {
        ResponseCookie cookie = baseBuilder("")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseBuilder(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite);
    }
}
