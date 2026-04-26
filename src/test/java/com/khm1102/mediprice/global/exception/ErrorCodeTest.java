package com.khm1102.mediprice.global.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorCodeTest {

    /** 모든 ErrorCode 값은 code/message/httpStatus 셋 다 채워야 함 (응답 누락 방지). */
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyValueIsFullyInitialized(ErrorCode code) {
        assertThat(code.getCode()).isNotBlank();
        assertThat(code.getMessage()).isNotBlank();
        assertThat(code.getHttpStatus()).isNotNull();
    }

    /** 5xx는 INTERNAL_ERROR 하나뿐이어야 함 — 다른 도메인 에러가 5xx로 들어가면 GlobalExceptionHandler가 stack까지 찍어버림. */
    @Test
    void onlyInternalErrorIsServerError() {
        long serverErrorCount = java.util.Arrays.stream(ErrorCode.values())
                .filter(c -> c.getHttpStatus().is5xxServerError())
                .count();

        assertThat(serverErrorCount).isEqualTo(1);
        assertThat(ErrorCode.INTERNAL_ERROR.getHttpStatus().is5xxServerError()).isTrue();
    }
}
