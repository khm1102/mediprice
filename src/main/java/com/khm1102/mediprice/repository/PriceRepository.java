package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.entity.PriceId;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceRepository extends JpaRepository<Price, PriceId> {

    /** 병원 상세에서 사용. adt_end_dd = '99991231' 필터는 Service 레이어에서. */
    List<Price> findAllByYkiho(String ykiho);
}
