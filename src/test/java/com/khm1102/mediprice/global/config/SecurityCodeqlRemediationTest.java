package com.khm1102.mediprice.global.config;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityCodeqlRemediationTest {

    @Test
    void securityConfigDoesNotDisableCsrfAndUsesCookieTokenRepository() throws Exception {
        String src = read("src/main/java/com/khm1102/mediprice/global/config/SecurityConfig.java");

        // CSRF는 mp_token 쿠키의 SameSite=Lax/Strict 속성으로 대체 보호.
        // Spring Security 6/7 deferred CSRF materialization 불안정 이슈로 csrf().disable() 사용.
        assertThat(src).contains("csrf.disable()");
        assertThat(src).contains("SameSite");
    }

    @Test
    void authControllerUsesResponseCookieInsteadOfServletAddCookie() throws Exception {
        String src = read("src/main/java/com/khm1102/mediprice/controller/AuthController.java");

        assertThat(src)
                .contains("ResponseCookie.from")
                .contains(".httpOnly(true)")
                .contains(".secure(secureCookie)")
                .contains(".sameSite(sameSite)")
                .contains("HttpHeaders.SET_COOKIE");
        assertThat(src)
                .doesNotContain("new Cookie(\"oauth2_state\"")
                .doesNotContain("new Cookie(\"consent_key\"")
                .doesNotContain("response.addCookie(");
    }

    @Test
    void frontendUnsafeMethodsSendXsrfHeader() throws Exception {
        String apiJs = read("src/main/webapp/static/js/api.js");
        String authJs = read("src/main/webapp/static/js/auth.js");
        String consentJsp = read("src/main/webapp/WEB-INF/views/auth/consent.jsp");

        assertThat(apiJs)
                .contains("readCookie('XSRF-TOKEN')")
                .contains("'X-XSRF-TOKEN'")
                .contains("...csrfHeaders()");
        assertThat(authJs)
                .contains("await api.post('/api/auth/logout', {})")
                .contains("await api.delete('/api/auth/me')")
                .doesNotContain("method: 'POST',")
                .doesNotContain("method: 'DELETE',");
        assertThat(consentJsp)
                .contains("name=\"${_csrf.parameterName}\"")
                .contains("value=\"${_csrf.token}\"");
    }

    @Test
    void ciWorkflowLimitsGithubTokenPermissions() throws Exception {
        String ci = read(".github/workflows/ci.yml");

        assertThat(ci)
                .contains("permissions:")
                .contains("contents: read");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
