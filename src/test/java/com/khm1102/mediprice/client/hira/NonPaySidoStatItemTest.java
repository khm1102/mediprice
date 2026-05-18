package com.khm1102.mediprice.client.hira;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getNonPaymentItemSidoCdList} 응답 XML 매핑 + wide → long 변환 검증.
 */
class NonPaySidoStatItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 응답에서 발췌 — 모발이식 (8개 시도만 통계 있음). */
    private static final String SAMPLE_XML = """
            <response>
              <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
              <body>
                <items>
                  <item>
                    <middAvgAll>1000000</middAvgAll>
                    <middAvgCcn>2000000</middAvgCcn>
                    <middAvgDj>550000</middAvgDj>
                    <middAvgIch>3483180</middAvgIch>
                    <middAvgKaw>200000</middAvgKaw>
                    <middAvgKyg>1051250</middAvgKyg>
                    <middAvgSj>539600</middAvgSj>
                    <middAvgSl>727250</middAvgSl>
                    <middAvgTg>4000000</middAvgTg>
                    <npayCd>1Z9230001</npayCd>
                    <npayKorNm>모발이식술료/모발이식술/500모미만</npayKorNm>
                    <prcAvgAll>1402099</prcAvgAll>
                    <prcAvgCcn>2000000</prcAvgCcn>
                    <prcAvgDj>550000</prcAvgDj>
                    <prcAvgIch>3483180</prcAvgIch>
                    <prcAvgKaw>200000</prcAvgKaw>
                    <prcAvgKyg>1496750</prcAvgKyg>
                    <prcAvgSj>539600</prcAvgSj>
                    <prcAvgSl>914083</prcAvgSl>
                    <prcAvgTg>4000000</prcAvgTg>
                    <prcMaxAll>4000000</prcMaxAll>
                    <prcMaxSl>2280000</prcMaxSl>
                    <prcMinAll>200000</prcMinAll>
                    <prcMinSl>200000</prcMinSl>
                    <stdDate>20260517</stdDate>
                  </item>
                </items>
                <numOfRows>1</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>655</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<NonPaySidoStatItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<NonPaySidoStatItem>>() {});

        assertThat(response.body().safeItems()).hasSize(1);
        NonPaySidoStatItem item = response.body().safeItems().get(0);
        assertThat(item.npayCd()).isEqualTo("1Z9230001");
        assertThat(item.prcAvgSl()).isEqualTo(914_083L);
        assertThat(item.middAvgSl()).isEqualTo(727_250L);
        assertThat(item.prcAvgKyg()).isEqualTo(1_496_750L);
        assertThat(item.prcAvgTg()).isEqualTo(4_000_000L);
        // 통계 없는 시도 (부산, 광주, 울산 등)는 null
        assertThat(item.prcAvgBs()).isNull();
        assertThat(item.prcAvgUsn()).isNull();
        assertThat(item.prcAvgJj()).isNull();
    }

    /** 통계 있는 시도만 entry로 반환되고 나머지는 skip. */
    @Test
    void asStatBySidoOnlyIncludesPopulatedRegions() {
        HiraResponse<NonPaySidoStatItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<NonPaySidoStatItem>>() {});

        Map<String, StatValues> stats = response.body().safeItems().get(0).asStatBySido();

        // 8개 시도 + All = 9개 entry
        assertThat(stats.keySet()).containsExactlyInAnyOrder(
                "All", "Sl", "Tg", "Ich", "Dj", "Sj", "Kyg", "Kaw", "Ccn");
        // 누락 시도는 entry 없음
        assertThat(stats).doesNotContainKeys("Bs", "Kw", "Usn", "Cb", "Jb", "Jn", "Ksb", "Ksn", "Jj");

        StatValues sl = stats.get("Sl");
        assertThat(sl.avg()).isEqualTo(914_083L);
        assertThat(sl.mid()).isEqualTo(727_250L);
        assertThat(sl.min()).isEqualTo(200_000L);
        assertThat(sl.max()).isEqualTo(2_280_000L);
    }
}
