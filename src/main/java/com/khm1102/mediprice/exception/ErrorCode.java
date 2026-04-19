package com.khm1102.mediprice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통 (C)
    INTERNAL_ERROR("C001", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("C002", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("C003", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("C004", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),

    // 인증 (A)
    LOGIN_FAILED("A001", "이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("A002", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("A003", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("A004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 비즈니스 — 게스트 (G)
    GUEST_SEARCH_LIMIT("G001", "비회원 검색 횟수를 초과했습니다. 로그인 후 이용해주세요.", HttpStatus.TOO_MANY_REQUESTS),

    // 비즈니스 — 병원 (H)
    HOSPITAL_NOT_FOUND("H001", "병원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PRICE_NOT_FOUND("H002", "가격 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 비즈니스 — 회원 (M)
    MEMBER_NOT_FOUND("M001", "회원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("M002", "이미 가입된 이메일입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
