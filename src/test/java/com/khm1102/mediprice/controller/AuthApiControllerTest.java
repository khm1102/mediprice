package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.auth.AuthenticationException;
import com.khm1102.mediprice.global.security.MemberPrincipal;
import com.khm1102.mediprice.global.security.MpTokenCookieFactory;
import com.khm1102.mediprice.service.AuthService;
import com.khm1102.mediprice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthApiControllerTest {

    @Mock AuthService authService;
    @Mock JwtUtil jwtUtil;
    @Mock MpTokenCookieFactory cookieFactory;

    private static final MemberPrincipal MEMBER = new MemberPrincipal(7L, "u@x", "MEMBER", "Name");
    private static final MemberPrincipal GUEST = new MemberPrincipal(null, "g-uuid", "GUEST", null);

    private AuthApiController controller() {
        return new AuthApiController(authService, jwtUtil, cookieFactory);
    }

    /** GUEST principal이 /api/auth/me를 호출하면 컨트롤러 단 가드가 차단한다. */
    @Test
    void rejectsGuestOnMe() {
        assertThatThrownBy(() -> controller().me(GUEST))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    /** null principal로 /api/auth/me 호출 → 차단. */
    @Test
    void rejectsNullPrincipalOnMe() {
        assertThatThrownBy(() -> controller().me(null))
                .isInstanceOf(AuthenticationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    /** MEMBER principal은 me() 통과. */
    @Test
    void allowsMemberOnMe() {
        ApiResponse<Map<String, Object>> response = controller().me(MEMBER);

        assertThat(response.success()).isTrue();
        assertThat(response.data())
                .containsEntry("memberId", 7L)
                .containsEntry("role", "MEMBER")
                .containsEntry("email", "u@x");
    }

    /** GUEST/null principal이 DELETE /api/auth/me를 호출하면 가드가 차단하고 withdraw가 호출되지 않는다. */
    @Test
    void rejectsGuestOnWithdraw() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> controller().withdraw(GUEST, response))
                .isInstanceOf(AuthenticationException.class);

        verify(authService, never()).withdraw(org.mockito.ArgumentMatchers.anyLong());
        verify(cookieFactory, never()).clearToken(response);
    }

    /** MEMBER가 탈퇴하면 cookieFactory.clearToken이 호출돼 mp_token이 제거된다. */
    @Test
    void clearsTokenCookieOnWithdraw() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller().withdraw(MEMBER, response);

        verify(authService).withdraw(7L);
        verify(cookieFactory).clearToken(response);
    }

    /** logout()은 토큰 검증 없이 쿠키만 제거. */
    @Test
    void logoutClearsCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller().logout(response);

        verify(cookieFactory).clearToken(response);
        verify(authService, never()).withdraw(org.mockito.ArgumentMatchers.anyLong());
    }

    /** guestToken()은 발급된 JWT를 cookieFactory.writeToken으로 전달. */
    @Test
    void guestTokenWritesCookieWithExpirationSeconds() {
        when(authService.generateGuestToken()).thenReturn("dummy-guest-jwt");
        when(jwtUtil.getExpirationMs()).thenReturn(60_000L);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller().guestToken(response);

        verify(cookieFactory).writeToken(response, "dummy-guest-jwt", 60L);
    }
}
