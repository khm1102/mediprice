package com.khm1102.mediprice.exception.auth;

import com.khm1102.mediprice.exception.ErrorCode;

/**
 * 우리 도메인의 인가 실패 예외.
 * <p>
 * Spring Security의 {@code org.springframework.security.access.AccessDeniedException}과 이름이 겹치지 않도록
 * 명시적으로 분리. Security 단계 인가 실패는 SecurityConfig의 AccessDeniedHandler가 처리하고,
 * 본 예외는 GlobalExceptionHandler가 처리한다.
 */
public class AuthorizationDeniedException extends AuthenticationException {

    public AuthorizationDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}
