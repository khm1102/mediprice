package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getSpclDiagInfo2.7} — 특수진료(진료가능분야).
 * <p>
 * 실제 API 응답 필드명은 {@code srchCd}/{@code srchCdNm}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpclDiagItem(
        String srchCd,
        String srchCdNm
) {
}
