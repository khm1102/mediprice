package com.khm1102.mediprice.client.hira;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getNonPaymentItemClcdList} 응답 XML이 {@link NonPayClcdStatItem}으로 매핑 +
 * wide → long 변환({@link NonPayClcdStatItem#asStatByClcd()}) 검증.
 */
class NonPayClcdStatItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 응답에서 발췌 — 모발이식. */
    private static final String SAMPLE_XML = """
            <response>
              <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
              <body>
                <items>
                  <item>
                    <middAvgAll>1000000</middAvgAll>
                    <middAvgGnhp>642000</middAvgGnhp>
                    <middAvgHosp>1906250</middAvgHosp>
                    <middAvgUsgh>2000000</middAvgUsgh>
                    <npayCd>1Z9230001</npayCd>
                    <npayKorNm>모발이식술료/모발이식술/500모미만</npayKorNm>
                    <prcAvgAll>1402099</prcAvgAll>
                    <prcAvgGnhp>790233</prcAvgGnhp>
                    <prcAvgHosp>1906250</prcAvgHosp>
                    <prcAvgUsgh>2044740</prcAvgUsgh>
                    <prcMaxAll>4000000</prcMaxAll>
                    <prcMaxGnhp>2205000</prcMaxGnhp>
                    <prcMaxHosp>3000000</prcMaxHosp>
                    <prcMaxUsgh>4000000</prcMaxUsgh>
                    <prcMinAll>200000</prcMinAll>
                    <prcMinGnhp>200000</prcMinGnhp>
                    <prcMinHosp>812500</prcMinHosp>
                    <prcMinUsgh>400000</prcMinUsgh>
                    <stdDate>20260517</stdDate>
                  </item>
                </items>
                <numOfRows>1</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>875</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<NonPayClcdStatItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<NonPayClcdStatItem>>() {});

        assertThat(response.body().safeItems()).hasSize(1);
        NonPayClcdStatItem item = response.body().safeItems().get(0);
        assertThat(item.npayCd()).isEqualTo("1Z9230001");
        assertThat(item.stdDate()).isEqualTo("20260517");
        assertThat(item.prcAvgAll()).isEqualTo(1_402_099L);
        assertThat(item.prcMinUsgh()).isEqualTo(400_000L);
        assertThat(item.prcMaxGnhp()).isEqualTo(2_205_000L);
    }

    @Test
    void asStatByClcdExpandsAllFourTypes() {
        HiraResponse<NonPayClcdStatItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<NonPayClcdStatItem>>() {});

        Map<String, StatValues> stats = response.body().safeItems().get(0).asStatByClcd();
        assertThat(stats).hasSize(4);
        assertThat(stats.keySet()).containsExactly("All", "Usgh", "Hosp", "Gnhp");

        StatValues all = stats.get("All");
        assertThat(all.avg()).isEqualTo(1_402_099L);
        assertThat(all.mid()).isEqualTo(1_000_000L);
        assertThat(all.min()).isEqualTo(200_000L);
        assertThat(all.max()).isEqualTo(4_000_000L);
    }

    /** 4통계 모두 null인 종별은 entry 생략. */
    @Test
    void skipsClcdEntryWhenAllStatsNull() {
        NonPayClcdStatItem item = new NonPayClcdStatItem(
                "X", "테스트", "20260517",
                100L, 100L, 100L, 100L,    // All 채움
                null, null, null, null,     // Usgh null
                200L, null, null, null,     // Hosp avg만
                null, null, null, null      // Gnhp null
        );

        Map<String, StatValues> stats = item.asStatByClcd();
        assertThat(stats.keySet()).containsExactly("All", "Hosp");
        assertThat(stats.get("Hosp").avg()).isEqualTo(200L);
        assertThat(stats.get("Hosp").min()).isNull();
    }
}
