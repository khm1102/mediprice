package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 비급여진료비정보서비스 {@code getNonPaymentItemCodeList2} 응답 item.
 * <p>
 * {@code adtEndDd = "99991231"}이면 현재 유효한 항목. MVP에선 모두 저장하고 조회 시 필터.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NonPayCodeItem(
        String npayCd,
        String npayKorNm,
        String npayMdivCd,
        String npayMdivCdNm,
        String npaySdivCd,
        String npaySdivCdNm,
        String adtFrDd,
        String adtEndDd
) {
}
