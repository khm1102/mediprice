package com.khm1102.mediprice.batch;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
import com.khm1102.mediprice.entity.Price;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ykiho 1개에 대한 가격상세 동기화 — 트랜잭션 단위.
 * <p>
 * {@link PriceSyncService}가 1600+ ykiho를 순회하면서 ykiho마다 본 메서드를 호출 →
 * 각 호출이 독립 트랜잭션이라 DB connection을 길게 점유하지 않는다.
 */
@Slf4j
@Service
public class PriceYkihoSyncService {

    private static final int DETAIL_PAGE_SIZE = 100;

    private final HiraNonPayClient client;

    @PersistenceContext
    private EntityManager em;

    public PriceYkihoSyncService(HiraNonPayClient client) {
        this.client = client;
    }

    /** ykiho 1개 처리. 빈 응답이면 0 반환, 가격 데이터 있으면 저장 건수 반환. */
    @Transactional
    public int saveOneYkiho(String ykiho) {
        int saved = 0;
        int pageNo = 1;
        while (true) {
            HiraBody<NonPayDtlItem> body = client.searchHospPriceDetail(ykiho, pageNo, DETAIL_PAGE_SIZE);
            if (body.safeItems().isEmpty()) {
                break;
            }
            for (NonPayDtlItem dto : body.safeItems()) {
                if (!dto.isActive()) {
                    continue;
                }
                try {
                    em.merge(toEntity(dto));
                    saved++;
                } catch (Exception e) {
                    log.warn("Price 저장 실패 (ykiho={}, npayCd={}): {}",
                            dto.ykiho(), dto.npayCd(), e.getMessage());
                }
            }
            int totalPages = (int) Math.ceil((double) body.getTotalCount() / DETAIL_PAGE_SIZE);
            if (pageNo >= totalPages) {
                break;
            }
            pageNo++;
            sleepBetweenCalls();
        }
        return saved;
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
}
