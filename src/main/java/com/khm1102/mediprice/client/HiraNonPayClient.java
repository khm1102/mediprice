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
import java.util.List;

/**
 * 비급여진료비정보서비스 — 2개 오퍼레이션:
 * <ul>
 *   <li>{@code getNonPaymentItemCodeList2} — 항목 코드 (배치 Step 1)</li>
 *   <li>{@code getNonPaymentItemHospDtlList} — 병원별 가격 상세 (배치 Step 3 가격 저장)</li>
 * </ul>
 * <b>참고:</b> {@code getNonPaymentItemHospList2}(가격 요약)는 외부 API page 2부터 timeout 빈발로 폐기.
 * Hospital 테이블의 ykiho를 직접 사용해 가격상세를 호출한다 (PriceSyncService).
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

    private <T> HiraBody<T> invoke(
            String path,
            java.util.function.UnaryOperator<org.springframework.web.util.UriBuilder> queryAdder,
            TypeReference<HiraResponse<T>> typeRef,
            int pageNo) {
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
            log.warn("{} 실패 (pageNo={}): {}", path, pageNo, e.getMessage());
            return HiraBody.empty(pageNo);
        }
    }
}
