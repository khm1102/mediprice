package com.khm1102.mediprice.batch.price;

import com.khm1102.mediprice.repository.PriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Price 정리 작업을 outer 트랜잭션과 격리한다.
 * <p>
 * {@link PriceYkihoSyncService#saveOneYkiho}는 자체 {@code @Transactional}을 가진다.
 * 그 안에서 직접 {@code @Modifying delete}를 호출하면 delete가 throw할 때 outer 트랜잭션이
 * rollback-only로 마킹되어 catch 이후에도 commit 단계에서
 * {@link org.springframework.transaction.UnexpectedRollbackException}이 발생한다.
 * <p>
 * 본 서비스 메서드는 {@code REQUIRES_NEW}로 별도 트랜잭션을 열고 닫는다. 실패의 영향 범위가
 * 정리 작업 자체에 한정되므로 호출자가 catch 후 후속 처리를 결정할 수 있다.
 */
@Service
public class PriceYkihoCleanupService {

    private final PriceRepository priceRepository;

    public PriceYkihoCleanupService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    /**
     * NODATA 정리 — HIRA가 명시적으로 가격 없음을 응답한 ykiho의 활성 가격 전체를 삭제한다.
     * 만료 이력(adt_end_dd != '99991231')은 보존.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int removeAllActiveByYkiho(String ykiho) {
        return priceRepository.removeAllActiveByYkiho(ykiho);
    }

    /**
     * stale 정리 — 정상 종료 시 이번 응답에서 본 활성 npayCd 집합에 없는 row를 삭제한다.
     * 빈 컬렉션이면 호출하지 말 것 (전체 삭제가 되므로) — 호출처에서 가드 필수.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int removeStaleByYkiho(String ykiho, Collection<String> activeCodes) {
        return priceRepository.removeStaleByYkiho(ykiho, activeCodes);
    }
}
