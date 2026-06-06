package com.khm1102.mediprice.client.hira.nonpay;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getNonPaymentItemCodeList} (구버전) 응답 XML이 {@link NonPayDescItem}으로
 * 올바르게 매핑되는지 검증. 일반인용 설명 텍스트가 핵심 데이터.
 */
class NonPayDescItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 응답에서 발췌 — 상급병실료차액 1인실. */
    private static final String SAMPLE_XML = """
            <response>
              <header>
                <resultCode>00</resultCode>
                <resultMsg>NORMAL SERVICE.</resultMsg>
              </header>
              <body>
                <items>
                  <item>
                    <divCd1>A</divCd1>
                    <divCd1Nm>상급병실료차액</divCd1Nm>
                    <divCd1Dsc>건강보험에서 정한 비용 외에 추가로 비용부담이 있는 병실입니다.</divCd1Dsc>
                    <divCd2>A1100</divCd2>
                    <divCd2Nm>1인실</divCd2Nm>
                    <divCd2Dsc>1개의 입원실에 1인 입원</divCd2Dsc>
                  </item>
                </items>
                <numOfRows>1</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>54</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<NonPayDescItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<NonPayDescItem>>() {});

        assertThat(response.body().safeItems()).hasSize(1);
        NonPayDescItem item = response.body().safeItems().get(0);
        assertThat(item.divCd1()).isEqualTo("A");
        assertThat(item.divCd1Nm()).isEqualTo("상급병실료차액");
        assertThat(item.divCd1Dsc()).contains("건강보험");
        assertThat(item.divCd2()).isEqualTo("A1100");
        assertThat(item.divCd2Nm()).isEqualTo("1인실");
        assertThat(item.divCd2Dsc()).contains("1개의 입원실");
        // 3차 분류는 본 row에 없음 → null
        assertThat(item.divCd3()).isNull();
    }
}
