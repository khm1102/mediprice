package com.khm1102.mediprice.service;

import com.khm1102.mediprice.entity.Member;
import com.khm1102.mediprice.repository.MemberRepository;
import com.khm1102.mediprice.util.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 3. 신규 OAuth 회원 생성
        Member newMember = Member.createOAuth(email, name, provider, oauthId);
        memberRepository.save(newMember);
        return jwtUtil.generateMemberToken(newMember.getId(), newMember.getEmail(), newMember.getRole().name(), newMember.getName());
    }
}
