package com.khm1102.mediprice.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * {@link NonPayItemSidoStat} 복합키 — (npayCd, sidoKey, stdDate).
 */
public class NonPayItemSidoStatId implements Serializable {

    private String npayCd;
    private String sidoKey;
    private String stdDate;

    public NonPayItemSidoStatId() {
    }

    public NonPayItemSidoStatId(String npayCd, String sidoKey, String stdDate) {
        this.npayCd = npayCd;
        this.sidoKey = sidoKey;
        this.stdDate = stdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NonPayItemSidoStatId that)) return false;
        return Objects.equals(npayCd, that.npayCd)
                && Objects.equals(sidoKey, that.sidoKey)
                && Objects.equals(stdDate, that.stdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(npayCd, sidoKey, stdDate);
    }
}
