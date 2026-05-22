package com.khm1102.mediprice.repository;

import com.khm1102.mediprice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndDeletedDttmIsNull(String email);

    Optional<Member> findByOauthProviderAndOauthIdAndDeletedDttmIsNull(String provider, String oauthId);

    boolean existsByEmailAndDeletedDttmIsNull(String email);
}
