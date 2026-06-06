package com.khm1102.mediprice.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MpTokenCookieFactoryTest {

    /** 발급 시 Set-Cookie 헤더에 HttpOnly + SameSite + Path=/ + Max-Age가 포함된다. */
    @Test
    void writeTokenSetsAllSecurityAttributes() {
        MpTokenCookieFactory factory = new MpTokenCookieFactory(true, "Strict");
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory.writeToken(response, "jwt-value", 600);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).startsWith("mp_token=jwt-value");
        assertThat(setCookie).contains("Path=/");
        assertThat(setCookie).contains("Max-Age=600");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Strict");
    }

    /** secure=false면 Secure 속성 미포함. SameSite 기본값(Lax)으로 동작. */
    @Test
    void writeTokenSkipsSecureWhenDisabled() {
        MpTokenCookieFactory factory = new MpTokenCookieFactory(false, "Lax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory.writeToken(response, "jwt-value", 600);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).doesNotContain("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
        assertThat(setCookie).contains("HttpOnly");
    }

    /** clearToken은 빈 값 + Max-Age=0으로 동일 속성을 가진 쿠키를 발급. */
    @Test
    void clearTokenSendsZeroMaxAgeCookie() {
        MpTokenCookieFactory factory = new MpTokenCookieFactory(true, "Lax");
        MockHttpServletResponse response = new MockHttpServletResponse();

        factory.clearToken(response);

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("mp_token=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("Secure");
        assertThat(setCookie).contains("SameSite=Lax");
    }
}
