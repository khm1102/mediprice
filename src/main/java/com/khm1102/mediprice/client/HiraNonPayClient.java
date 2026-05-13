package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HiraResponse;
import com.khm1102.mediprice.client.hira.NonPayCodeItem;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.time.Duration;

/**
 * 비급여진료비정보서비스 — 2개 오퍼레이션:
 * <ul>
 *   <li>{@code getNonPaymentItemCodeList2} — 항목 코드 (배치 Step 1)</li>
 *   <li>{@code getNonPaymentItemHospDtlList} — 병원별 가격 상세 (배치 Step 3)</li>
 * </ul>
 */
@Slf4j
@Component
public class HiraNonPayClient {

    private static final String BASE_URL = "http://apis.data.go.kr/B551182/nonPaymentDamtInfoService";

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final String serviceKey;

    public HiraNonPayClient(
            @Value("${hira.api-key}") String serviceKey,
            XmlMapper hiraXmlMapper) {
        this.serviceKey = serviceKey;
        this.xmlMapper = hiraXmlMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(factory)
                .build();
    }

    public HiraBody<NonPayCodeItem> searchItemCodes(int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemCodeList2", b -> b
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows),
                new TypeReference<HiraResponse<NonPayCodeItem>>() {}, pageNo);
    }

    public HiraBody<NonPayDtlItem> searchHospPriceDetail(String ykiho, int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemHospDtlList", b -> b
                        .queryParam("ykiho", ykiho)
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows),
                new TypeReference<HiraResponse<NonPayDtlItem>>() {}, pageNo);
    }

    /** HIRA API는 산발적 timeout/connection reset 잦아 3회 exp backoff 재시도. */
    private static final long[] RETRY_BACKOFF_MS = {200L, 800L, 2000L};

    private <T> HiraBody<T> invoke(
            String path,
            java.util.function.UnaryOperator<org.springframework.web.util.UriBuilder> queryAdder,
            TypeReference<HiraResponse<T>> typeRef,
            int pageNo) {
        Exception lastError = null;
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MS.length; attempt++) {
            try {
                byte[] xml = restClient.get()
                        .uri(b -> queryAdder.apply(
                                b.path(path).queryParam("ServiceKey", serviceKey)
                        ).build())
                        .retrieve()
                        .body(byte[].class);
                HiraResponse<T> response = xmlMapper.readValue(xml, typeRef);
                if (response.body() == null) {
                    return HiraBody.empty(pageNo);
                }
                return response.body();
            } catch (Exception e) {
                lastError = e;
                if (attempt < RETRY_BACKOFF_MS.length) {
                    try {
                        Thread.sleep(RETRY_BACKOFF_MS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return HiraBody.empty(pageNo);
                    }
                }
            }
        }
        log.warn("{} 최종 실패 (pageNo={}): {}",
                path, pageNo, lastError == null ? "unknown" : lastError.getMessage());
        return HiraBody.empty(pageNo);
    }
}
