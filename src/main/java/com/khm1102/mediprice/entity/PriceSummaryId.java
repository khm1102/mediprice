package com.khm1102.mediprice.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link PriceSummary} 복합키 — (ykiho, npayCd, adtFrDd). 시기별 row를 모두 보존하므로
 * adtFrDd가 키 일부.
 */
public class PriceSummaryId implements Serializable {

    private String ykiho;
    private String npayCd;
    private String adtFrDd;

    public PriceSummaryId() {
    }

    public PriceSummaryId(String ykiho, String npayCd, String adtFrDd) {
        this.ykiho = ykiho;
        this.npayCd = npayCd;
        this.adtFrDd = adtFrDd;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriceSummaryId that)) return false;
        return Objects.equals(ykiho, that.ykiho)
                && Objects.equals(npayCd, that.npayCd)
                && Objects.equals(adtFrDd, that.adtFrDd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ykiho, npayCd, adtFrDd);
    }
}
