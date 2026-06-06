package com.khm1102.mediprice.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.HospBasisItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.dataformat.xml.XmlMapper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link HiraHospitalClient}의 resultCode 분기 검증.
 */
class HiraHospitalClientTest {

    private WireMockServer wireMock;
    private HiraHospitalClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();
        HiraServiceKeyProvider keyProvider = new HiraServiceKeyProvider("", "test-key");
        client = new HiraHospitalClient(keyProvider, wireMock.baseUrl(), xmlMapper);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void normalResponseProducesNormalStatus() {
        stubXml("/getHospBasisList", """
                <response><header><resultCode>00</resultCode></header>
                <body><items>
                  <item><ykiho>YK1</ykiho><yadmNm>강북삼성병원</yadmNm><clCd>11</clCd><clCdNm>상급종합</clCdNm>
                        <addr>서울 종로구</addr><telno>02-1</telno>
                        <XPos>126.97</XPos><YPos>37.59</YPos></item>
                </items><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>1</totalCount></body></response>
                """);

        HiraBody<HospBasisItem> body = client.searchHospitals("110000", 1, 10);

        assertThat(body.isNormal()).isTrue();
        assertThat(body.safeItems()).hasSize(1);
    }

    @Test
    void noDataResponseProducesNoDataStatus() {
        stubXml("/getHospBasisList", """
                <response><header><resultCode>03</resultCode><resultMsg>NODATA_ERROR</resultMsg></header>
                <body><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>0</totalCount></body></response>
                """);

        HiraBody<HospBasisItem> body = client.searchHospitals("110000", 1, 10);

        assertThat(body.isNoData()).isTrue();
        assertThat(body.safeItems()).isEmpty();
    }

    /** NODATA 응답에서 body 태그 자체가 빠진 경우에도 header만 보고 NODATA로 분류해야 한다. */
    @Test
    void noDataResponseWithoutBodyIsStillClassifiedAsNoData() {
        stubXml("/getHospBasisList", """
                <response><header><resultCode>03</resultCode><resultMsg>NODATA_ERROR</resultMsg></header></response>
                """);

        HiraBody<HospBasisItem> body = client.searchHospitals("110000", 1, 10);

        assertThat(body.isNoData()).isTrue();
    }

    @Test
    void trafficLimitResponseProducesFailedStatus() {
        stubXml("/getHospBasisList", """
                <response><header><resultCode>22</resultCode><resultMsg>LIMITED NUMBER OF SERVICE REQUESTS EXCEEDS ERROR.</resultMsg></header>
                <body></body></response>
                """);

        HiraBody<HospBasisItem> body = client.searchHospitals("110000", 1, 10);

        assertThat(body.isFailed()).isTrue();
    }

    @Test
    void malformedXmlProducesFailedStatusAfterRetries() {
        stubXml("/getHospBasisList", "<<<not-xml>>>");

        HiraBody<HospBasisItem> body = client.searchHospitals("110000", 2, 10);

        assertThat(body.isFailed()).isTrue();
        assertThat(body.getPageNo()).isEqualTo(2);
    }

    private void stubXml(String path, String body) {
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody(body)));
    }
}
