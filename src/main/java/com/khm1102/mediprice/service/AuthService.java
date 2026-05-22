package com.khm1102.mediprice.service;

import com.khm1102.mediprice.entity.Favorite;
import com.khm1102.mediprice.entity.Member;
import com.khm1102.mediprice.repository.FavoriteRepository;
import com.khm1102.mediprice.repository.MemberRepository;
import com.khm1102.mediprice.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final FavoriteRepository favoriteRepository;
    private final JwtUtil jwtUtil;

    public AuthService(MemberRepository memberRepository,
                       FavoriteRepository favoriteRepository,
                       JwtUtil jwtUtil) {
        this.memberRepository = memberRepository;
        this.favoriteRepository = favoriteRepository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(readOnly = true)
    public String generateGuestToken() {
        return jwtUtil.generateGuestToken(UUID.randomUUID().toString());
    }

    /**
     * OAuth 로그인 처리.
     * - 기존 회원: JWT 토큰 반환
     * - 신규 회원: null 반환 (약관 동의 필요)
     */
    public String handleOAuthLogin(String email, String name, String provider, String oauthId) {
        // 1. OAuth ID로 기존 계정 조회
        Optional<Member> byOAuth = memberRepository
                .findByOauthProviderAndOauthIdAndDeletedDttmIsNull(provider, oauthId);
        if (byOAuth.isPresent()) {
            Member m = byOAuth.get();
            return jwtUtil.generateMemberToken(m.getId(), m.getEmail(), m.getRole().name(), m.getName());
        }

        // 2. 이메일로 기존 계정 조회 → OAuth 연결
        Optional<Member> byEmail = memberRepository.findByEmailAndDeletedDttmIsNull(email);
        if (byEmail.isPresent()) {
            Member m = byEmail.get();
            m.updateOAuthInfo(name, provider, oauthId);
            return jwtUtil.generateMemberToken(m.getId(), m.getEmail(), m.getRole().name(), m.getName());
        }

        // 3. 신규 사용자 → null 반환 (약관 동의 후 registerNewMember 호출)
        return null;
    }

    /**
     * 회원 탈퇴 — 즐겨찾기 논리 삭제 후 회원 논리 삭제.
     */
    public void withdraw(Long memberId) {
        List<Favorite> favorites = favoriteRepository.findByMemberIdAndDeletedDttmIsNull(memberId);
        favorites.forEach(Favorite::delete);

        memberRepository.findById(memberId).ifPresent(Member::delete);
    }

    /**
     * 약관 동의 완료 후 회원을 등록하고 JWT 토큰을 반환한다.
     * <p>
     * 처리 순서:
     * 1. 동일 OAuth ID의 활성 계정 존재 → 토큰만 재발급 (동시 가입 방어)
     * 2. 동일 OAuth ID의 탈퇴 계정 존재 → soft-deleted 레코드 재활성화 (unique 제약 우회)
     * 3. 완전 신규 → INSERT
     */
    public String registerNewMember(String email, String name, String provider, String oauthId) {
        // 1. 동시 가입 방어: 활성 계정이 이미 있으면 기존 토큰 반환
        Optional<Member> existing = memberRepository
                .findByOauthProviderAndOauthIdAndDeletedDttmIsNull(provider, oauthId);
        if (existing.isPresent()) {
            Member m = existing.get();
            return jwtUtil.generateMemberToken(m.getId(), m.getEmail(), m.getRole().name(), m.getName());
        }

        // 2. 탈퇴한 계정 재가입 — 새 INSERT 대신 기존 레코드 재활성화
        //    (email 컬럼 unique 제약이 있어 INSERT하면 constraint violation 발생)
        Optional<Member> deleted = memberRepository.findByOauthProviderAndOauthId(provider, oauthId);
        if (deleted.isPresent()) {
            Member m = deleted.get();
            m.reactivate(name, OffsetDateTime.now());
            return jwtUtil.generateMemberToken(m.getId(), m.getEmail(), m.getRole().name(), m.getName());
        }

        // 3. 완전 신규 가입
        Member newMember = Member.createOAuth(email, name, provider, oauthId, OffsetDateTime.now());
        memberRepository.save(newMember);
        return jwtUtil.generateMemberToken(newMember.getId(), newMember.getEmail(),
                newMember.getRole().name(), newMember.getName());
    }
}
