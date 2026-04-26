package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getSpclDiagInfo2.7} — 특수진료(진료가능분야).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpclDiagItem(
        String srvTpCd,
        String srvTpCdNm
) {
}
