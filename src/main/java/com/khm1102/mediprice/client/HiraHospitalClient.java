package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import com.khm1102.mediprice.client.hira.auth.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.hospital.HospBasisItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

/**
 * 병원정보서비스 — {@code getHospBasisList} 호출.
 * <p>
 * 실패는 빈 body가 아니라 {@link HiraBody.Status#FAILED} 본문으로 반환한다.
 * 호출처(HospitalSyncService)가 NODATA와 FAILED를 구분해야 페이지 누락을 막을 수 있다.
 */
@Slf4j
@Component
public class HiraHospitalClient {

    private final HiraApiHttpClient httpClient;
    private final XmlMapper xmlMapper;
    private final HiraServiceKeyProvider keyProvider;

    public HiraHospitalClient(
            HiraServiceKeyProvider keyProvider,
            @Value("${hira.hospital-base-url:https://apis.data.go.kr/B551182/hospInfoServicev2}") String baseUrl,
            XmlMapper hiraXmlMapper) {
        this.keyProvider = keyProvider;
        this.xmlMapper = hiraXmlMapper;
        this.httpClient = new HiraApiHttpClient(baseUrl);
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
                HiraResponse<HospBasisItem> response = httpClient.getXml(b -> {
                            b.path("/getHospBasisList")
                                    .queryParam("ServiceKey", serviceKey)
                                    .queryParam("sidoCd", sidoCd)
                                    .queryParam("pageNo", pageNo)
                                    .queryParam("numOfRows", numOfRows);
                            if (sgguCd != null && !sgguCd.isBlank()) {
                                b.queryParam("sgguCd", sgguCd);
                            }
                            return b;
                        },
                        new TypeReference<HiraResponse<HospBasisItem>>() {}, xmlMapper);
                return HiraResponseClassifier.classify(
                        response, "getHospBasisList", pageNo, "sidoCd=" + sidoCd + ", pageNo=" + pageNo, log);
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
}
