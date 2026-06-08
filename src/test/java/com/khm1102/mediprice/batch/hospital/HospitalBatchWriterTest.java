package com.khm1102.mediprice.batch.hospital;

import com.khm1102.mediprice.client.hira.hospital.HospBasisItem;
import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.repository.HospitalRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalBatchWriterTest {

    @Mock HospitalRepository hospitalRepository;
    @Mock EntityManager em;

    @InjectMocks HospitalBatchWriter writer;

    @BeforeEach
    void wireEm() throws Exception {
        var f = HospitalBatchWriter.class.getDeclaredField("em");
        f.setAccessible(true);
        f.set(writer, em);
    }

    /** 기존 병원 row가 있을 때 HIRA의 null optional 필드는 기존 값을 비우지 않는다. */
    @Test
    void existingHospitalKeepsCurrentValuesWhenIncomingFieldsAreNull() {
        Hospital existing = Hospital.builder()
                .ykiho("YK1")
                .yadmNm("기존병원")
                .clCd("31")
                .clCdNm("의원")
                .addr("기존주소")
                .xPos(126.97)
                .yPos(37.56)
                .telNo("02-1111-2222")
                .hospUrl("https://example.com")
                .drTotCnt(3)
                .sidoCdNm("서울")
                .sgguCdNm("중구")
                .build();
        when(em.find(eq(Hospital.class), eq("YK1"))).thenReturn(existing);

        int saved = writer.saveBatch(List.of(new HospBasisItem(
                "YK1", null, null, null, null, null, null, null, null, null, null, null)));

        assertThat(saved).isEqualTo(1);
        assertThat(existing.getYadmNm()).isEqualTo("기존병원");
        assertThat(existing.getAddr()).isEqualTo("기존주소");
        assertThat(existing.getTelNo()).isEqualTo("02-1111-2222");
        assertThat(existing.getXPos()).isEqualTo(126.97);
        assertThat(existing.getYPos()).isEqualTo(37.56);
        verify(em, never()).persist(any());
        verify(hospitalRepository, never()).updateLocation(any(), anyDouble(), anyDouble());
    }

    /** 신규 병원의 필수 이름이 없으면 flush-time constraint failure 대신 row 단위 skip. */
    @Test
    void skipsNewHospitalWhenRequiredNameIsMissing() {
        int saved = writer.saveBatch(List.of(new HospBasisItem(
                "YK1", null, "31", "의원", "주소", null, null, null, "서울", "중구", 126.97, 37.56)));

        assertThat(saved).isZero();
        verify(em, never()).persist(any());
        verify(hospitalRepository, never()).updateLocation(any(), anyDouble(), anyDouble());
        verify(em).flush();
        verify(em).clear();
    }
}
