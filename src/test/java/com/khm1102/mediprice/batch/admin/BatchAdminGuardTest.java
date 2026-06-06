package com.khm1102.mediprice.batch.admin;

import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BatchAdminGuard} 단위 테스트.
 * <p>
 * 모든 fail 케이스에서 {@link BusinessException} + 정확한 ErrorCode를 단언.
 * trim 금지 정책의 회귀 가드도 포함 — 헤더 앞뒤 공백은 mismatch로 처리.
 */
class BatchAdminGuardTest {

    private static MockHttpServletRequest withSecret(String headerValue) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/internal/batch/sync");
        if (headerValue != null) {
            req.addHeader(BatchAdminGuard.SECRET_HEADER, headerValue);
        }
        return req;
    }

    // ── enabled 플래그 가드 ──────────────────────────────────────────────────

    @Test
    void disabledByDefaultThrowsBatchAdminDisabled() {
        BatchAdminGuard guard = new BatchAdminGuard(false, "any-secret");
        assertThatThrownBy(() -> guard.requirePermission(withSecret("any-secret")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_DISABLED);
    }

    // ── secret 설정값 null/blank ─────────────────────────────────────────────

    @Test
    void enabledButSecretNullThrowsForbidden() {
        BatchAdminGuard guard = new BatchAdminGuard(true, null);
        assertThatThrownBy(() -> guard.requirePermission(withSecret("anything")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    @Test
    void enabledButSecretBlankThrowsForbidden() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "   ");
        assertThatThrownBy(() -> guard.requirePermission(withSecret("anything")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    // ── 헤더 null/blank ─────────────────────────────────────────────────────

    @Test
    void headerMissingThrowsForbidden() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "real-secret");
        assertThatThrownBy(() -> guard.requirePermission(withSecret(null)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    @Test
    void headerBlankThrowsForbidden() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "real-secret");
        assertThatThrownBy(() -> guard.requirePermission(withSecret("   ")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    // ── 값 불일치 ────────────────────────────────────────────────────────────

    @Test
    void headerMismatchThrowsForbidden() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "real-secret");
        assertThatThrownBy(() -> guard.requirePermission(withSecret("wrong-secret")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    @Test
    void lengthMismatchedSecretsAreRejected() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "abcd");
        assertThatThrownBy(() -> guard.requirePermission(withSecret("abc")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    /**
     * trim 금지 정책 회귀 가드 — 헤더 앞뒤 공백을 제거하지 않고 정확히 일치해야 통과.
     * 누가 secret.equals(header.trim())으로 바꾸면 이 테스트가 실패한다.
     */
    @Test
    void headerWithSurroundingWhitespaceIsRejected() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "abc");
        assertThatThrownBy(() -> guard.requirePermission(withSecret(" abc ")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);
    }

    // ── 통과 ────────────────────────────────────────────────────────────────

    @Test
    void correctSecretPasses() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "real-secret-xyz");
        assertThatCode(() -> guard.requirePermission(withSecret("real-secret-xyz")))
                .doesNotThrowAnyException();
    }

    /** 내부 공백이 있는 secret도 글자 단위로 정확히 일치하면 통과. */
    @Test
    void secretWithInternalSpacesPassesIfHeaderMatchesExactly() {
        BatchAdminGuard guard = new BatchAdminGuard(true, "two words");
        assertThatCode(() -> guard.requirePermission(withSecret("two words")))
                .doesNotThrowAnyException();
    }

    /** SECRET_HEADER 상수가 회귀로 변경되면 운영 배포 가이드도 같이 바뀌어야 한다. */
    @Test
    void secretHeaderConstantIsXBatchAdminSecret() {
        assertThat(BatchAdminGuard.SECRET_HEADER).isEqualTo("X-Batch-Admin-Secret");
    }
}
