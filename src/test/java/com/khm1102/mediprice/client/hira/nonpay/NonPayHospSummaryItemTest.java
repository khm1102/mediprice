package com.khm1102.mediprice.client.hira.nonpay;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getNonPaymentItemHospList2} 응답 XML이 {@link NonPayHospSummaryItem}으로
 * 올바르게 매핑되는지 검증. min/max + 지역·종별·항목 메타 21개 필드 전부.
 */
class NonPayHospSummaryItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 응답에서 발췌 — 성빈센트병원 1인실. */
    private static final String SAMPLE_XML = """
            <response>
              <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
              <body>
                <items>
                  <item>
                    <adtEndDd>99991231</adtEndDd>
                    <adtFrDd>20260121</adtFrDd>
                    <clCd>01</clCd>
                    <clCdNm>상급종합</clCdNm>
                    <maxPrc>450000</maxPrc>
                    <minPrc>400000</minPrc>
                    <npayCd>ABZ010001</npayCd>
                    <npayDtlDivCd>1010A010</npayDtlDivCd>
                    <npayDtlDivCdNm>1인실</npayDtlDivCdNm>
                    <npayKorNm>상급병실료/1인실</npayKorNm>
                    <npayMdivCd>1010A</npayMdivCd>
                    <npayMdivCdNm>상급병실료</npayMdivCdNm>
                    <npaySdivCd>1010A010</npaySdivCd>
                    <npaySdivCdNm>1인실</npaySdivCdNm>
                    <sgguCd>310603</sgguCd>
                    <sgguCdNm>수원팔달구</sgguCdNm>
                    <sidoCd>310000</sidoCd>
                    <sidoCdNm>경기</sidoCdNm>
                    <urlAddr>https://example.com</urlAddr>
                    <yadmNm>가톨릭대학교 성빈센트병원</yadmNm>
                    <ykiho>YKIHO-SAMPLE</ykiho>
                  </item>
                </items>
                <numOfRows>1</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>188700</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<NonPayHospSummaryItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<NonPayHospSummaryItem>>() {});

        assertThat(response.body().safeItems()).hasSize(1);
        NonPayHospSummaryItem item = response.body().safeItems().get(0);
        assertThat(item.ykiho()).isEqualTo("YKIHO-SAMPLE");
        assertThat(item.yadmNm()).isEqualTo("가톨릭대학교 성빈센트병원");
        assertThat(item.clCd()).isEqualTo("01");
        assertThat(item.clCdNm()).isEqualTo("상급종합");
        assertThat(item.sidoCd()).isEqualTo("310000");
        assertThat(item.sidoCdNm()).isEqualTo("경기");
        assertThat(item.sgguCd()).isEqualTo("310603");
        assertThat(item.sgguCdNm()).isEqualTo("수원팔달구");
        assertThat(item.npayCd()).isEqualTo("ABZ010001");
        assertThat(item.npayKorNm()).isEqualTo("상급병실료/1인실");
        assertThat(item.npayMdivCd()).isEqualTo("1010A");
        assertThat(item.npayMdivCdNm()).isEqualTo("상급병실료");
        assertThat(item.npaySdivCd()).isEqualTo("1010A010");
        assertThat(item.npaySdivCdNm()).isEqualTo("1인실");
        assertThat(item.npayDtlDivCd()).isEqualTo("1010A010");
        assertThat(item.npayDtlDivCdNm()).isEqualTo("1인실");
        assertThat(item.minPrc()).isEqualTo(400_000L);
        assertThat(item.maxPrc()).isEqualTo(450_000L);
        assertThat(item.adtFrDd()).isEqualTo("20260121");
        assertThat(item.adtEndDd()).isEqualTo("99991231");
        assertThat(item.urlAddr()).isEqualTo("https://example.com");
    }
}
