package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.auth.AuthenticationException;
import com.khm1102.mediprice.global.security.MemberPrincipal;
import com.khm1102.mediprice.global.security.MpTokenCookieFactory;
import com.khm1102.mediprice.service.AuthService;
import com.khm1102.mediprice.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final MpTokenCookieFactory mpTokenCookieFactory;

    public AuthApiController(AuthService authService,
                             JwtUtil jwtUtil,
                             MpTokenCookieFactory mpTokenCookieFactory) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.mpTokenCookieFactory = mpTokenCookieFactory;
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        mpTokenCookieFactory.clearToken(response);
        return ApiResponse.success(null);
    }

    @GetMapping("/token/guest")
    public ApiResponse<Void> guestToken(HttpServletResponse response) {
        String token = authService.generateGuestToken();
        mpTokenCookieFactory.writeToken(response, token, jwtUtil.getExpirationMs() / 1000);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal MemberPrincipal principal) {
        Long memberId = requireMember(principal);
        return ApiResponse.success(Map.of(
                "memberId", memberId,
                "email", principal.email(),
                "role", principal.role()
        ));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal MemberPrincipal principal,
                                      HttpServletResponse response) {
        Long memberId = requireMember(principal);
        authService.withdraw(memberId);
        mpTokenCookieFactory.clearToken(response);
        return ApiResponse.success(null);
    }

    /**
     * SecurityConfig가 1차로 ROLE_MEMBER만 통과시키지만 deep defense로 컨트롤러 단에서도 가드한다.
     */
    private static Long requireMember(MemberPrincipal principal) {
        if (principal == null || principal.isGuest() || principal.memberId() == null) {
            throw new AuthenticationException(ErrorCode.UNAUTHORIZED);
        }
        return principal.memberId();
    }
}
