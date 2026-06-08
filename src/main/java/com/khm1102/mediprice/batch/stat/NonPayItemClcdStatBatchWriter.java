package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.hira.stat.NonPayClcdStatItem;
import com.khm1102.mediprice.client.hira.stat.StatValues;
import com.khm1102.mediprice.entity.NonPayItemClcdStat;
import com.khm1102.mediprice.entity.NonPayItemClcdStatId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Clcd 통계 batch writer — wide → long 변환 + 페이지 단위 REQUIRES_NEW.
 * <p>
 * 한 wide row는 종별 최대 4개 long row로 펼쳐짐 ({@code All/Usgh/Hosp/Gnhp}).
 * 4통계 모두 null인 종별은 entry 생략.
 */
@Slf4j
@Service
public class NonPayItemClcdStatBatchWriter {

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<NonPayClcdStatItem> page) {
        int saved = 0;
        for (NonPayClcdStatItem dto : page) {
            if (dto.npayCd() == null || dto.stdDate() == null) {
                continue;
            }
            Map<String, StatValues> statByClcd = dto.asStatByClcd();
            for (Map.Entry<String, StatValues> entry : statByClcd.entrySet()) {
                try {
                    NonPayItemClcdStat incoming = toEntity(dto.npayCd(), entry.getKey(), dto.stdDate(), entry.getValue());
                    NonPayItemClcdStat existing = em.find(NonPayItemClcdStat.class,
                            new NonPayItemClcdStatId(dto.npayCd(), entry.getKey(), dto.stdDate()));
                    if (existing == null) {
                        em.persist(incoming);
                    } else {
                        existing.updateFromBatch(incoming);
                    }
                    saved++;
                } catch (Exception e) {
                    log.warn("NonPayItemClcdStat 저장 실패 (npayCd={}, clcdKey={}): {}",
                            dto.npayCd(), entry.getKey(), e.getMessage());
                }
            }
        }
        em.flush();
        em.clear();
        return saved;
    }

    private NonPayItemClcdStat toEntity(String npayCd, String clcdKey, String stdDate, StatValues v) {
        return NonPayItemClcdStat.builder()
                .npayCd(npayCd)
                .clcdKey(clcdKey)
                .stdDate(stdDate)
                .prcAvg(v.avg())
                .prcMid(v.mid())
                .prcMin(v.min())
                .prcMax(v.max())
                .build();
    }
}
