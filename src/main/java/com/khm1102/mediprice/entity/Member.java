package com.khm1102.mediprice.entity;

import com.khm1102.mediprice.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "Member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Column(name = "email", unique = true, length = 255)
    private String email;

    /** BCrypt 해시. 현재 미사용 (OAuth 전용). */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** "google" 등 OAuth 제공자 식별자. */
    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    /** OAuth 제공자가 부여한 고유 ID. */
    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    /** 이용약관 동의 일시 (신규 가입 시 기록). */
    @Column(name = "terms_agreed_at")
    private OffsetDateTime termsAgreedAt;

    public enum Role { MEMBER, ADMIN }

    /** OAuth 회원 생성 (구글 등) */
    public static Member createOAuth(String email, String name, String provider, String oauthId,
                                     OffsetDateTime termsAgreedAt) {
        Member m = new Member();
        m.email = email;
        m.name = name;
        m.role = Role.MEMBER;
        m.oauthProvider = provider;
        m.oauthId = oauthId;
        m.termsAgreedAt = termsAgreedAt;
        return m;
    }

    /** 기존 계정에 OAuth 연결 또는 OAuth 정보 업데이트 */
    public void updateOAuthInfo(String name, String provider, String oauthId) {
        this.name = name;
        this.oauthProvider = provider;
        this.oauthId = oauthId;
        // 이 기능 도입 전 생성된 계정은 termsAgreedAt 이 null 일 수 있음
        // OAuth 최초 연결 시점을 동의 일시로 기록
        if (this.termsAgreedAt == null) {
            this.termsAgreedAt = OffsetDateTime.now();
        }
    }
}
