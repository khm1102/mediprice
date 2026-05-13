package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 비급여진료비정보서비스 {@code getNonPaymentItemHospDtlList} 응답 item.
 * <p>
 * 실제 현재금액({@code curAmt})과 적용기간을 담는다. {@code adtEndDd = "99991231"}만 저장한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NonPayDtlItem(
        String ykiho,
        String npayCd,
        String npayKorNm,
        Long curAmt,
        String adtFrDd,
        String adtEndDd
) {
    public static final String ACTIVE_END_DATE = "99991231";

    public boolean isActive() {
        return ACTIVE_END_DATE.equals(adtEndDd);
    }
}
