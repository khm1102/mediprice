package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * 병원정보서비스 {@code getHospBasisList1} 응답 item.
 * <p>
 * 응답 필드 중 MVP에서 저장할 항목만 선언. {@code XPos}/{@code YPos}는 대문자 시작이라 명시 매핑 필요.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HospBasisItem(
        String ykiho,
        String yadmNm,
        String clCd,
        String clCdNm,
        String addr,
        String telno,
        String hospUrl,
        Integer drTotCnt,
        String sidoCdNm,
        String sgguCdNm,
        @JacksonXmlProperty(localName = "XPos") Double xPos,
        @JacksonXmlProperty(localName = "YPos") Double yPos
) {
}
