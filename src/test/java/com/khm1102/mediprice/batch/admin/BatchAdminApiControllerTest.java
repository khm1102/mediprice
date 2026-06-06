package com.khm1102.mediprice.batch.admin;

import com.khm1102.mediprice.batch.item.NonPayItemDescSyncService;
import com.khm1102.mediprice.batch.orchestrator.BatchService;
import com.khm1102.mediprice.batch.price.PriceSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemClcdStatSyncService;
import com.khm1102.mediprice.batch.stat.NonPayItemSidoStatSyncService;
import com.khm1102.mediprice.batch.summary.PriceSummarySyncService;
import com.khm1102.mediprice.global.common.ApiResponse;
import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.business.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Field;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link BatchAdminApiController} 단위 테스트.
 * <p>
 * Direct executor({@code Runnable::run})를 주입해 {@code CompletableFuture.runAsync}가 동기 실행되도록 한다.
 * 이로써 6개 엔드포인트 각각에서 happy-path 시 (a) 올바른 SyncService 호출 (b) 다른 SyncService 호출 없음
 * (c) BatchService.evictPostBatchCaches 호출을 직접 verify할 수 있다.
 * <p>
 * RUNNING AtomicBoolean이 정적이라 테스트 간 leak — {@code @BeforeEach}에서 reflection으로 false 재설정.
 */
@ExtendWith(MockitoExtension.class)
class BatchAdminApiControllerTest {

    @Mock BatchAdminGuard guard;
    @Mock BatchService batchService;
    @Mock PriceSyncService priceSyncService;
    @Mock NonPayItemDescSyncService descSyncService;
    @Mock PriceSummarySyncService summarySyncService;
    @Mock NonPayItemClcdStatSyncService clcdStatSyncService;
    @Mock NonPayItemSidoStatSyncService sidoStatSyncService;

    private final Executor directExecutor = Runnable::run;
    private BatchAdminApiController controller;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() throws Exception {
        controller = new BatchAdminApiController(
                guard, batchService,
                priceSyncService, descSyncService, summarySyncService,
                clcdStatSyncService, sidoStatSyncService,
                directExecutor);
        request = new MockHttpServletRequest("POST", "/api/internal/batch/sync");
        resetRunningFlag();
    }

    /** RUNNING은 static AtomicBoolean이라 테스트 간 leak — 매번 false로 강제 리셋. */
    private static void resetRunningFlag() throws Exception {
        Field field = BatchAdminApiController.class.getDeclaredField("RUNNING");
        field.setAccessible(true);
        AtomicBoolean running = (AtomicBoolean) field.get(null);
        running.set(false);
    }

    // ── happy-path: 6 endpoint × 가드 통과 시 올바른 SyncService만 호출 ────────

