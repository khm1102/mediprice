package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.hira.nonpay.NonPayCodeItem;
import com.khm1102.mediprice.entity.NonPayItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 비급여 항목 batch writer — {@link NonPayItemSyncService}의 페이지 워커 트랜잭션 단위.
 * <p>
 * 페이지 단위 {@code REQUIRES_NEW} 트랜잭션 + flush/clear → 영속성 컨텍스트 비대화 방지.
 */
@Slf4j
@Service
public class NonPayItemBatchWriter {

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<NonPayCodeItem> page) {
        int saved = 0;
        for (NonPayCodeItem dto : page) {
            try {
                NonPayItem incoming = toEntity(dto);
                NonPayItem existing = em.find(NonPayItem.class, dto.npayCd());
                if (existing == null) {
                    if (incoming.getNpayKorNm() == null) {
                        log.warn("NonPayItem 신규 row 필수값 누락 skip (npayCd={}, npayKorNm=null)", dto.npayCd());
                        continue;
                    }
                    em.persist(incoming);
                } else {
                    existing.updateFromBatch(incoming);
                }
                saved++;
            } catch (Exception e) {
                log.warn("NonPayItem 저장 실패 (npayCd={}): {}", dto.npayCd(), e.getMessage());
            }
        }
        em.flush();
        em.clear();
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
}
