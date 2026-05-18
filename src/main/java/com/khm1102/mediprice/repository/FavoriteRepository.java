package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    List<Favorite> findByMemberIdAndDeletedDttmIsNull(Long memberId);

    Optional<Favorite> findByMemberIdAndYkihoAndDeletedDttmIsNull(Long memberId, String ykiho);

    /** 소프트 삭제 레코드 포함 조회 — 재추가 시 복원용 */
    Optional<Favorite> findByMemberIdAndYkiho(Long memberId, String ykiho);

    boolean existsByMemberIdAndYkihoAndDeletedDttmIsNull(Long memberId, String ykiho);
}