    @Test
    void triggerSyncCallsBatchServiceAndEvictsCachesOnly() {
        ResponseEntity<ApiResponse<String>> res = controller.triggerSync(request);

        verify(guard).requirePermission(request);
        verify(batchService).syncAll();
        verify(batchService).evictPostBatchCaches();
        verifyNoInteractions(priceSyncService, descSyncService, summarySyncService,
                clcdStatSyncService, sidoStatSyncService);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().success()).isTrue();
    }

    @Test
    void triggerPriceSyncCallsOnlyPriceSyncService() {
        ResponseEntity<ApiResponse<String>> res = controller.triggerPriceSync(request);

        verify(guard).requirePermission(request);
        verify(priceSyncService).sync();
        verify(batchService).evictPostBatchCaches();
        verify(batchService, never()).syncAll();
        verifyNoInteractions(descSyncService, summarySyncService,
                clcdStatSyncService, sidoStatSyncService);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void triggerDescSyncCallsOnlyDescSyncService() {
        ResponseEntity<ApiResponse<String>> res = controller.triggerDescSync(request);

        verify(guard).requirePermission(request);
        verify(descSyncService).sync();
        verify(batchService).evictPostBatchCaches();
        verify(batchService, never()).syncAll();
        verifyNoInteractions(priceSyncService, summarySyncService,
                clcdStatSyncService, sidoStatSyncService);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void triggerSummarySyncCallsOnlySummarySyncService() {
        ResponseEntity<ApiResponse<String>> res = controller.triggerSummarySync(request);

        verify(guard).requirePermission(request);
        verify(summarySyncService).sync();
        verify(batchService).evictPostBatchCaches();
        verify(batchService, never()).syncAll();
        verifyNoInteractions(priceSyncService, descSyncService,
                clcdStatSyncService, sidoStatSyncService);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void triggerClcdStatSyncCallsOnlyClcdStatSyncService() {
        ResponseEntity<ApiResponse<String>> res = controller.triggerClcdStatSync(request);

        verify(guard).requirePermission(request);
        verify(clcdStatSyncService).sync();
        verify(batchService).evictPostBatchCaches();
        verify(batchService, never()).syncAll();
        verifyNoInteractions(priceSyncService, descSyncService, summarySyncService,
                sidoStatSyncService);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void triggerSidoStatSyncCallsOnlySidoStatSyncService() {
        ResponseEntity<ApiResponse<String>> res = controller.triggerSidoStatSync(request);

        verify(guard).requirePermission(request);
        verify(sidoStatSyncService).sync();
        verify(batchService).evictPostBatchCaches();
        verify(batchService, never()).syncAll();
        verifyNoInteractions(priceSyncService, descSyncService, summarySyncService,
                clcdStatSyncService);
        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    // ── 차단 케이스 (guard throws) — 6 endpoint × disabled / forbidden ─────────

    /** parameterized용: endpoint 호출자 + 표시 이름. */
    private record Endpoint(String name,
                            BiFunction<BatchAdminApiController, HttpServletRequest,
                                    ResponseEntity<ApiResponse<String>>> invoker) {
        @Override public String toString() { return name; }
    }

    static Stream<Endpoint> allEndpoints() {
        return Stream.of(
                new Endpoint("POST /sync",            BatchAdminApiController::triggerSync),
                new Endpoint("POST /sync/prices",     BatchAdminApiController::triggerPriceSync),
                new Endpoint("POST /sync/desc",       BatchAdminApiController::triggerDescSync),
                new Endpoint("POST /sync/summary",    BatchAdminApiController::triggerSummarySync),
                new Endpoint("POST /sync/clcd-stat",  BatchAdminApiController::triggerClcdStatSync),
                new Endpoint("POST /sync/sido-stat",  BatchAdminApiController::triggerSidoStatSync)
        );
    }

    @ParameterizedTest(name = "disabled blocks {0} and never touches any sync service")
    @MethodSource("allEndpoints")
    void disabledGuardBlocksAllEndpointsAndPreventsAnyServiceCall(Endpoint endpoint) {
        doThrow(new BusinessException(ErrorCode.BATCH_ADMIN_DISABLED))
                .when(guard).requirePermission(any());

        assertThatThrownBy(() -> endpoint.invoker().apply(controller, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_DISABLED);

        verifyNoInteractions(batchService, priceSyncService, descSyncService,
                summarySyncService, clcdStatSyncService, sidoStatSyncService);
    }

    @ParameterizedTest(name = "secret failure blocks {0} and never touches any sync service")
    @MethodSource("allEndpoints")
    void secretFailureBlocksAllEndpointsAndPreventsAnyServiceCall(Endpoint endpoint) {
        doThrow(new BusinessException(ErrorCode.BATCH_ADMIN_FORBIDDEN,
                "X-Batch-Admin-Secret이 일치하지 않습니다."))
                .when(guard).requirePermission(any());

        assertThatThrownBy(() -> endpoint.invoker().apply(controller, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BATCH_ADMIN_FORBIDDEN);

        verifyNoInteractions(batchService, priceSyncService, descSyncService,
                summarySyncService, clcdStatSyncService, sidoStatSyncService);
    }

    // ── B002 idempotency: 가드 통과 후 RUNNING이 켜져 있으면 두 번째 호출은 차단 ─

    @Test
    void secondInvocationWhileRunningReturnsAlreadyRunning() throws Exception {
        // 첫 호출 후 RUNNING이 finally에서 false로 리셋되므로 두 번째도 통과한다.
        // 이 테스트는 "다른 호출이 진행 중"이라는 상황 — RUNNING을 true로 강제 set.
        Field field = BatchAdminApiController.class.getDeclaredField("RUNNING");
        field.setAccessible(true);
        AtomicBoolean running = (AtomicBoolean) field.get(null);
        running.set(true);

        ResponseEntity<ApiResponse<String>> res = controller.triggerSync(request);

        verify(guard).requirePermission(request);
        assertThat(res.getStatusCode().value()).isEqualTo(409);
        verify(batchService, never()).syncAll();
        verify(batchService, never()).evictPostBatchCaches();
    }
}
