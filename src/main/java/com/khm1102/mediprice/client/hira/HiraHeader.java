package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 심평원 API 공통 응답 헤더.
 * <p>
 * 정상: {@code resultCode = "00"} (NORMAL_CODE)<br>
 * 데이터 없음: {@code resultCode = "03"} (NODATA_ERROR)<br>
 * 트래픽 초과: {@code resultCode = "22"}<br>
 * 그 외 코드는 hira-docs OpenAPI 활용가이드 참조.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HiraHeader(String resultCode, String resultMsg) {

    public static final String NORMAL_CODE = "00";

    public boolean isSuccess() {
        return NORMAL_CODE.equals(resultCode);
    }
}
