package com.khm1102.mediprice.client.hira.detail;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 의료기관별상세정보서비스 {@code getMedOftInfo2.7} — 의료장비.
 * <p>
 * 실제 API 응답 필드명은 {@code oftCd}/{@code oftCdNm}/{@code oftCnt}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MedOftItem(
        String oftCd,
        String oftCdNm,
        Integer oftCnt
) {
}
