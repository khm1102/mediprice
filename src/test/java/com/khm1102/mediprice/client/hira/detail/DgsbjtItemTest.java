package com.khm1102.mediprice.client.hira.detail;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getDgsbjtInfo2.7} 실제 응답 XML이 {@link DgsbjtItem}으로 올바르게 매핑되는지 검증.
 * 회귀 방지용 — 심평원이 응답 필드명을 바꾸면 이 테스트가 깨진다.
 */
class DgsbjtItemTest {

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
                    <cdiagDrCnt>0</cdiagDrCnt>
                    <dgsbjtCd>01</dgsbjtCd>
                    <dgsbjtCdNm>내과</dgsbjtCdNm>
                    <dgsbjtPrSdrCnt>62</dgsbjtPrSdrCnt>
                  </item>
                  <item>
                    <cdiagDrCnt>0</cdiagDrCnt>
                    <dgsbjtCd>02</dgsbjtCd>
                    <dgsbjtCdNm>신경과</dgsbjtCdNm>
                    <dgsbjtPrSdrCnt>8</dgsbjtPrSdrCnt>
                  </item>
                </items>
                <numOfRows>2</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>29</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<DgsbjtItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<DgsbjtItem>>() {});

        assertThat(response.body().safeItems()).hasSize(2);
        DgsbjtItem first = response.body().safeItems().get(0);
        assertThat(first.dgsbjtCd()).isEqualTo("01");
        assertThat(first.dgsbjtCdNm()).isEqualTo("내과");
        assertThat(first.dgsbjtPrSdrCnt()).isEqualTo(62);
    }
}
