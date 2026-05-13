package com.khm1102.mediprice.global.common;

import com.khm1102.mediprice.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    /** 성공이면 data만 채우고 error는 비워두는지. */
    @Test
    void successWrapsDataAndLeavesErrorNull() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isEqualTo("hello");
        assertThat(response.error()).isNull();
    }

    /** ErrorCode만 넘기면 그 안의 code/message 그대로 쓰는지. */
    @Test
    void errorUsesErrorCodeMessage() {
        ApiResponse<Object> response = ApiResponse.error(ErrorCode.HOSPITAL_NOT_FOUND);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().code()).isEqualTo("H001");
        assertThat(response.error().message()).isEqualTo(ErrorCode.HOSPITAL_NOT_FOUND.getMessage());
    }

    /** 두 번째 인자 메시지 넘기면 ErrorCode 기본 메시지 덮어씀 (검증 실패 같은 케이스용). */
    @Test
    void errorWithDetailOverridesDefaultMessage() {
        ApiResponse<Object> response = ApiResponse.error(ErrorCode.INVALID_INPUT, "ykiho는 필수입니다");

        assertThat(response.error().code()).isEqualTo("C002");
        assertThat(response.error().message()).isEqualTo("ykiho는 필수입니다");
    }
}
