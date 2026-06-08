package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.common.HiraBody;
import com.khm1102.mediprice.client.hira.nonpay.NonPayDtlItem;
import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.entity.PriceId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * ykiho 1개에 대한 가격상세 동기화 — 트랜잭션 단위.
 * <p>
 * {@link PriceSyncService}가 다수 ykiho를 순회하면서 ykiho마다 본 메서드를 호출 →
 * 각 호출이 독립 트랜잭션이라 DB connection을 길게 점유하지 않는다.
 * <p>
 * 페이지 응답 상태(정상/NODATA/실패)를 구분해 중간 페이지 누락을 NODATA로 오인하지 않도록 한다.
 * <ul>
 *   <li>NORMAL(저장된 active 코드 ≥ 1): 본 응답의 활성 코드 집합 기준
 *       {@link PriceYkihoCleanupService#removeStaleByYkiho}로 정리.
 *       정리 실패는 best-effort라 데이터 저장이 정상이면 NORMAL 유지(다음 배치에서 재시도).</li>
 *   <li>NORMAL(첫 페이지 NODATA): HIRA가 명시적으로 가격 없음 응답 → 기존 active 가격 전체 삭제
 *       ({@link PriceYkihoCleanupService#removeAllActiveByYkiho}). 정리 실패 시 FAILED로 반환해
 *       재시도 신호를 보존한다 (사용자에게 stale 가격을 노출하지 않기 위함).</li>
 *   <li>FAILED(응답 실패/중간 페이지 누락): 어떤 정리도 하지 않는다 — 정상 데이터를 잃을 위험.</li>
 * </ul>
 * <p>
 * 정리 작업은 {@link PriceYkihoCleanupService}의 {@code REQUIRES_NEW} 트랜잭션으로 격리한다.
 * outer 트랜잭션이 rollback-only로 오염되는 일을 막아 catch 후 정상 분기 처리를 보장한다.
 */
@Slf4j
@Service
public class PriceYkihoSyncService {

    private static final int DETAIL_PAGE_SIZE = 100;
    /** 페이지 실패 시 재시도 횟수. 0이면 재시도 없음. */
    private static final int PAGE_RETRY = 2;

    private final HiraNonPayClient client;
    private final PriceYkihoCleanupService cleanupService;

    @PersistenceContext
    private EntityManager em;

    public PriceYkihoSyncService(HiraNonPayClient client,
                                 PriceYkihoCleanupService cleanupService) {
        this.client = client;
        this.cleanupService = cleanupService;
    }

    /**
     * ykiho 1개 처리.
     * <ul>
     *   <li>NORMAL: 모든 페이지 정상 처리 (1페이지 NODATA 포함)</li>
     *   <li>FAILED: 어느 페이지에서든 응답 실패 또는 중간 페이지 누락 발생</li>
     * </ul>
     */
    @Transactional
    public SyncResult saveOneYkiho(String ykiho) {
        int saved = 0;
        int pageNo = 1;
        Set<String> seenNpayCds = new HashSet<>();

        while (true) {
            HiraBody<NonPayDtlItem> body = fetchWithRetry(ykiho, pageNo);

            if (body.isFailed()) {
                log.warn("Price 페이지 실패 종료 (ykiho={}, pageNo={})", ykiho, pageNo);
                return new SyncResult(saved, HiraBody.Status.FAILED);
            }

            if (body.isNoData() || body.safeItems().isEmpty()) {
                if (pageNo == 1) {
                    // HIRA가 명시적으로 가격 없음 응답 — 기존 활성 가격이 남아 있으면 사용자에게
                    // 거짓 가격을 노출하므로 active row를 전부 정리한다 (만료된 이력은 보존).
                    // 정리 실패 시 NORMAL로 swallow하면 다음 배치까지 stale 가격이 살아남으므로
                    // FAILED를 반환해 재시도 신호를 보존한다.
                    try {
                        int removed = cleanupService.removeAllActiveByYkiho(ykiho);
                        if (removed > 0) {
                            log.info("Price NODATA 정리 (ykiho={}, removed={})", ykiho, removed);
                        }
                        return new SyncResult(0, HiraBody.Status.NORMAL);
                    } catch (Exception e) {
                        log.warn("Price NODATA 정리 실패 — FAILED 반환으로 재시도 유도 (ykiho={}): {}",
                                ykiho, e.getMessage());
                        return new SyncResult(0, HiraBody.Status.FAILED);
                    }
                }
                log.warn("Price 중간 페이지 누락 (ykiho={}, pageNo={})", ykiho, pageNo);
                return new SyncResult(saved, HiraBody.Status.FAILED);
            }

            for (NonPayDtlItem dto : body.safeItems()) {
                if (!dto.isActive()) {
                    continue;
                }
                seenNpayCds.add(dto.npayCd());
                try {
                    Price incoming = toEntity(dto);
                    Price existing = em.find(Price.class, new PriceId(dto.ykiho(), dto.npayCd()));
                    if (existing == null) {
                        em.persist(incoming);
                    } else {
                        existing.updateFromBatch(incoming);
                    }
                    saved++;
                } catch (Exception e) {
                    log.warn("Price 저장 실패 (ykiho={}, npayCd={}): {}",
                            dto.ykiho(), dto.npayCd(), e.getMessage());
                }
            }

            int totalPages = totalPages(body);
            if (pageNo >= totalPages) {
                break;
            }
            pageNo++;
            sleepBetweenCalls();
        }

        // 정상 종료 시에만 stale 정리. 빈 집합으로는 호출하지 않음 (응답이 잘렸을 가능성도 있어 보수적으로).
        // 정리는 REQUIRES_NEW로 격리돼 outer 트랜잭션을 오염시키지 않으므로 실패해도 저장된 데이터는 commit.
        // 저장 자체는 정상이라 NORMAL 유지 — 다음 배치 stale 정리에서 자연 재시도.
        if (!seenNpayCds.isEmpty()) {
            try {
                int removed = cleanupService.removeStaleByYkiho(ykiho, seenNpayCds);
                if (removed > 0) {
                    log.info("Price stale row 정리 (ykiho={}, removed={})", ykiho, removed);
                }
            } catch (Exception e) {
                log.warn("Price stale 정리 실패 (ykiho={}): {}", ykiho, e.getMessage());
            }
        }

        return new SyncResult(saved, HiraBody.Status.NORMAL);
    }

    private HiraBody<NonPayDtlItem> fetchWithRetry(String ykiho, int pageNo) {
        HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail(ykiho, pageNo, DETAIL_PAGE_SIZE);
        for (int attempt = 0; attempt < PAGE_RETRY && body.isFailed(); attempt++) {
            sleepBetweenCalls();
            body = client.searchHospPriceDetail(ykiho, pageNo, DETAIL_PAGE_SIZE);
        }
        return body;
    }

    private static int totalPages(HiraBody<NonPayDtlItem> body) {
        if (body.getTotalCount() <= 0) {
            return 1;
        }
        return (int) Math.ceil((double) body.getTotalCount() / DETAIL_PAGE_SIZE);
    }

    private Price toEntity(NonPayDtlItem dto) {
        return Price.builder()
                .ykiho(dto.ykiho())
                .npayCd(dto.npayCd())
                .curAmt(dto.curAmt())
                .adtFrDd(dto.adtFrDd())
                .adtEndDd(dto.adtEndDd())
                .build();
    }

    private void sleepBetweenCalls() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 페이지 루프 결과. NORMAL은 빈 결과 포함 정상 종료, FAILED는 응답 실패/중간 누락. */
    public record SyncResult(int saved, HiraBody.Status status) {
    }
}
