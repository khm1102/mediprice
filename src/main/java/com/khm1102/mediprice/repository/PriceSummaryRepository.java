package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.PriceSummary;
import com.khm1102.mediprice.entity.PriceSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSummaryRepository extends JpaRepository<PriceSummary, PriceSummaryId> {
}
