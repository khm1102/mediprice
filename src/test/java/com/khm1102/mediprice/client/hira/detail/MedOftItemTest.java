package com.khm1102.mediprice.client.hira.detail;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getMedOftInfo2.7} 실제 응답 XML이 {@link MedOftItem}으로 올바르게 매핑되는지 검증.
 * <p>
 * 과거 버그: 필드명을 {@code medOftCd}/{@code medOftCdNm}/{@code medOftCnt}로 정의했으나
 * 실제 API는 {@code oftCd}/{@code oftCdNm}/{@code oftCnt} → 전 필드 null이 됐었음.
 */
class MedOftItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 건국대학교병원 응답에서 발췌. */
    private static final String SAMPLE_XML = """
            <response>
              <header>
                <resultCode>00</resultCode>
                <resultMsg>NORMAL SERVICE.</resultMsg>
              </header>
              <body>
                <items>
                  <item>
                    <oftCd>B101</oftCd>
                    <oftCdNm>일반엑스선촬영장치</oftCdNm>
                    <oftCnt>12</oftCnt>
                  </item>
                  <item>
                    <oftCd>B201</oftCd>
                    <oftCdNm>양전자단층촬영기 (PET)</oftCdNm>
                    <oftCnt>1</oftCnt>
                  </item>
                </items>
                <numOfRows>2</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>14</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<MedOftItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<MedOftItem>>() {});

        assertThat(response.body().safeItems()).hasSize(2);
        MedOftItem first = response.body().safeItems().get(0);
        assertThat(first.oftCd()).isEqualTo("B101");
        assertThat(first.oftCdNm()).isEqualTo("일반엑스선촬영장치");
        assertThat(first.oftCnt()).isEqualTo(12);
    }
}
