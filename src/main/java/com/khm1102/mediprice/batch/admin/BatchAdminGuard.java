package com.khm1102.mediprice.batch.admin;

import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * `/api/internal/batch/**` 수동 트리거 보호 가드.
 * <p>
 * 두 단계 검증:
 * <ol>
 *   <li>{@code batch.admin-enabled} 플래그 (기본 false) — 꺼져 있으면 {@code B001 BATCH_ADMIN_DISABLED}.</li>
 *   <li>{@code batch.admin-secret} 설정값과 {@code X-Batch-Admin-Secret} 헤더 정확 일치 —
 *       하나라도 null/blank거나 불일치면 {@code B003 BATCH_ADMIN_FORBIDDEN}.</li>
 * </ol>
 * <p>
 * 정책:
 * <ul>
 *   <li>fail-closed 기본값: 둘 다 미설정이면 모든 요청 거부.</li>
 *   <li>{@code String.isBlank()}로 null/blank 통합 판정. 양끝 공백 {@code trim} 금지 —
 *       설정값과 헤더가 글자 단위로 정확히 일치해야 함 (내부 공백 있는 secret도 그대로 일치).</li>
 *   <li>{@link #constantTimeEquals(String, String)}로 측면채널(timing) 공격을 줄임.
 *       길이 불일치는 즉시 false, 동일 길이 내에서는 short-circuit 없이 XOR 누적.</li>
 * </ul>
 * <p>
 * SecurityConfig의 `/api/internal/**` permitAll은 그대로 유지하고 본 가드가 단일 진입점.
 * 향후 Spring Security 단의 ADMIN role 추가는 이중 방어로 별도 PR에서 추가 권장.
 */
@Component
public class BatchAdminGuard {

    /** 요청 헤더 이름. 운영 가이드/curl 예시에도 동일 문자열 사용. */
    public static final String SECRET_HEADER = "X-Batch-Admin-Secret";

    private final boolean adminEnabled;
    private final String adminSecret;

    public BatchAdminGuard(@Value("${batch.admin-enabled:false}") boolean adminEnabled,
                           @Value("${batch.admin-secret:}") String adminSecret) {
        this.adminEnabled = adminEnabled;
        this.adminSecret = adminSecret;
    }

    /**
     * 인증 통과면 void return. 실패 시 {@link BusinessException}을 던진다
     * — GlobalExceptionHandler가 403 + ApiResponse error로 변환.
     */
    public void requirePermission(HttpServletRequest request) {
        if (!adminEnabled) {
            throw new BusinessException(ErrorCode.BATCH_ADMIN_DISABLED);
        }
        if (adminSecret == null || adminSecret.isBlank()) {
            throw new BusinessException(ErrorCode.BATCH_ADMIN_FORBIDDEN,
                    "batch.admin-secret이 설정되지 않았습니다.");
        }
        String provided = request.getHeader(SECRET_HEADER);
        if (provided == null || provided.isBlank()) {
            throw new BusinessException(ErrorCode.BATCH_ADMIN_FORBIDDEN,
                    SECRET_HEADER + " 헤더가 누락되었습니다.");
        }
        if (!constantTimeEquals(provided, adminSecret)) {
            throw new BusinessException(ErrorCode.BATCH_ADMIN_FORBIDDEN,
                    SECRET_HEADER + "이 일치하지 않습니다.");
        }
    }

    /**
     * 두 문자열의 상수시간 비교. 길이 다르면 즉시 false, 동일 길이 내에서는
     * XOR 누적으로 short-circuit 없이 비교 — timing 측면채널을 줄인다.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
