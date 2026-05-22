package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndDeletedDttmIsNull(String email);

    Optional<Member> findByOauthProviderAndOauthIdAndDeletedDttmIsNull(String provider, String oauthId);

    /** 탈퇴 여부 무관하게 조회 — 재가입 시 soft-deleted 레코드 재활성화용 */
    Optional<Member> findByOauthProviderAndOauthId(String provider, String oauthId);

    boolean existsByEmailAndDeletedDttmIsNull(String email);
}
