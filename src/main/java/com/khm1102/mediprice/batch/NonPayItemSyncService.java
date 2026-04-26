package com.khm1102.mediprice.batch;

import com.khm1102.mediprice.client.HiraNonPayClient;
import com.khm1102.mediprice.client.hira.HiraBody;
import com.khm1102.mediprice.client.hira.NonPayCodeItem;
import com.khm1102.mediprice.entity.NonPayItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Step 1 — 비급여 항목 코드 동기화 ({@code getNonPaymentItemCodeList2}).
 * <p>
 * EntityManager.merge로 npay_cd 기준 upsert. 개별 항목 실패는 로그만 찍고 계속.
 */
@Slf4j
@Service
public class NonPayItemSyncService {

    private static final int PAGE_SIZE = 300;

    private final HiraNonPayClient client;

    @PersistenceContext
    private EntityManager em;

    public NonPayItemSyncService(HiraNonPayClient client) {
        this.client = client;
    }

    @Transactional
    public int sync() {
        int saved = 0;
        int pageNo = 1;
        while (true) {
            HiraBody<NonPayCodeItem> body = client.searchItemCodes(pageNo, PAGE_SIZE);
            if (body.safeItems().isEmpty()) {
                break;
            }
            for (NonPayCodeItem dto : body.safeItems()) {
                try {
                    em.merge(toEntity(dto));
                    saved++;
                } catch (Exception e) {
                    log.warn("NonPayItem 저장 실패 (npayCd={}): {}", dto.npayCd(), e.getMessage());
                }
            }
            int totalPages = (int) Math.ceil((double) body.getTotalCount() / PAGE_SIZE);
            if (pageNo >= totalPages) {
                break;
            }
            pageNo++;
            sleepBetweenCalls();
        }
        log.info("NonPayItemSyncService 완료 — saved={}", saved);
        return saved;
    }

    private NonPayItem toEntity(NonPayCodeItem dto) {
        return NonPayItem.builder()
                .npayCd(dto.npayCd())
                .npayKorNm(dto.npayKorNm())
                .npayMdivCd(dto.npayMdivCd())
                .npayMdivCdNm(dto.npayMdivCdNm())
                .npaySdivCd(dto.npaySdivCd())
                .npaySdivCdNm(dto.npaySdivCdNm())
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
