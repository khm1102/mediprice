package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.entity.PriceId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRepository extends JpaRepository<Price, PriceId> {

    /** 병원 상세에서 사용 — DB 레벨에서 adt_end_dd 필터링. */
    List<Price> findAllByYkihoAndAdtEndDd(String ykiho, String adtEndDd);
}
