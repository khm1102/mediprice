package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.entity.PriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PriceRepository extends JpaRepository<Price, PriceId> {

    /** 병원 상세에서 사용 — DB 레벨에서 adt_end_dd 필터링. */
    List<Price> findAllByYkihoAndAdtEndDd(String ykiho, String adtEndDd);

    /**
     * 이번 동기화에서 본 활성 npayCd 집합에 없는 기존 Price row를 물리 삭제한다.
     * <p>
     * 호출처는 ykiho 동기화가 NORMAL로 끝났을 때만 호출해야 한다. 부분 실패 상태에서 호출하면
     * 단지 응답이 잘려서 빠진 코드를 stale로 오인해 정상 데이터를 잃을 수 있다.
     * <p>
     * activeCodes가 비면 전체 삭제가 되므로 호출처에서 빈 컬렉션 가드 필수.
     */
    @Modifying
    @Query("delete from Price p where p.ykiho = :ykiho and p.npayCd not in :activeCodes")
    int removeStaleByYkiho(@Param("ykiho") String ykiho,
                           @Param("activeCodes") Collection<String> activeCodes);
}
