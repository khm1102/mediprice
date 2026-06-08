package com.khm1102.mediprice.batch.stat;

import com.khm1102.mediprice.client.hira.stat.NonPaySidoStatItem;
import com.khm1102.mediprice.client.hira.stat.StatValues;
import com.khm1102.mediprice.entity.NonPayItemSidoStat;
import com.khm1102.mediprice.entity.NonPayItemSidoStatId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Sido 통계 batch writer — wide → long 변환 + 페이지 단위 REQUIRES_NEW.
 * <p>
 * 한 wide row는 시도 최대 18개 long row로 펼쳐짐 ({@code All} + 17개 시도 약어).
 * 4통계 모두 null인 시도는 entry 생략.
 */
@Slf4j
@Service
public class NonPayItemSidoStatBatchWriter {

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<NonPaySidoStatItem> page) {
        int saved = 0;
        for (NonPaySidoStatItem dto : page) {
            if (dto.npayCd() == null || dto.stdDate() == null) {
                continue;
            }
            Map<String, StatValues> statBySido = dto.asStatBySido();
            for (Map.Entry<String, StatValues> entry : statBySido.entrySet()) {
                try {
                    NonPayItemSidoStat incoming = toEntity(dto.npayCd(), entry.getKey(), dto.stdDate(), entry.getValue());
                    NonPayItemSidoStat existing = em.find(NonPayItemSidoStat.class,
                            new NonPayItemSidoStatId(dto.npayCd(), entry.getKey(), dto.stdDate()));
                    if (existing == null) {
                        em.persist(incoming);
                    } else {
                        existing.updateFromBatch(incoming);
                    }
                    saved++;
                } catch (Exception e) {
                    log.warn("NonPayItemSidoStat 저장 실패 (npayCd={}, sidoKey={}): {}",
                            dto.npayCd(), entry.getKey(), e.getMessage());
                }
            }
        }
        em.flush();
        em.clear();
        return saved;
    }

    private NonPayItemSidoStat toEntity(String npayCd, String sidoKey, String stdDate, StatValues v) {
        return NonPayItemSidoStat.builder()
                .npayCd(npayCd)
                .sidoKey(sidoKey)
                .stdDate(stdDate)
                .prcAvg(v.avg())
                .prcMid(v.mid())
                .prcMin(v.min())
                .prcMax(v.max())
                .build();
    }
}
