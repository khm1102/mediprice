package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.service.AuthService;
import com.khm1102.mediprice.service.ConsentService;
import com.khm1102.mediprice.service.GoogleOAuthService;
import com.khm1102.mediprice.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final ConsentService consentService;
    private final GoogleOAuthService googleOAuthService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService,
                          ConsentService consentService,
                          GoogleOAuthService googleOAuthService,
                          JwtUtil jwtUtil) {
        this.authService = authService;
        this.consentService = consentService;
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

            clearStateCookie(response);

            if (token != null) {
                // 기존 회원 → 바로 로그인
                setTokenCookie(response, token);
                return "redirect:/";
            } else {
                // 신규 회원 → 약관 동의 페이지로 이동
                String consentKey = consentService.store(
                        userInfo.email(), userInfo.name(), "google", userInfo.id());
                setConsentKeyCookie(response, consentKey);
                return "redirect:/auth/consent";
            }
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    /** 약관 동의 페이지 */
    @GetMapping("/consent")
    public String consentPage(HttpServletRequest request, Model model) {
        String consentKey = extractCookie(request, "consent_key");
        Optional<ConsentService.PendingConsent> pc = consentService.retrieve(consentKey);
        if (pc.isEmpty()) {
            // 유효하지 않은 접근 → 메인으로
            return "redirect:/";
        }
        model.addAttribute("userName", pc.get().name());
        model.addAttribute("userEmail", pc.get().email());
        return "auth/consent";
    }

    /** 약관 동의 제출 */
    @PostMapping("/consent")
    public String consentSubmit(@RequestParam(defaultValue = "false") boolean termsAgreed,
                                @RequestParam(defaultValue = "false") boolean privacyAgreed,
                                @RequestParam(defaultValue = "false") boolean locationAgreed,
                                @RequestParam(defaultValue = "false") boolean ageAgreed,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        String consentKey = extractCookie(request, "consent_key");
        Optional<ConsentService.PendingConsent> pc = consentService.retrieve(consentKey);
        if (pc.isEmpty()) {
            return "redirect:/";
        }

        // 필수 항목 미동의 시 다시 동의 페이지로
        if (!termsAgreed || !privacyAgreed || !locationAgreed || !ageAgreed) {
            return "redirect:/auth/consent?error=required";
        }

        ConsentService.PendingConsent pending = pc.get();
        String token = authService.registerNewMember(
                pending.email(), pending.name(), pending.provider(), pending.oauthId());

        consentService.remove(consentKey);
        clearConsentKeyCookie(response);
        setTokenCookie(response, token);

        return "redirect:/";
    }

    // ── 쿠키 헬퍼 ─────────────────────────────────────────────────────

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("mp_token", token);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtUtil.getExpirationMs() / 1000));
        response.addCookie(cookie);
    }

    private void setConsentKeyCookie(HttpServletResponse response, String key) {
        Cookie cookie = new Cookie("consent_key", key);
        cookie.setHttpOnly(true);
        cookie.setPath("/auth/consent");
        cookie.setMaxAge(600); // 10분
        response.addCookie(cookie);
    }

    private void clearConsentKeyCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("consent_key", "");
        cookie.setMaxAge(0);
        cookie.setPath("/auth/consent");
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
