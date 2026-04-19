package com.khm1102.mediprice.dto;

import com.khm1102.mediprice.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;

    private ApiResponse(boolean success, T data, ErrorDetail error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

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

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, new ErrorDetail("UNKNOWN", message));
    }
}
