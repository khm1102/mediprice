package com.khm1102.mediprice.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link NonPayItemClcdStat} 복합키 — (npayCd, clcdKey, stdDate).
 * <p>
 * stdDate가 변하면 새 row가 들어와 이전 통계를 그대로 보존. 통계 시계열을 유지하기 위함.
 */
public class NonPayItemClcdStatId implements Serializable {

    private String npayCd;
    private String clcdKey;
    private String stdDate;

    public NonPayItemClcdStatId() {
    }

    public NonPayItemClcdStatId(String npayCd, String clcdKey, String stdDate) {
        this.npayCd = npayCd;
        this.clcdKey = clcdKey;
        this.stdDate = stdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NonPayItemClcdStatId that)) return false;
        return Objects.equals(npayCd, that.npayCd)
                && Objects.equals(clcdKey, that.clcdKey)
                && Objects.equals(stdDate, that.stdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(npayCd, clcdKey, stdDate);
    }
}
