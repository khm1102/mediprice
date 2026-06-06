package com.khm1102.mediprice.client.hira.nonpay;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 비급여진료비정보서비스 구버전 {@code getNonPaymentItemCodeList} 응답 item.
 * <p>
 * 신버전과 달리 1·2·3차 분류 코드와 함께 일반인용 설명 텍스트({@code *Dsc})를 제공한다.
 * 신버전 코드 체계({@code npayCd}/{@code npayMdivCd})와는 매핑이 다르므로 raw 그대로 저장.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NonPayDescItem(
        String divCd1,
        String divCd1Nm,
        String divCd1Dsc,
        String divCd2,
        String divCd2Nm,
        String divCd2Dsc,
        String divCd3,
        String divCd3Nm,
        String divCd3Dsc
) {
}
