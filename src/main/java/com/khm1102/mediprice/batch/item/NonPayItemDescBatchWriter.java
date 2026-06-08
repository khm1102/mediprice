package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.hira.nonpay.NonPayDescItem;
import com.khm1102.mediprice.entity.NonPayItemDesc;
import com.khm1102.mediprice.repository.NonPayItemDescRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 비급여 항목 설명 batch writer — UNIQUE (div_cd_1, div_cd_2, div_cd_3) 키 기반 upsert.
 * <p>
 * BaseEntity의 auto-increment id를 보존하기 위해 SELECT id → INSERT 또는 UPDATE 분기.
 * 페이지 단위 {@code REQUIRES_NEW} 트랜잭션.
 */
@Slf4j
@Service
public class NonPayItemDescBatchWriter {

    private final NonPayItemDescRepository repository;

    @PersistenceContext
    private EntityManager em;

    public NonPayItemDescBatchWriter(NonPayItemDescRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int saveBatch(List<NonPayDescItem> page) {
        int saved = 0;
        for (NonPayDescItem dto : page) {
            try {
                upsert(dto);
                saved++;
            } catch (Exception e) {
                log.warn("NonPayItemDesc 저장 실패 (div_cd_1={}, div_cd_2={}, div_cd_3={}): {}",
                        dto.divCd1(), dto.divCd2(), dto.divCd3(), e.getMessage());
            }
        }
        em.flush();
        em.clear();
        return saved;
    }

    private void upsert(NonPayDescItem dto) {
        Long existingId = em.createQuery("""
                        SELECT d.id
                        FROM NonPayItemDesc d
                        WHERE d.divCd1 = :c1
                          AND ((d.divCd2 IS NULL AND :c2 IS NULL) OR d.divCd2 = :c2)
                          AND ((d.divCd3 IS NULL AND :c3 IS NULL) OR d.divCd3 = :c3)
                        """, Long.class)
                .setParameter("c1", dto.divCd1())
                .setParameter("c2", dto.divCd2())
                .setParameter("c3", dto.divCd3())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (existingId == null) {
            repository.save(toEntity(dto));
        } else {
            NonPayItemDesc existing = em.find(NonPayItemDesc.class, existingId);
            if (existing != null) {
                existing.updateFromBatch(toEntity(dto));
            }
        }
    }

    private NonPayItemDesc toEntity(NonPayDescItem dto) {
        return NonPayItemDesc.builder()
                .divCd1(dto.divCd1())
                .divCd1Nm(dto.divCd1Nm())
                .divCd1Dsc(dto.divCd1Dsc())
                .divCd2(dto.divCd2())
                .divCd2Nm(dto.divCd2Nm())
                .divCd2Dsc(dto.divCd2Dsc())
                .divCd3(dto.divCd3())
                .divCd3Nm(dto.divCd3Nm())
                .divCd3Dsc(dto.divCd3Dsc())
                .build();
    }
}
