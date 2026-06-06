package com.khm1102.mediprice.client;

import com.khm1102.mediprice.client.hira.detail.DgsbjtItem;
import com.khm1102.mediprice.client.hira.detail.DtlInfoItem;
import com.khm1102.mediprice.client.hira.common.HiraResponse;
import com.khm1102.mediprice.client.hira.auth.HiraServiceKeyProvider;
import com.khm1102.mediprice.client.hira.detail.MedOftItem;
import com.khm1102.mediprice.client.hira.detail.SpclDiagItem;
import com.khm1102.mediprice.client.hira.detail.TrnsprtItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.dataformat.xml.XmlMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 의료기관별상세정보서비스 — 5개 API. 병원 상세 화면용 실시간 호출.
 * <p>
 * 5개 호출은 {@link #fetchAllAsync(String)}로 병렬 실행({@code hiraDetailExecutor} pool).
 * 개별 호출 실패는 빈 결과로 fallback — 한 API가 죽어도 다른 API 결과는 반환.
 */
@Slf4j
@Component
public class HiraDetailClient {

    private final HiraApiHttpClient httpClient;
    private final XmlMapper xmlMapper;
    private final HiraServiceKeyProvider keyProvider;
    private final Executor executor;

    public HiraDetailClient(
            HiraServiceKeyProvider keyProvider,
            @Value("${hira.detail-base-url:https://apis.data.go.kr/B551182/MadmDtlInfoService2.7}") String baseUrl,
            XmlMapper hiraXmlMapper,
            @Qualifier("hiraDetailExecutor") Executor hiraDetailExecutor) {
        this.keyProvider = keyProvider;
        this.xmlMapper = hiraXmlMapper;
        this.executor = hiraDetailExecutor;
        this.httpClient = new HiraApiHttpClient(baseUrl);
    }

    public record HospitalDetailBundle(
            List<DgsbjtItem> dgsbjtList,
            List<MedOftItem> medOftList,
            List<TrnsprtItem> trnsprtList,
            Optional<DtlInfoItem> dtlInfo,
            List<SpclDiagItem> spclDiagList) {
    }

    public HospitalDetailBundle fetchAll(String ykiho) {
        return fetchAllAsync(ykiho).join();
    }

    public CompletableFuture<HospitalDetailBundle> fetchAllAsync(String ykiho) {
        CompletableFuture<List<DgsbjtItem>> dgsbjtFuture =
                CompletableFuture.supplyAsync(() -> fetchDgsbjt(ykiho), executor);
        CompletableFuture<List<MedOftItem>> medOftFuture =
                CompletableFuture.supplyAsync(() -> fetchMedOft(ykiho), executor);
        CompletableFuture<List<TrnsprtItem>> trnsprtFuture =
                CompletableFuture.supplyAsync(() -> fetchTrnsprt(ykiho), executor);
        CompletableFuture<Optional<DtlInfoItem>> dtlInfoFuture =
                CompletableFuture.supplyAsync(() -> fetchDtlInfo(ykiho), executor);
        CompletableFuture<List<SpclDiagItem>> spclDiagFuture =
                CompletableFuture.supplyAsync(() -> fetchSpclDiag(ykiho), executor);

        return CompletableFuture.allOf(dgsbjtFuture, medOftFuture, trnsprtFuture, dtlInfoFuture, spclDiagFuture)
                .thenApply(v -> new HospitalDetailBundle(
                        dgsbjtFuture.join(),
                        medOftFuture.join(),
                        trnsprtFuture.join(),
                        dtlInfoFuture.join(),
                        spclDiagFuture.join()));
    }

    private List<DgsbjtItem> fetchDgsbjt(String ykiho) {
        return invokeList("/getDgsbjtInfo2.7", ykiho,
                new TypeReference<HiraResponse<DgsbjtItem>>() {});
    }

    private List<MedOftItem> fetchMedOft(String ykiho) {
        return invokeList("/getMedOftInfo2.7", ykiho,
                new TypeReference<HiraResponse<MedOftItem>>() {});
    }

    private List<TrnsprtItem> fetchTrnsprt(String ykiho) {
        return invokeList("/getTrnsprtInfo2.7", ykiho,
                new TypeReference<HiraResponse<TrnsprtItem>>() {});
    }

    private Optional<DtlInfoItem> fetchDtlInfo(String ykiho) {
        List<DtlInfoItem> items = invokeList("/getDtlInfo2.7", ykiho,
                new TypeReference<HiraResponse<DtlInfoItem>>() {});
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }

    private List<SpclDiagItem> fetchSpclDiag(String ykiho) {
        return invokeList("/getSpclDiagInfo2.7", ykiho,
                new TypeReference<HiraResponse<SpclDiagItem>>() {});
    }

    private <T> List<T> invokeList(String path, String ykiho, TypeReference<HiraResponse<T>> typeRef) {
        String serviceKey = keyProvider.next();
        try {
            HiraResponse<T> response = httpClient.getXml(b -> b.path(path)
                    .queryParam("ServiceKey", serviceKey)
                    .queryParam("ykiho", ykiho)
                    .queryParam("pageNo", 1)
                    .queryParam("numOfRows", 100), typeRef, xmlMapper);
            if (response.body() == null) {
                return List.of();
            }
            return response.body().safeItems();
        } catch (Exception e) {
            log.warn("{} 실패 (ykiho={}): {}", path, ykiho, e.getMessage());
            return List.of();
        }
    }
}
