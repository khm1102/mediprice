package com.khm1102.mediprice.dto;

import com.khm1102.mediprice.exception.ErrorCode;

/**
 * 모든 REST API 응답의 공통 래퍼.
 * <p>
 * Java 21 record + Jackson 3 native 지원.
 * <p>
 * 응답 모델 전용 — 외부 API 클라이언트의 입력 모델로 재사용하지 않는다.
 */
public record ApiResponse<T>(boolean success, T data, ErrorDetail error) {

    public record ErrorDetail(String code, String message) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorDetail(errorCode.getCode(), errorCode.getMessage()));
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String detailMessage) {
        return new ApiResponse<>(false, null, new ErrorDetail(errorCode.getCode(), detailMessage));
    }
}
