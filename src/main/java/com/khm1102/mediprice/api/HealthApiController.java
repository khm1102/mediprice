package com.khm1102.mediprice.api;

import com.khm1102.mediprice.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * 모니터링/probe용 헬스체크 엔드포인트.
 * <p>
 * "앱(Tomcat + Spring 컨텍스트)이 응답 가능한가"만 응답한다 — HTTP 200 + {@code success: true}로 충분.
 * DB/캐시 등 인프라 상태 체크는 별도 인프라 도구(Prometheus exporter, 클라우드 probe, DBA 모니터링)가 담당.
 */
@RestController
@RequestMapping("/api/health")
public class HealthApiController {

    public record HealthResponse(OffsetDateTime time) {
    }

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse(OffsetDateTime.now()));
    }
}
