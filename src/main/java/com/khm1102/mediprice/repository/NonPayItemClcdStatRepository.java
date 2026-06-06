package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.NonPayItemClcdStat;
import com.khm1102.mediprice.entity.NonPayItemClcdStatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NonPayItemClcdStatRepository
        extends JpaRepository<NonPayItemClcdStat, NonPayItemClcdStatId> {

    /**
     * 검색 결과 enrich용 batch 조회 — 전달된 npayCd 집합의 모든 (clcdKey, stdDate) 통계 행을 한 번에 가져온다.
     * 호출자가 (npayCd, clcdKey) 기준으로 최신 stdDate를 골라 평균을 결정한다.
     * 결과 수는 입력 npayCd 수 × clcdKey 종류(≤4) × stdDate 이력으로 작아 메모리 안전.
     */
    @Query("SELECT s FROM NonPayItemClcdStat s WHERE s.npayCd IN :npayCds")
    List<NonPayItemClcdStat> findAllByNpayCdIn(@Param("npayCds") Collection<String> npayCds);
}
