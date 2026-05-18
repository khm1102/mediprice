package com.khm1102.mediprice.client.hira;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getSpclDiagInfo2.7} 실제 응답 XML이 {@link SpclDiagItem}으로 올바르게 매핑되는지 검증.
 * <p>
 * 과거 버그: 필드명을 {@code srvTpCd}/{@code srvTpCdNm}로 정의했으나
 * 실제 API는 {@code srchCd}/{@code srchCdNm} → 전 필드 null이 됐었음.
 */
class SpclDiagItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 강북삼성병원 응답에서 발췌. */
    private static final String SAMPLE_XML = """
            <response>
              <header>
                <resultCode>00</resultCode>
                <resultMsg>NORMAL SERVICE.</resultMsg>
              </header>
              <body>
                <items>
                  <item>
                    <srchCd>TD</srchCd>
                    <srchCdNm>가정용 인공호흡기 환자 재택의료 시범기관</srchCdNm>
                  </item>
                  <item>
                    <srchCd>F2</srchCd>
                    <srchCdNm>경피적 대동맥판삽입 실시기관</srchCdNm>
                  </item>
                </items>
                <numOfRows>2</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>25</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<SpclDiagItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<SpclDiagItem>>() {});

        assertThat(response.body().safeItems()).hasSize(2);
        SpclDiagItem first = response.body().safeItems().get(0);
        assertThat(first.srchCd()).isEqualTo("TD");
        assertThat(first.srchCdNm()).isEqualTo("가정용 인공호흡기 환자 재택의료 시범기관");
    }
}
