package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HiraHeader;
import com.khm1102.mediprice.client.hira.HiraResponse;
import com.khm1102.mediprice.client.hira.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.HospBasisItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.time.Duration;

/**
 * 병원정보서비스 — {@code getHospBasisList1} 호출.
 * <p>
 * 실패는 빈 body가 아니라 {@link HiraBody.Status#FAILED} 본문으로 반환한다.
 * 호출처(HospitalSyncService)가 NODATA와 FAILED를 구분해야 페이지 누락을 막을 수 있다.
 */
@Slf4j
@Component
public class HiraHospitalClient {

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final HiraServiceKeyProvider keyProvider;

    public HiraHospitalClient(
            HiraServiceKeyProvider keyProvider,
            @Value("${hira.hospital-base-url:https://apis.data.go.kr/B551182/hospInfoServicev2}") String baseUrl,
            XmlMapper hiraXmlMapper) {
        this.keyProvider = keyProvider;
        this.xmlMapper = hiraXmlMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(90));
        // URI_COMPONENT: base64 인증키의 +/= 문자가 query component에서 깨지지 않게 인코딩한다.
        DefaultUriBuilderFactory uriFactory = new DefaultUriBuilderFactory(baseUrl);
        uriFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.URI_COMPONENT);
        this.restClient = RestClient.builder()
                .uriBuilderFactory(uriFactory)
                .requestFactory(factory)
                .build();
    }

    /** HIRA API는 산발적 timeout/connection reset이 잦아 3회 exp backoff 재시도 (200ms, 800ms, 2000ms). */
    private static final long[] RETRY_BACKOFF_MS = {200L, 800L, 2000L};

    public HiraBody<HospBasisItem> searchHospitals(String sidoCd, int pageNo, int numOfRows) {
        return searchHospitals(sidoCd, null, pageNo, numOfRows);
    }

    public HiraBody<HospBasisItem> searchHospitals(String sidoCd, String sgguCd, int pageNo, int numOfRows) {
        Exception lastError = null;
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MS.length; attempt++) {
            String serviceKey = keyProvider.next();
            try {
                byte[] xml = restClient.get()
                        .uri(b -> {
                            b.path("/getHospBasisList")
                                    .queryParam("ServiceKey", serviceKey)
                                    .queryParam("sidoCd", sidoCd)
                                    .queryParam("pageNo", pageNo)
                                    .queryParam("numOfRows", numOfRows);
                            if (sgguCd != null && !sgguCd.isBlank()) {
                                b.queryParam("sgguCd", sgguCd);
                            }
                            return b.build();
                        })
                        .retrieve()
                        .body(byte[].class);
                HiraResponse<HospBasisItem> response = xmlMapper.readValue(
                        xml, new TypeReference<HiraResponse<HospBasisItem>>() {});
                return classify(response, sidoCd, pageNo);
            } catch (Exception e) {
                lastError = e;
                if (attempt < RETRY_BACKOFF_MS.length) {
                    log.warn("getHospBasisList 일시 실패 (sidoCd={}, pageNo={}, attempt={}/{}): {} — backoff {}ms",
                            sidoCd, pageNo, attempt + 1, RETRY_BACKOFF_MS.length + 1,
                            e.getMessage(), RETRY_BACKOFF_MS[attempt]);
                    try {
                        Thread.sleep(RETRY_BACKOFF_MS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return HiraBody.failed(pageNo);
                    }
                }
            }
        }
        log.warn("getHospBasisList 최종 실패 (sidoCd={}, pageNo={}): {}",
                sidoCd, pageNo, lastError == null ? "unknown" : lastError.getMessage());
        return HiraBody.failed(pageNo);
    }

    /**
     * resultCode 기반으로 NORMAL/NODATA/FAILED를 구분. 실패를 빈 body로 평탄화하지 않는다.
     * <p>
     * header를 body null보다 먼저 본다. HIRA가 NODATA(resultCode=03)에서 body 자체를 비워 보내는 케이스가 있어,
     * body null 단계에서 FAILED로 떨어지면 진짜 NODATA를 실패로 오인하게 된다.
     */
    private HiraBody<HospBasisItem> classify(HiraResponse<HospBasisItem> response, String sidoCd, int pageNo) {
        if (response == null) {
            log.warn("getHospBasisList response null (sidoCd={}, pageNo={})", sidoCd, pageNo);
            return HiraBody.failed(pageNo);
        }
        HiraHeader header = response.header();
        if (header != null && header.isNoData()) {
            return HiraBody.noData(pageNo);
        }
        if (response.body() == null) {
            log.warn("getHospBasisList body null (sidoCd={}, pageNo={}, resultCode={})",
                    sidoCd, pageNo, header == null ? "null" : header.resultCode());
            return HiraBody.failed(pageNo);
        }
        if (header == null || header.isSuccess()) {
            response.body().setStatus(HiraBody.Status.NORMAL);
            return response.body();
        }
        log.warn("getHospBasisList resultCode={} resultMsg={} (sidoCd={}, pageNo={})",
                header.resultCode(), header.resultMsg(), sidoCd, pageNo);
        return HiraBody.failed(pageNo);
    }
}
