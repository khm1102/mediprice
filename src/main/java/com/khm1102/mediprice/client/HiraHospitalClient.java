package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HiraResponse;
import com.khm1102.mediprice.client.hira.HospBasisItem;
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
 * 병원정보서비스 — {@code getHospBasisList1} 호출.
 * 실패 시 빈 body 반환 (예외 전파 금지 — 배치가 다음 시도/페이지로 진행).
 */
@Slf4j
@Component
public class HiraHospitalClient {

    private static final String BASE_URL = "https://apis.data.go.kr/B551182/hospInfoServicev2";

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final String serviceKey;

    public HiraHospitalClient(
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

    public HiraBody<HospBasisItem> searchHospitals(String sidoCd, int pageNo, int numOfRows) {
        try {
            byte[] xml = restClient.get()
                    .uri(b -> b.path("/getHospBasisList")
                            .queryParam("ServiceKey", serviceKey)
                            .queryParam("sidoCd", sidoCd)
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .build())
                    .retrieve()
                    .body(byte[].class);
            HiraResponse<HospBasisItem> response = xmlMapper.readValue(
                    xml, new TypeReference<HiraResponse<HospBasisItem>>() {});
            if (response.body() == null) {
                return emptyBody(pageNo);
            }
            return response.body();
        } catch (Exception e) {
            log.warn("getHospBasisList 실패 (sidoCd={}, pageNo={}): {}", sidoCd, pageNo, e.getMessage());
            return emptyBody(pageNo);
        }
    }

    private static HiraBody<HospBasisItem> emptyBody(int pageNo) {
        return HiraBody.empty(pageNo);
    }
}
