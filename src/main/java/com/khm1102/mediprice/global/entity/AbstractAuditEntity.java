package com.khm1102.mediprice.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 시간 감사 필드(created/updated/deleted)만 가진 추상 엔티티.
 * <p>
 * {@link BaseEntity}는 {@code Long id}를 함께 갖지만, 도메인 엔티티가 자체 PK(예: ykiho, npay_cd, 복합키)를
 * 가질 때 id 충돌을 피하기 위해 본 클래스를 상속한다.
 * <p>
 * Hibernate 설정의 {@code hibernate.jdbc.time_zone=UTC}로 OffsetDateTime이 UTC로 정규화되어 저장된다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class AbstractAuditEntity {

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

    /** 논리 삭제 — CLAUDE.md §5.2 (delete = 논리 삭제, remove = 물리 삭제). */
    public void delete() {
        this.deletedDttm = OffsetDateTime.now();
    }
}
