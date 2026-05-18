package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.HiraResponse;
import com.khm1102.mediprice.client.hira.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.NonPayClcdStatItem;
import com.khm1102.mediprice.client.hira.NonPayCodeItem;
import com.khm1102.mediprice.client.hira.NonPayDescItem;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
import com.khm1102.mediprice.client.hira.NonPayHospSummaryItem;
import com.khm1102.mediprice.client.hira.NonPaySidoStatItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.time.Duration;
import java.util.function.UnaryOperator;

import org.springframework.web.util.UriBuilder;

/**
 * 비급여진료비정보서비스 — 6개 오퍼레이션:
 * <ul>
 *   <li>{@code getNonPaymentItemCodeList2} — 항목 코드 (배치 Step 1)</li>
 *   <li>{@code getNonPaymentItemHospDtlList} — 병원별 가격 상세 (배치 Step 3)</li>
 *   <li>{@code getNonPaymentItemCodeList} (구버전) — 항목 설명 텍스트 (Step 4)</li>
 *   <li>{@code getNonPaymentItemHospList2} — 병원×항목 시기별 min/max 요약 (Step 5)</li>
 *   <li>{@code getNonPaymentItemClcdList} — 종별 통계 (Step 6)</li>
 *   <li>{@code getNonPaymentItemSidoCdList} — 지역별 통계 (Step 7)</li>
 * </ul>
 */
@Slf4j
@Component
public class HiraNonPayClient {

    private final RestClient restClient;
    private final XmlMapper xmlMapper;
    private final HiraServiceKeyProvider keyProvider;

    public HiraNonPayClient(
            HiraServiceKeyProvider keyProvider,
            @Value("${hira.nonpay-base-url:http://apis.data.go.kr/B551182/nonPaymentDamtInfoService}") String baseUrl,
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

    /** 구버전 항목 코드 — 일반인용 설명 텍스트 제공. */
    public HiraBody<NonPayDescItem> searchItemDescList(int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemCodeList", b -> b
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows),
                new TypeReference<HiraResponse<NonPayDescItem>>() {}, pageNo);
    }

    /** 병원×항목 시기별 min/max 가격 요약. */
    public HiraBody<NonPayHospSummaryItem> searchHospPriceSummary(int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemHospList2", b -> b
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows),
                new TypeReference<HiraResponse<NonPayHospSummaryItem>>() {}, pageNo);
    }

    /** 종별 통계 — clCd 파라미터 없이 전체 호출 (한 row가 모든 종별 wide 컬럼 포함). */
    public HiraBody<NonPayClcdStatItem> searchClcdStat(int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemClcdList", b -> b
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows),
                new TypeReference<HiraResponse<NonPayClcdStatItem>>() {}, pageNo);
    }

    /** 지역별 통계 — sidoCd 파라미터 없이 전체 호출 (한 row가 모든 시도 wide 컬럼 포함). */
    public HiraBody<NonPaySidoStatItem> searchSidoStat(int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemSidoCdList", b -> b
                        .queryParam("pageNo", pageNo)
                        .queryParam("numOfRows", numOfRows),
                new TypeReference<HiraResponse<NonPaySidoStatItem>>() {}, pageNo);
    }

    /** HIRA API는 산발적 timeout/connection reset 잦아 3회 exp backoff 재시도. */
    private static final long[] RETRY_BACKOFF_MS = {200L, 800L, 2000L};

    private <T> HiraBody<T> invoke(
            String path,
            UnaryOperator<UriBuilder> queryAdder,
            TypeReference<HiraResponse<T>> typeRef,
            int pageNo) {
        Exception lastError = null;
        for (int attempt = 0; attempt <= RETRY_BACKOFF_MS.length; attempt++) {
            String serviceKey = keyProvider.next();
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
