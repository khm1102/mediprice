package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.NonPayItemClcdStat;
import com.khm1102.mediprice.entity.NonPayItemClcdStatId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NonPayItemClcdStatRepository
        extends JpaRepository<NonPayItemClcdStat, NonPayItemClcdStatId> {
}
