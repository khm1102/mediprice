package com.khm1102.mediprice.global.exception;

import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import com.khm1102.mediprice.global.exception.business.HospitalNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    /** 5xx ErrorCode 예외는 500 + INTERNAL_ERROR 코드로 응답 (logError로 stack까지 남김). */
    @Test
    void mediPriceExceptionWith5xxReturnsServerError() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMediPriceException(new BusinessException(ErrorCode.INTERNAL_ERROR));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("C001");
    }

    /** 4xx 도메인 예외는 ErrorCode가 지정한 상태코드 그대로. */
    @Test
    void mediPriceExceptionWith4xxReturnsDomainStatus() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleMediPriceException(new HospitalNotFoundException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error().code()).isEqualTo("H001");
    }

    /** BindException은 첫 필드에러를 detail에 담음 (전체 나열은 노이즈라 의도적으로 1개만). */
    @Test
    void bindExceptionPutsFirstFieldErrorInDetail() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "lat", "범위를 벗어났습니다"));
        bindingResult.addError(new FieldError("target", "lng", "다른 에러"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleBind(new BindException(bindingResult));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().message()).isEqualTo("lat: 범위를 벗어났습니다");
    }

    /** 필드에러가 비어있으면 ErrorCode.INVALID_INPUT 기본 메시지로 fallback. */
    @Test
    void bindExceptionFallsBackToDefaultMessageWhenNoFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBind(new BindException(bindingResult));

        assertThat(response.getBody().error().message()).isEqualTo(ErrorCode.INVALID_INPUT.getMessage());
    }

    /** 누락된 query param은 어떤 파라미터인지 detail에 노출 (디버깅 친화). */
    @Test
    void missingParameterIncludesNameInDetail() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleMissingParam(
                new MissingServletRequestParameterException("lat", "double"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().message()).contains("lat", "double", "누락");
    }

    /** 모르는 RuntimeException은 INTERNAL_ERROR로 격상 (운영 알림 트리거 + 클라이언트엔 일반 메시지). */
    @Test
    void runtimeExceptionFallsBackToInternalError() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleRuntime(new RuntimeException("뭔가 터짐"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error().code()).isEqualTo("C001");
    }
}
