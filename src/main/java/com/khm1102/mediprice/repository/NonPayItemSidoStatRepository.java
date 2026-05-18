package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.NonPayItemSidoStat;
import com.khm1102.mediprice.entity.NonPayItemSidoStatId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NonPayItemSidoStatRepository
        extends JpaRepository<NonPayItemSidoStat, NonPayItemSidoStatId> {
}
