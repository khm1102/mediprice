package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.repository.PriceRepository;
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
 * 정상 종료 시에만 이번 응답에서 본 활성 코드 집합 기준 stale row를 제거한다.
 */
@Slf4j
@Service
public class PriceYkihoSyncService {

    private static final int DETAIL_PAGE_SIZE = 100;
    /** 페이지 실패 시 재시도 횟수. 0이면 재시도 없음. */
    private static final int PAGE_RETRY = 2;

    private final HiraNonPayClient client;
    private final PriceRepository priceRepository;

    @PersistenceContext
    private EntityManager em;

    public PriceYkihoSyncService(HiraNonPayClient client, PriceRepository priceRepository) {
        this.client = client;
        this.priceRepository = priceRepository;
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
                    return new SyncResult(0, HiraBody.Status.NORMAL);
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
                    em.merge(toEntity(dto));
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
        if (!seenNpayCds.isEmpty()) {
            try {
                int removed = priceRepository.removeStaleByYkiho(ykiho, seenNpayCds);
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
