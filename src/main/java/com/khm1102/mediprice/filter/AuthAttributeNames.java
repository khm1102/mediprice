package com.khm1102.mediprice.filter;

/**
 * 인증 필터와 SecurityConfig EntryPoint 간 공유되는 request attribute 키.
 * <p>
 * 외부 코드(컨트롤러, 다른 필터)가 임의로 set하지 말 것 — 인증 결과 위조 가능성.
 */
public final class AuthAttributeNames {

    /**
     * JwtAuthFilter가 토큰 만료/변조 시 {@link com.khm1102.mediprice.exception.ErrorCode}를 담아
     * AuthenticationEntryPoint가 응답 코드를 결정할 때 참조하는 키.
     */
    public static final String ERROR_CODE = "auth.errorCode";

    private AuthAttributeNames() {
    }
}
