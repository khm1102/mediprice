package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getMedOftInfo2.7} — 의료장비.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedOftItem(
        String medOftCd,
        String medOftCdNm,
        Integer medOftCnt
) {
}
