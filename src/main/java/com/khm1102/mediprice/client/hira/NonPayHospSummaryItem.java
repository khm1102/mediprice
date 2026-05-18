package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 비급여진료비정보서비스 {@code getNonPaymentItemHospList2} 응답 item — 병원×항목 시기별 가격 요약.
 * <p>
 * {@link NonPayDtlItem}이 (ykiho, npayCd)당 활성 가격 1건만 제공하는 것과 달리, 본 API는
 * 시기별 row + {@code minPrc}/{@code maxPrc} 범위 + 지역/종별 메타데이터를 묶어 제공.
 * 따라서 별도 {@code PriceSummary} 테이블에 시기까지 보존하여 저장한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NonPayHospSummaryItem(
        String ykiho,
        String yadmNm,
        String clCd,
        String clCdNm,
        String sidoCd,
        String sidoCdNm,
        String sgguCd,
        String sgguCdNm,
        String npayCd,
        String npayKorNm,
        String npayMdivCd,
        String npayMdivCdNm,
        String npaySdivCd,
        String npaySdivCdNm,
        String npayDtlDivCd,
        String npayDtlDivCdNm,
        Long minPrc,
        Long maxPrc,
        String adtFrDd,
        String adtEndDd,
        String urlAddr
) {
}
