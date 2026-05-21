package com.khm1102.mediprice.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 신규 OAuth 사용자의 약관 동의 전 임시 정보를 보관한다.
 * 서버 재시작 시 초기화된다 (프로토타입 수준).
 */
@Service
public class ConsentService {

    /** 임시 키 유효 시간 (10분) */
    private static final long EXPIRY_SECONDS = 600;

    private final Map<String, PendingConsent> pending = new ConcurrentHashMap<>();

    public record PendingConsent(
            String email,
            String name,
            String provider,
            String oauthId,
            Instant createdAt
    ) {}

    /**
     * 신규 OAuth 사용자 정보를 임시 저장하고, UUID 키를 반환한다.
     */
    public String store(String email, String name, String provider, String oauthId) {
        String key = UUID.randomUUID().toString();
        pending.put(key, new PendingConsent(email, name, provider, oauthId, Instant.now()));
        return key;
    }

    /**
     * 키로 임시 정보를 조회한다. 10분 초과 시 삭제 후 empty 반환.
     */
    public Optional<PendingConsent> retrieve(String key) {
        if (key == null) {
            return Optional.empty();
        }
        PendingConsent pc = pending.get(key);
        if (pc == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(pc.createdAt().plusSeconds(EXPIRY_SECONDS))) {
            pending.remove(key);
            return Optional.empty();
        }
        return Optional.of(pc);
    }

    /**
     * 키를 삭제한다 (동의 완료 또는 취소 시).
     */
    public void remove(String key) {
        if (key != null) {
            pending.remove(key);
        }
    }
}
