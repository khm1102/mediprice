package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import com.khm1102.mediprice.client.hira.auth.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.stat.NonPayClcdStatItem;
import com.khm1102.mediprice.client.hira.nonpay.NonPayCodeItem;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDescItem;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDtlItem;
import com.khm1102.mediprice.client.hira.nonpay.NonPayHospSummaryItem;
import com.khm1102.mediprice.client.hira.stat.NonPaySidoStatItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
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

    private final HiraApiHttpClient httpClient;
    private final XmlMapper xmlMapper;
    private final HiraServiceKeyProvider keyProvider;

    public HiraNonPayClient(
            HiraServiceKeyProvider keyProvider,
            @Value("${hira.nonpay-base-url:https://apis.data.go.kr/B551182/nonPaymentDamtInfoService}") String baseUrl,
            XmlMapper hiraXmlMapper) {
        this.keyProvider = keyProvider;
        this.xmlMapper = hiraXmlMapper;
        this.httpClient = new HiraApiHttpClient(baseUrl);
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
        return searchHospPriceSummary(null, pageNo, numOfRows);
    }

    /**
     * 병원×항목 시기별 min/max 가격 요약 — 시도 필터 버전.
     * <p>
     * sidoCd가 null/blank면 전국 호출로 위임. quota 부담을 줄이려고 시도 분할로 호출하고 싶을 때 사용.
     */
    public HiraBody<NonPayHospSummaryItem> searchHospPriceSummary(
            @Nullable String sidoCd, int pageNo, int numOfRows) {
        return invoke("/getNonPaymentItemHospList2", b -> {
                    b.queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows);
                    if (sidoCd != null && !sidoCd.isBlank()) {
                        b.queryParam("sidoCd", sidoCd);
                    }
                    return b;
                },
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
                HiraResponse<T> response = httpClient.getXml(b -> queryAdder.apply(
                        b.path(path).queryParam("ServiceKey", serviceKey)
                ), typeRef, xmlMapper);
                return HiraResponseClassifier.classify(response, path, pageNo, "pageNo=" + pageNo, log);
            } catch (Exception e) {
                lastError = e;
                if (attempt < RETRY_BACKOFF_MS.length) {
                    try {
                        Thread.sleep(RETRY_BACKOFF_MS[attempt]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return HiraBody.failed(pageNo);
                    }
                }
            }
        }
        log.warn("{} 최종 실패 (pageNo={}): {}",
                path, pageNo, lastError == null ? "unknown" : lastError.getMessage());
        return HiraBody.failed(pageNo);
    }
}
