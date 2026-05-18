package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.service.AuthService;
import com.khm1102.mediprice.service.GoogleOAuthService;
import com.khm1102.mediprice.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          GoogleOAuthService googleOAuthService,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.googleOAuthService = googleOAuthService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/oauth2/authorize/google")
    public void startGoogleOAuth(HttpServletResponse response) throws IOException {
        String state = UUID.randomUUID().toString();

        Cookie stateCookie = new Cookie("oauth2_state", state);
        stateCookie.setHttpOnly(true);
        stateCookie.setPath("/");
        stateCookie.setMaxAge(300); // 5분
        response.addCookie(stateCookie);

        response.sendRedirect(googleOAuthService.buildAuthorizationUrl(state));
    }

    @GetMapping("/oauth2/callback")
    public String googleOAuthCallback(@RequestParam(required = false) String code,
                                      @RequestParam(required = false) String state,
                                      @RequestParam(required = false) String error,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        if (error != null) {
            return "redirect:/";
        }

        String stateCookie = extractCookie(request, "oauth2_state");
        if (stateCookie == null || !stateCookie.equals(state)) {
            return "redirect:/";
        }

        try {
            GoogleOAuthService.GoogleUserInfo userInfo = googleOAuthService.exchangeCodeForUserInfo(code);
            String token = authService.handleOAuthLogin(
                    userInfo.email(), userInfo.name(), "google", userInfo.id());

            setTokenCookie(response, token);
            clearStateCookie(response);

            return "redirect:/";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("mp_token", token);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtUtil.getExpirationMs() / 1000));
        response.addCookie(cookie);
    }

    private void clearStateCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("oauth2_state", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
