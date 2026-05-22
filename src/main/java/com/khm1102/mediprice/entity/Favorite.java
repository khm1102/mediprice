package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "Favorite",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "ykiho"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite extends BaseEntity {

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "ykiho", nullable = false, length = 200)
    private String ykiho;

    public static Favorite create(Long memberId, String ykiho) {
        Favorite f = new Favorite();
        f.memberId = memberId;
        f.ykiho = ykiho;
        return f;
    }
}
