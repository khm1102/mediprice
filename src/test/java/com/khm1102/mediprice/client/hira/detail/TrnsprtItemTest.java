package com.khm1102.mediprice.client.hira.detail;

import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import tools.jackson.dataformat.xml.XmlMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code getTrnsprtInfo2.7} 실제 응답 XML이 {@link TrnsprtItem}으로 올바르게 매핑되는지 검증.
 * <p>
 * 과거 버그: 모델이 주차 필드({@code parkYn} 등)를 가지고 있었으나 실제 API는 대중교통 정보
 * ({@code trafNm}/{@code lineNo}/{@code arivPlc}/{@code dir}/{@code dist})를 반환.
 * 주차 정보는 {@code getDtlInfo2.7} ({@link DtlInfoItem}) 소관.
 */
class TrnsprtItemTest {

    private final XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();

    /** 실제 강북삼성병원 응답에서 발췌 — 한 병원에 지하철+버스 복수 row. */
    private static final String SAMPLE_XML = """
            <response>
              <header>
                <resultCode>00</resultCode>
                <resultMsg>NORMAL SERVICE.</resultMsg>
              </header>
              <body>
                <items>
                  <item>
                    <arivPlc>서대문역</arivPlc>
                    <dir>4번 출구</dir>
                    <dist>도보 5분</dist>
                    <lineNo>5호선</lineNo>
                    <trafNm>지하철</trafNm>
                  </item>
                  <item>
                    <arivPlc>서대문사거리, 적십자병원 앞</arivPlc>
                    <dir>-</dir>
                    <dist>10m</dist>
                    <lineNo>101, 710, 470, 471, 704, 720, 601</lineNo>
                    <trafNm>시내버스</trafNm>
                  </item>
                </items>
                <numOfRows>2</numOfRows>
                <pageNo>1</pageNo>
                <totalCount>4</totalCount>
              </body>
            </response>
            """;

    @Test
    void deserializesActualHiraResponse() {
        HiraResponse<TrnsprtItem> response = xmlMapper.readValue(
                SAMPLE_XML, new TypeReference<HiraResponse<TrnsprtItem>>() {});

        assertThat(response.body().safeItems()).hasSize(2);
        TrnsprtItem subway = response.body().safeItems().get(0);
        assertThat(subway.trafNm()).isEqualTo("지하철");
        assertThat(subway.lineNo()).isEqualTo("5호선");
        assertThat(subway.arivPlc()).isEqualTo("서대문역");
        assertThat(subway.dir()).isEqualTo("4번 출구");
        assertThat(subway.dist()).isEqualTo("도보 5분");

        TrnsprtItem bus = response.body().safeItems().get(1);
        assertThat(bus.trafNm()).isEqualTo("시내버스");
    }
}
