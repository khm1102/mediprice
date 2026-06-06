package com.khm1102.mediprice.controller;

import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import com.khm1102.mediprice.service.HospitalDetailService;
import com.khm1102.mediprice.service.HospitalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@code /api/hospitals/{ykiho}}, {@code /basics}, {@code /extras} 핸들러의 ykiho 입력 검증.
 * <p>
 * ykiho는 HIRA HTTP 호출의 query param으로 그대로 흘러가므로 SSRF taint source.
 * 표준/URL-safe base64에서 나올 수 있는 문자({@code A-Za-z0-9+/=_-})만 허용해 path 구분자를 차단한다.
 */
@ExtendWith(MockitoExtension.class)
class HospitalApiControllerLookupTest {

    @Mock HospitalService hospitalService;
    @Mock HospitalDetailService detailService;

    private HospitalApiController controller() {
        return new HospitalApiController(hospitalService, detailService);
    }

    @Test
    void base64LikeYkihoReachesService() {
        HospitalApiController c = controller();

        c.lookupHospital("JDQ4MTYxMUFCQ0RFRkdISg==");
        c.lookupBasics("ABC123");
        c.lookupExtras("URL-safe_Token-");

        verify(detailService).lookupDetail("JDQ4MTYxMUFCQ0RFRkdISg==");
        verify(detailService).lookupBasics("ABC123");
        verify(detailService).lookupExtras("URL-safe_Token-");
    }

    @Test
    void longYkihoIsAllowedSincePatternHasNoLengthCap() {
        String longYkiho = "A".repeat(150);

        controller().lookupBasics(longYkiho);

        verify(detailService).lookupBasics(longYkiho);
    }

    @Test
    void pathTraversalCharactersAreRejected() {
        HospitalApiController c = controller();

        assertInvalid(() -> c.lookupHospital("../etc/passwd"));
        assertInvalid(() -> c.lookupBasics("ABC.123"));
        assertInvalid(() -> c.lookupExtras("ABC\\123"));

        verifyNoInteractions(detailService);
    }

    @Test
    void queryAndFragmentSeparatorsAreRejected() {
        HospitalApiController c = controller();

        assertInvalid(() -> c.lookupHospital("ABC?id=1"));
        assertInvalid(() -> c.lookupBasics("ABC#frag"));
        assertInvalid(() -> c.lookupExtras("ABC&x=1"));

        verifyNoInteractions(detailService);
    }

    @Test
    void whitespaceAndControlCharactersAreRejected() {
        HospitalApiController c = controller();

        assertInvalid(() -> c.lookupHospital("ABC 123"));
        assertInvalid(() -> c.lookupBasics("ABC\t123"));
        assertInvalid(() -> c.lookupExtras("ABC\n123"));

        verifyNoInteractions(detailService);
    }

    @Test
    void emptyYkihoIsRejected() {
        assertInvalid(() -> controller().lookupHospital(""));
        verifyNoInteractions(detailService);
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
    }
}
