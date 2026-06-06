package com.khm1102.mediprice.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import com.khm1102.mediprice.client.hira.auth.HiraServiceKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.concurrent.Executor;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HiraDetailClient}의 5개 API 병렬 호출을 WireMock으로 stub해서 통합 검증.
 * <p>
 * 검증 포인트:
 * <ul>
 *   <li>5개 API 응답이 올바르게 {@link HospitalDetailBundle}에 매핑되는가</li>
 *   <li>일부 API 실패 시 해당 결과만 비고 나머지는 정상 반환되는가 (실패 격리)</li>
 * </ul>
 */
class HiraDetailClientTest {

    private WireMockServer wireMock;
    private HiraDetailClient client;
    private Executor executor;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();

        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(5);
        pool.setMaxPoolSize(10);
        pool.setQueueCapacity(50);
        pool.setThreadNamePrefix("test-hira-");
        pool.initialize();
        executor = pool;

        XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();
        HiraServiceKeyProvider keyProvider = new HiraServiceKeyProvider("", "test-key");
        client = new HiraDetailClient(keyProvider, wireMock.baseUrl(), xmlMapper, executor);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void fetchAllMapsAllFiveApisIntoBundle() {
        stubXml("/getDgsbjtInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><dgsbjtCd>01</dgsbjtCd><dgsbjtCdNm>내과</dgsbjtCdNm><dgsbjtPrSdrCnt>62</dgsbjtPrSdrCnt></item>
                </items></body></response>
                """);
        stubXml("/getMedOftInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><oftCd>B301</oftCd><oftCdNm>MRI</oftCdNm><oftCnt>3</oftCnt></item>
                </items></body></response>
                """);
        stubXml("/getTrnsprtInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><trafNm>지하철</trafNm><lineNo>5호선</lineNo><arivPlc>서대문역</arivPlc><dir>4번 출구</dir><dist>도보 5분</dist></item>
                </items></body></response>
                """);
        stubXml("/getDtlInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><parkQty>298</parkQty><parkXpnsYn>Y</parkXpnsYn><parkEtc>당일 최대 8시간</parkEtc>
                        <rcvWeek>08:00 ~ 17:00</rcvWeek><rcvSat>08:00 ~ 12:00</rcvSat>
                        <lunchWeek>12:30 ~ 13:30</lunchWeek>
                        <noTrmtSun>휴진</noTrmtSun><noTrmtHoli>휴진</noTrmtHoli>
                        <emyDayYn>Y</emyDayYn><emyNgtYn>Y</emyNgtYn></item>
                </items></body></response>
                """);
        stubXml("/getSpclDiagInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><srchCd>TD</srchCd><srchCdNm>응급의료센터</srchCdNm></item>
                </items></body></response>
                """);

        HospitalDetailBundle bundle = client.fetchAll("YKIHO-1");

        assertThat(bundle.dgsbjtList()).hasSize(1);
        assertThat(bundle.dgsbjtList().get(0).dgsbjtCdNm()).isEqualTo("내과");

        assertThat(bundle.medOftList()).hasSize(1);
        assertThat(bundle.medOftList().get(0).oftCdNm()).isEqualTo("MRI");

        assertThat(bundle.trnsprtList()).hasSize(1);
        assertThat(bundle.trnsprtList().get(0).trafNm()).isEqualTo("지하철");

        assertThat(bundle.dtlInfo()).isPresent();
        assertThat(bundle.dtlInfo().get().parkQty()).isEqualTo("298");
        assertThat(bundle.dtlInfo().get().emyDayYn()).isEqualTo("Y");

        assertThat(bundle.spclDiagList()).hasSize(1);
        assertThat(bundle.spclDiagList().get(0).srchCdNm()).isEqualTo("응급의료센터");
        wireMock.verify(getRequestedFor(urlPathEqualTo("/getDgsbjtInfo2.7"))
                .withQueryParam("ServiceKey", equalTo("test-key"))
                .withQueryParam("ykiho", equalTo("YKIHO-1"))
                .withQueryParam("pageNo", equalTo("1"))
                .withQueryParam("numOfRows", equalTo("100")));
    }

    @Test
    void oneApiFailureDoesNotBlockOtherResults() {
        // dgsbjt만 500 에러, 나머지는 정상 응답
        wireMock.stubFor(get(urlPathEqualTo("/getDgsbjtInfo2.7"))
                .willReturn(aResponse().withStatus(500)));
        stubXml("/getMedOftInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><oftCd>B301</oftCd><oftCdNm>MRI</oftCdNm><oftCnt>3</oftCnt></item>
                </items></body></response>
                """);
        stubXml("/getTrnsprtInfo2.7", emptyBody());
        stubXml("/getDtlInfo2.7", emptyBody());
        stubXml("/getSpclDiagInfo2.7", emptyBody());

        HospitalDetailBundle bundle = client.fetchAll("YKIHO-1");

        // 실패한 dgsbjt는 빈 리스트
        assertThat(bundle.dgsbjtList()).isEmpty();
        // 다른 API는 정상 반환
        assertThat(bundle.medOftList()).hasSize(1);
        assertThat(bundle.trnsprtList()).isEmpty();
        assertThat(bundle.dtlInfo()).isEmpty();
        assertThat(bundle.spclDiagList()).isEmpty();
    }

    @Test
    void missingBodyInOneApiFallsBackToOnlyThatSection() {
        wireMock.stubFor(get(urlPathEqualTo("/getDgsbjtInfo2.7"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody("<response><header><resultCode>03</resultCode></header></response>")));
        stubXml("/getMedOftInfo2.7", """
                <response><header><resultCode>00</resultCode></header><body><items>
                  <item><oftCd>B301</oftCd><oftCdNm>MRI</oftCdNm><oftCnt>3</oftCnt></item>
                </items></body></response>
                """);
        stubXml("/getTrnsprtInfo2.7", emptyBody());
        stubXml("/getDtlInfo2.7", emptyBody());
        stubXml("/getSpclDiagInfo2.7", emptyBody());

        HospitalDetailBundle bundle = client.fetchAll("YKIHO-1");

        assertThat(bundle.dgsbjtList()).isEmpty();
        assertThat(bundle.medOftList()).hasSize(1);
        assertThat(bundle.medOftList().get(0).oftCdNm()).isEqualTo("MRI");
        assertThat(bundle.trnsprtList()).isEmpty();
        assertThat(bundle.dtlInfo()).isEmpty();
        assertThat(bundle.spclDiagList()).isEmpty();
    }

    private void stubXml(String path, String body) {
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody(body)));
    }

    private static String emptyBody() {
        return """
                <response><header><resultCode>00</resultCode></header><body>
                  <items></items><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>0</totalCount>
                </body></response>
                """;
    }
}
