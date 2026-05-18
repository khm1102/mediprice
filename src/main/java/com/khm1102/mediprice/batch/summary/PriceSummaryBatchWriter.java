package com.khm1102.mediprice.batch.summary;

import com.khm1102.mediprice.client.hira.NonPayHospSummaryItem;
import com.khm1102.mediprice.entity.PriceSummary;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PriceSummary 배치 writer — {@link PriceSummarySyncService}의 Consumer 트랜잭션 단위.
 * <p>
 * Producer-Consumer self-call 시 트랜잭션 프록시 우회를 피하기 위해 별도 빈으로 분리.
 * batch 단위로 merge + flush/clear → N² dirty-check 차단.
 */
@Slf4j
@Service
public class PriceSummaryBatchWriter {

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<NonPayHospSummaryItem> batch) {
        int saved = 0;
        for (NonPayHospSummaryItem dto : batch) {
            if (dto.ykiho() == null || dto.npayCd() == null || dto.adtFrDd() == null) {
                continue;
            }
            try {
                em.merge(toEntity(dto));
                saved++;
            } catch (Exception e) {
                log.warn("PriceSummary 저장 실패 (ykiho={}, npayCd={}, adtFrDd={}): {}",
                        dto.ykiho(), dto.npayCd(), dto.adtFrDd(), e.getMessage());
            }
        }
        em.flush();
        em.clear();
        return saved;
    }

    private PriceSummary toEntity(NonPayHospSummaryItem dto) {
        return PriceSummary.builder()
                .ykiho(dto.ykiho())
                .npayCd(dto.npayCd())
                .adtFrDd(dto.adtFrDd())
                .adtEndDd(dto.adtEndDd())
                .clCd(dto.clCd())
                .clCdNm(dto.clCdNm())
                .sidoCd(dto.sidoCd())
                .sidoCdNm(dto.sidoCdNm())
                .sgguCd(dto.sgguCd())
                .sgguCdNm(dto.sgguCdNm())
                .yadmNm(dto.yadmNm())
                .npayKorNm(dto.npayKorNm())
                .minPrc(dto.minPrc())
                .maxPrc(dto.maxPrc())
                .urlAddr(dto.urlAddr())
                .build();
    }
}
