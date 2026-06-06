package com.khm1102.mediprice.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
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
 * {@link HiraNonPayClient}의 resultCode 분기 검증.
 * <p>
 * NORMAL("00"), NODATA("03"), FAILED("22"/HTTP 500/잘못된 XML)가 각각 다른 상태로 분류돼야 한다.
 * 옛 코드는 모두 빈 body로 평탄화해 배치가 실패를 NODATA로 오인했다.
 */
class HiraNonPayClientTest {

    private WireMockServer wireMock;
    private HiraNonPayClient client;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        XmlMapper xmlMapper = XmlMapper.builder().findAndAddModules().build();
        HiraServiceKeyProvider keyProvider = new HiraServiceKeyProvider("", "test-key");
        client = new HiraNonPayClient(keyProvider, wireMock.baseUrl(), xmlMapper);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    /** resultCode "00"이고 items가 있으면 NORMAL. */
    @Test
    void normalResultCodeProducesNormalStatus() {
        stubXml("/getNonPaymentItemHospDtlList", """
                <response><header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
                <body><items>
                  <item><ykiho>YK1</ykiho><npayCd>N001</npayCd><curAmt>10000</curAmt>
                        <adtFrDd>20240101</adtFrDd><adtEndDd>99991231</adtEndDd></item>
                </items><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>1</totalCount></body></response>
                """);

        HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail("YK1", 1, 10);

        assertThat(body.isNormal()).isTrue();
        assertThat(body.safeItems()).hasSize(1);
    }

    /** resultCode "03"이면 NODATA + 빈 items. */
    @Test
    void nodataResultCodeProducesNoDataStatus() {
        stubXml("/getNonPaymentItemHospDtlList", """
                <response><header><resultCode>03</resultCode><resultMsg>NODATA_ERROR</resultMsg></header>
                <body><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>0</totalCount></body></response>
                """);

        HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail("YK1", 1, 10);

        assertThat(body.isNoData()).isTrue();
        assertThat(body.safeItems()).isEmpty();
    }

    /**
     * HIRA가 NODATA에서 body 자체를 비워 보내는 경우 — header.resultCode=03만 보고 NODATA로 분류해야 한다.
     * 옛 classify는 body null을 먼저 보고 FAILED로 떨어뜨려 배치가 진짜 NODATA를 외부 호출 실패로 오인했다.
     */
    @Test
    void nodataResultCodeWithMissingBodyIsStillClassifiedAsNoData() {
        stubXml("/getNonPaymentItemHospDtlList", """
                <response><header><resultCode>03</resultCode><resultMsg>NODATA_ERROR</resultMsg></header></response>
                """);

        HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail("YK1", 1, 10);

        assertThat(body.isNoData()).isTrue();
        assertThat(body.safeItems()).isEmpty();
    }

    /** resultCode "22" (트래픽 초과)는 정상 응답이지만 FAILED로 분류. */
    @Test
    void trafficLimitResultCodeProducesFailedStatus() {
        stubXml("/getNonPaymentItemHospDtlList", """
                <response><header><resultCode>22</resultCode><resultMsg>LIMITED NUMBER OF SERVICE REQUESTS EXCEEDS ERROR.</resultMsg></header>
                <body></body></response>
                """);

        HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail("YK1", 1, 10);

        assertThat(body.isFailed()).isTrue();
    }

    /** XML 파싱 실패도 retry 후 FAILED 상태. */
    @Test
    void malformedXmlEventuallyProducesFailedStatus() {
        stubXml("/getNonPaymentItemHospDtlList", "<<<not-xml>>>");

        HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail("YK1", 1, 10);

        assertThat(body.isFailed()).isTrue();
        assertThat(body.getPageNo()).isEqualTo(1);
    }

    /** sidoCd 오버로드는 URI에 sidoCd 파라미터를 추가한다. */
    @Test
    void summaryOverloadAttachesSidoCd() {
        stubXml("/getNonPaymentItemHospList2", """
                <response><header><resultCode>00</resultCode></header>
                <body><items></items><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>0</totalCount></body></response>
                """);

        client.searchHospPriceSummary("110000", 1, 10);

        wireMock.verify(
                com.github.tomakehurst.wiremock.client.WireMock
                        .getRequestedFor(urlPathEqualTo("/getNonPaymentItemHospList2"))
                        .withQueryParam("sidoCd",
                                com.github.tomakehurst.wiremock.client.WireMock.equalTo("110000")));
    }

    private void stubXml(String path, String body) {
        wireMock.stubFor(get(urlPathEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody(body)));
    }
}
