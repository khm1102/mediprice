package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.security.MemberPrincipal;
import com.khm1102.mediprice.service.AuthService;
import com.khm1102.mediprice.util.JwtUtil;
import jakarta.servlet.http.Cookie;
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

    public AuthApiController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("mp_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ApiResponse.success(null);
    }

    @GetMapping("/token/guest")
    public ApiResponse<Void> guestToken(HttpServletResponse response) {
        String token = authService.generateGuestToken();
        Cookie cookie = new Cookie("mp_token", token);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtUtil.getExpirationMs() / 1000));
        response.addCookie(cookie);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@AuthenticationPrincipal MemberPrincipal principal) {
        return ApiResponse.success(Map.of(
                "memberId", principal.memberId(),
                "email", principal.email(),
                "role", principal.role()
        ));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal MemberPrincipal principal,
                                      HttpServletResponse response) {
        authService.withdraw(principal.memberId());
        Cookie cookie = new Cookie("mp_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ApiResponse.success(null);
    }
}
