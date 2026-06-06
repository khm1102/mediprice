package com.khm1102.mediprice.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDtlItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HiraApiHttpClientTest {

    private WireMockServer wireMock;
    private HiraApiHttpClient httpClient;
    private XmlMapper xmlMapper;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        httpClient = new HiraApiHttpClient(wireMock.baseUrl());
        xmlMapper = XmlMapper.builder().findAndAddModules().build();
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void getXmlPreservesEncodedServiceKeyCharactersAndParsesResponse() throws Exception {
        wireMock.stubFor(get(urlPathEqualTo("/getNonPaymentItemHospDtlList"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody("""
                                <response><header><resultCode>00</resultCode></header>
                                <body><items>
                                  <item><ykiho>YK1</ykiho><npayCd>N001</npayCd><curAmt>10000</curAmt></item>
                                </items><pageNo>1</pageNo><totalCount>1</totalCount></body></response>
                                """)));

        HiraResponse<NonPayDtlItem> response = httpClient.getXml(b -> b
                        .path("/getNonPaymentItemHospDtlList")
                        .queryParam("ServiceKey", "abc+/=def")
                        .queryParam("pageNo", 1),
                new TypeReference<HiraResponse<NonPayDtlItem>>() {}, xmlMapper);

        assertThat(response.header().isSuccess()).isTrue();
        assertThat(response.body().safeItems()).hasSize(1);
        assertThat(response.body().safeItems().get(0).npayCd()).isEqualTo("N001");
        wireMock.verify(getRequestedFor(urlPathEqualTo("/getNonPaymentItemHospDtlList"))
                .withQueryParam("ServiceKey", equalTo("abc+/=def"))
                .withQueryParam("pageNo", equalTo("1")));
    }

    @Test
    void getThrowsIOExceptionForNon2xxStatusBeforeXmlParsing() {
        wireMock.stubFor(get(urlPathEqualTo("/broken"))
                .willReturn(aResponse().withStatus(503).withBody("service unavailable")));

        assertThatThrownBy(() -> httpClient.get(b -> b.path("/broken")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("HIRA HTTP 503");
    }

    @Test
    void malformedXmlIsReportedByGetXmlCaller() {
        wireMock.stubFor(get(urlPathEqualTo("/malformed"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/xml; charset=UTF-8")
                        .withBody("<<<not-xml>>>")));

        assertThatThrownBy(() -> httpClient.getXml(
                b -> b.path("/malformed"),
                new TypeReference<HiraResponse<NonPayDtlItem>>() {},
                xmlMapper))
                .hasMessageContaining("Unexpected character");
    }
}
