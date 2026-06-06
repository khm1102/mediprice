package com.khm1102.mediprice.global.exception;

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
    TOKEN_EXPIRED("A002", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("A003", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("A004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    UNAUTHORIZED("A005", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),

    // 배치 (B)
    BATCH_ADMIN_DISABLED("B001", "배치 수동 실행이 비활성화되어 있습니다.", HttpStatus.FORBIDDEN),
    BATCH_ALREADY_RUNNING("B002", "이미 실행 중인 배치가 있습니다.", HttpStatus.CONFLICT),

    // 비즈니스 — 병원 (H)
    HOSPITAL_NOT_FOUND("H001", "병원 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 비즈니스 — 즐겨찾기 (F)
    FAVORITE_NOT_FOUND("F001", "즐겨찾기 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FAVORITE_ALREADY_EXISTS("F002", "이미 즐겨찾기에 추가된 병원입니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
