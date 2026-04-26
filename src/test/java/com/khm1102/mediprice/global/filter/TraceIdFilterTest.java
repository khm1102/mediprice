package com.khm1102.mediprice.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TraceIdFilterTest {

    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock FilterChain chain;

    private final TraceIdFilter filter = new TraceIdFilter();
    private String capturedMdc;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    /** 헤더 없으면 UUID로 새로 만들고, 끝나면 MDC 비우는지 (요청 간 누수 방지). */
    @Test
    void generatesUuidWhenHeaderMissingAndPropagatesToMdcAndResponse() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn(null);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> traceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Trace-Id"), traceIdCaptor.capture());
        verify(chain).doFilter(request, response);

        String traceId = traceIdCaptor.getValue();
        assertThat(traceId).hasSize(32).matches("[0-9a-f]+");
        assertThat(MDC.get("traceId")).isNull();  // finally 블록이 정리해야 함
    }

    /** 게이트웨이가 보낸 헤더는 정상 형식이면 그대로 씀 (분산 추적 연결). */
    @Test
    void propagatesValidIncomingHeader() throws Exception {
        String incoming = "abc-123-XYZ-456";
        when(request.getHeader("X-Trace-Id")).thenReturn(incoming);
        captureMdcDuringChain();

        filter.doFilter(request, response, chain);

        verify(response).setHeader("X-Trace-Id", incoming);
        assertThat(capturedMdc).isEqualTo(incoming);
    }

    /** CRLF 같은 거 박혀 있으면 버리고 새로 만듦 (로그 인젝션 방어). */
    @Test
    void discardsHeaderWithUnsafeCharsAndGeneratesNewTraceId() throws Exception {
        when(request.getHeader("X-Trace-Id")).thenReturn("evil\r\nLog-Inject: yes");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<String> traceIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("X-Trace-Id"), traceIdCaptor.capture());
        assertThat(traceIdCaptor.getValue())
                .doesNotContain("\r", "\n", " ")
                .matches("[0-9a-f]{32}");
    }

    private void captureMdcDuringChain() throws Exception {
        doAnswer((InvocationOnMock inv) -> {
            capturedMdc = MDC.get("traceId");
            return null;
        }).when(chain).doFilter(any(), any());
    }
}
