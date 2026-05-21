package com.khm1102.mediprice.service;

import com.khm1102.mediprice.entity.Member;
import com.khm1102.mediprice.repository.MemberRepository;
import com.khm1102.mediprice.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    public AuthService(MemberRepository memberRepository, JwtUtil jwtUtil) {
        this.memberRepository = memberRepository;
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
     * 약관 동의 완료 후 신규 회원을 생성하고 JWT 토큰을 반환한다.
     */
    public String registerNewMember(String email, String name, String provider, String oauthId) {
        // 동시 가입 방어: 동일 OAuth ID로 이미 생성된 경우 기존 토큰 반환
        Optional<Member> existing = memberRepository
                .findByOauthProviderAndOauthIdAndDeletedDttmIsNull(provider, oauthId);
        if (existing.isPresent()) {
            Member m = existing.get();
            return jwtUtil.generateMemberToken(m.getId(), m.getEmail(), m.getRole().name(), m.getName());
        }

        Member newMember = Member.createOAuth(email, name, provider, oauthId, OffsetDateTime.now());
        memberRepository.save(newMember);
        return jwtUtil.generateMemberToken(newMember.getId(), newMember.getEmail(),
                newMember.getRole().name(), newMember.getName());
    }
}
