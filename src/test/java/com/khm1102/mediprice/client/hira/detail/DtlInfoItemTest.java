package com.khm1102.mediprice.client.hira.detail;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getDtlInfo2.7} 실제 응답 XML이 {@link DtlInfoItem}으로 올바르게 매핑되는지 검증.
 * <p>
 * 한 병원에 단일 row가 반환된다. 응답에는 요일별 진료시간 등 다른 필드도 있지만 MVP는
 * 주차+접수시간+점심+휴진+응급만 사용.
 */
class DtlInfoItemTest {

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
                    <emyDayTelNo1>2001-2892</emyDayTelNo1>
                    <emyDayYn>Y</emyDayYn>
                    <emyNgtYn>Y</emyNgtYn>
                    <lunchWeek>12:30 ~ 13:30</lunchWeek>
                    <noTrmtHoli>휴진</noTrmtHoli>
                    <noTrmtSun>휴진</noTrmtSun>
                    <parkEtc>외래(검사)-당일 최대 8시간 / 입·퇴원,응급실- 입차시부터 24시간</parkEtc>
                    <parkQty>298</parkQty>
                    <parkXpnsYn>Y</parkXpnsYn>
                    <rcvSat>08:00 ~ 12:00</rcvSat>
                    <rcvWeek>08:00 ~ 17:00</rcvWeek>
                  </item>
                </items>
                <numOfRows>1</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>1</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<DtlInfoItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<DtlInfoItem>>() {});

        assertThat(response.body().safeItems()).hasSize(1);
        DtlInfoItem item = response.body().safeItems().get(0);
        assertThat(item.parkQty()).isEqualTo("298");
        assertThat(item.parkXpnsYn()).isEqualTo("Y");
        assertThat(item.parkEtc()).contains("외래");
        assertThat(item.rcvWeek()).isEqualTo("08:00 ~ 17:00");
        assertThat(item.rcvSat()).isEqualTo("08:00 ~ 12:00");
        assertThat(item.lunchWeek()).isEqualTo("12:30 ~ 13:30");
        assertThat(item.noTrmtSun()).isEqualTo("휴진");
        assertThat(item.noTrmtHoli()).isEqualTo("휴진");
        assertThat(item.emyDayYn()).isEqualTo("Y");
        assertThat(item.emyNgtYn()).isEqualTo("Y");
    }
}
