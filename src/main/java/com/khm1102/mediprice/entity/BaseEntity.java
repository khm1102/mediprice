package com.khm1102.mediprice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_dttm", nullable = false, updatable = false)
    private OffsetDateTime createdDttm;

    @Column(name = "updated_dttm", nullable = false)
    private OffsetDateTime updatedDttm;

    @Column(name = "deleted_dttm")
    private OffsetDateTime deletedDttm;

    @PrePersist
    protected void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdDttm = now;
        this.updatedDttm = now;
    }

    @PreUpdate
    protected void onPreUpdate() {
        this.updatedDttm = OffsetDateTime.now();
    }

    public void softDelete() {
        this.deletedDttm = OffsetDateTime.now();
    }
}
