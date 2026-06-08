package com.khm1102.mediprice.batch.item;

import com.khm1102.mediprice.client.hira.nonpay.NonPayCodeItem;
import com.khm1102.mediprice.entity.NonPayItem;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NonPayItemBatchWriterTest {

    @Mock EntityManager em;

    @InjectMocks NonPayItemBatchWriter writer;

    @BeforeEach
    void wireEm() throws Exception {
        var f = NonPayItemBatchWriter.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(writer, em);
    }

    /** 신규 항목은 page row마다 persist + 끝에 flush/clear. */
    @Test
    void persistsEachNewItemThenFlushAndClear() {
        List<NonPayCodeItem> page = List.of(
                new NonPayCodeItem("N1", "이름1", "M", "중", "S", "소", "2024", "99991231"),
                new NonPayCodeItem("N2", "이름2", "M", "중", "S", "소", "2024", "99991231"));

        int saved = writer.saveBatch(page);

        assertThat(saved).isEqualTo(2);

        ArgumentCaptor<NonPayItem> captor = ArgumentCaptor.forClass(NonPayItem.class);
        verify(em, times(2)).persist(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(NonPayItem::getNpayCd)
                .containsExactly("N1", "N2");

        verify(em, times(1)).flush();
        verify(em, times(1)).clear();
    }

    /** persist 실패해도 다른 row 진행 + flush/clear 호출. */
    @Test
    void continuesOnIndividualFailure() {
        org.mockito.Mockito.doThrow(new RuntimeException("fail"))
                .doNothing()
                .when(em).persist(any());

        List<NonPayCodeItem> page = List.of(
                new NonPayCodeItem("N1", "이름1", null, null, null, null, null, null),
                new NonPayCodeItem("N2", "이름2", null, null, null, null, null, null));

        int saved = writer.saveBatch(page);

        assertThat(saved).isEqualTo(1);
        verify(em, times(2)).persist(any());
        verify(em).flush();
        verify(em).clear();
    }

    /** 기존 항목 update에서는 null 입력이 기존 값을 덮지 않는다. */
    @Test
    void existingItemKeepsCurrentValuesWhenIncomingFieldsAreNull() {
        NonPayItem existing = NonPayItem.builder()
                .npayCd("N1")
                .npayKorNm("기존명")
                .npayMdivCd("M1")
                .npayMdivCdNm("기존중분류")
                .npaySdivCd("S1")
                .npaySdivCdNm("기존소분류")
                .adtFrDd("20240101")
                .adtEndDd("99991231")
                .build();
        when(em.find(eq(NonPayItem.class), eq("N1"))).thenReturn(existing);

        int saved = writer.saveBatch(List.of(
                new NonPayCodeItem("N1", null, null, null, null, null, null, null)));

        assertThat(saved).isEqualTo(1);
        assertThat(existing.getNpayKorNm()).isEqualTo("기존명");
        assertThat(existing.getNpayMdivCdNm()).isEqualTo("기존중분류");
        assertThat(existing.getAdtEndDd()).isEqualTo("99991231");
    }

    /** 신규 항목의 필수 이름이 없으면 flush-time constraint failure 대신 row 단위 skip. */
    @Test
    void skipsNewItemWhenRequiredNameIsMissing() {
        int saved = writer.saveBatch(List.of(
                new NonPayCodeItem("N1", null, null, null, null, null, null, null)));

        assertThat(saved).isZero();
        verify(em, never()).persist(any());
        verify(em).flush();
        verify(em).clear();
    }
}
