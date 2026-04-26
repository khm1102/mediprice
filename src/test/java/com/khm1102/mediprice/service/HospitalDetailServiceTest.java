package com.khm1102.mediprice.service;

import com.khm1102.mediprice.client.HiraDetailClient;
import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import com.khm1102.mediprice.client.hira.DgsbjtItem;
import com.khm1102.mediprice.client.hira.MedOftItem;
import com.khm1102.mediprice.client.hira.SpclDiagItem;
import com.khm1102.mediprice.client.hira.TrnsprtItem;
import com.khm1102.mediprice.dto.HospitalDetailDto;
import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.global.exception.business.HospitalNotFoundException;
import com.khm1102.mediprice.repository.HospitalRepository;
import com.khm1102.mediprice.repository.PriceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalDetailServiceTest {

    @Mock HospitalRepository hospitalRepository;
    @Mock PriceRepository priceRepository;
    @Mock NonPayItemService nonPayItemService;
    @Mock HiraDetailClient detailClient;

    @InjectMocks HospitalDetailService service;

    private static final String YKIHO = "YK001";

    /** ykiho에 매칭되는 병원 없으면 404로 떨어지게 — 컨트롤러가 그대로 던짐. */
    @Test
    void throwsHospitalNotFoundWhenYkihoMissing() {
        when(hospitalRepository.findById(YKIHO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.lookupDetail(YKIHO))
                .isInstanceOf(HospitalNotFoundException.class);
    }

    /** Price는 99991231짜리만 노출. 만료된 가격이 화면에 새는 게 가장 무서운 버그. */
    @Test
    void filtersOutInactivePricesByAdtEndDd() {
        givenHospital();
        Price active = price("N001", 50_000L, "99991231");
        Price expired = price("N002", 99_999L, "20231231");
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of(active, expired));
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());
        when(detailClient.fetchAll(YKIHO)).thenReturn(emptyBundle());

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.prices()).extracting(HospitalDetailDto.PriceItem::npayCd)
                .containsExactly("N001");
    }

    /** 코드→이름 맵에 있으면 한글명, 없으면 코드 그대로 (NonPayItem 동기화 누락 대비 fallback). */
    @Test
    void usesNameMapAndFallsBackToCodeWhenMissing() {
        givenHospital();
        Price mapped = price("N001", 50_000L, "99991231");
        Price unmapped = price("N999", 30_000L, "99991231");
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of(mapped, unmapped));
        when(nonPayItemService.lookupNamesByCodes(List.of("N001", "N999")))
                .thenReturn(Map.of("N001", "박피술"));
        when(detailClient.fetchAll(YKIHO)).thenReturn(emptyBundle());

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.prices()).extracting(HospitalDetailDto.PriceItem::npayKorNm)
                .containsExactly("박피술", "N999");
    }

    /** 4개 외부 API 결과 병합 — null cdNm 항목은 제거, trnsprt Optional은 record로 변환. */
    @Test
    void mergesBundleFiltersNullsAndConvertsTrnsprt() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());

        HospitalDetailBundle bundle = new HospitalDetailBundle(
                List.of(new DgsbjtItem("01", "내과", null), new DgsbjtItem("99", null, null)),
                List.of(new MedOftItem("M1", "MRI", 1), new MedOftItem("M2", null, null)),
                Optional.of(new TrnsprtItem("Y", "N", "주차2시간무료", "지하철 2호선", "20")),
                List.of(new SpclDiagItem("S1", "응급의료센터"), new SpclDiagItem("S2", null))
        );
        when(detailClient.fetchAll(YKIHO)).thenReturn(bundle);

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.dgsbjtList()).containsExactly("내과");
        assertThat(dto.medOftList()).containsExactly("MRI");
        assertThat(dto.spclDiagList()).containsExactly("응급의료센터");
        assertThat(dto.trnsprtInfo()).isNotNull();
        assertThat(dto.trnsprtInfo().parkYn()).isEqualTo("Y");
        assertThat(dto.trnsprtInfo().trafInfo()).isEqualTo("지하철 2호선");
    }

    /** 교통정보 응답 자체가 빈 경우 → trnsprtInfo는 null (응답에서 통째 생략됨). */
    @Test
    void leavesTrnsprtInfoNullWhenBundleHasNone() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());
        when(detailClient.fetchAll(YKIHO)).thenReturn(emptyBundle());

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.trnsprtInfo()).isNull();
    }

    private Hospital sampleHospital;

    @BeforeEach
    void initHospital() {
        sampleHospital = Hospital.builder()
                .ykiho(YKIHO)
                .yadmNm("샘플병원")
                .addr("서울시 강남구")
                .telNo("02-0000-0000")
                .clCdNm("종합병원")
                .hospUrl("http://sample.hospital")
                .drTotCnt(50)
                .build();
    }

    private void givenHospital() {
        when(hospitalRepository.findById(YKIHO)).thenReturn(Optional.of(sampleHospital));
    }

    private static Price price(String code, long amt, String endDd) {
        return Price.builder()
                .ykiho(YKIHO)
                .npayCd(code)
                .curAmt(amt)
                .adtEndDd(endDd)
                .build();
    }

    private static HospitalDetailBundle emptyBundle() {
        return new HospitalDetailBundle(List.of(), List.of(), Optional.empty(), List.of());
    }
}
