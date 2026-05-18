package com.khm1102.mediprice.service;

import com.khm1102.mediprice.client.HiraDetailClient;
import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import com.khm1102.mediprice.client.hira.DgsbjtItem;
import com.khm1102.mediprice.client.hira.DtlInfoItem;
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

    /** 진료과목·의료장비·특수진료는 *CdNm null인 항목 제거. */
    @Test
    void filtersOutNullNamesFromCodeLists() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());

        HospitalDetailBundle bundle = new HospitalDetailBundle(
                List.of(new DgsbjtItem("01", "내과", null), new DgsbjtItem("99", null, null)),
                List.of(new MedOftItem("M1", "MRI", 1), new MedOftItem("M2", null, null)),
                List.of(),
                Optional.empty(),
                List.of(new SpclDiagItem("S1", "응급의료센터"), new SpclDiagItem("S2", null))
        );
        when(detailClient.fetchAll(YKIHO)).thenReturn(bundle);

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.dgsbjtList()).containsExactly("내과");
        assertThat(dto.medOftList()).containsExactly("MRI");
        assertThat(dto.spclDiagList()).containsExactly("응급의료센터");
    }

    /** 교통 정보 복수 row를 그대로 TransitItem 리스트로 변환. */
    @Test
    void mapsTrnsprtListToTransitItems() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());

        HospitalDetailBundle bundle = new HospitalDetailBundle(
                List.of(), List.of(),
                List.of(
                        new TrnsprtItem("지하철", "5호선", "서대문역", "4번 출구", "도보 5분"),
                        new TrnsprtItem("시내버스", "710", "병원 앞", "-", "50m")
                ),
                Optional.empty(), List.of()
        );
        when(detailClient.fetchAll(YKIHO)).thenReturn(bundle);

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.transitList()).hasSize(2);
        assertThat(dto.transitList().get(0).trafNm()).isEqualTo("지하철");
        assertThat(dto.transitList().get(0).lineNo()).isEqualTo("5호선");
        assertThat(dto.transitList().get(1).trafNm()).isEqualTo("시내버스");
    }

    /** DtlInfo가 있으면 parkingInfo와 operatingInfo로 분리 매핑. */
    @Test
    void splitsDtlInfoIntoParkingAndOperating() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());

        DtlInfoItem dtl = new DtlInfoItem(
                "298", "Y", "당일 8시간",
                "08:00 ~ 17:00", "08:00 ~ 12:00", "12:30 ~ 13:30",
                "휴진", "휴진", "Y", "Y"
        );
        HospitalDetailBundle bundle = new HospitalDetailBundle(
                List.of(), List.of(), List.of(), Optional.of(dtl), List.of()
        );
        when(detailClient.fetchAll(YKIHO)).thenReturn(bundle);

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.parkingInfo()).isNotNull();
        assertThat(dto.parkingInfo().parkQty()).isEqualTo("298");
        assertThat(dto.parkingInfo().parkXpnsYn()).isEqualTo("Y");

        assertThat(dto.operatingInfo()).isNotNull();
        assertThat(dto.operatingInfo().rcvWeek()).isEqualTo("08:00 ~ 17:00");
        assertThat(dto.operatingInfo().emyDayYn()).isEqualTo("Y");
    }

    /** 주차 필드 3개가 모두 null이면 ParkingInfo 자체를 null로 — 프론트가 섹션 통째로 숨길 수 있게. */
    @Test
    void parkingInfoIsNullWhenAllParkFieldsNull() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());

        DtlInfoItem dtl = new DtlInfoItem(
                null, null, null,
                "08:00 ~ 17:00", null, null,
                null, null, null, null
        );
        HospitalDetailBundle bundle = new HospitalDetailBundle(
                List.of(), List.of(), List.of(), Optional.of(dtl), List.of()
        );
        when(detailClient.fetchAll(YKIHO)).thenReturn(bundle);

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.parkingInfo()).isNull();
        assertThat(dto.operatingInfo()).isNotNull();
        assertThat(dto.operatingInfo().rcvWeek()).isEqualTo("08:00 ~ 17:00");
    }

    /** DtlInfo 응답 자체가 없으면 parkingInfo와 operatingInfo 모두 null. */
    @Test
    void parkingAndOperatingNullWhenDtlInfoMissing() {
        givenHospital();
        when(priceRepository.findAllByYkiho(YKIHO)).thenReturn(List.of());
        when(nonPayItemService.lookupNamesByCodes(any())).thenReturn(Map.of());
        when(detailClient.fetchAll(YKIHO)).thenReturn(emptyBundle());

        HospitalDetailDto dto = service.lookupDetail(YKIHO);

        assertThat(dto.parkingInfo()).isNull();
        assertThat(dto.operatingInfo()).isNull();
        assertThat(dto.transitList()).isEmpty();
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
        return new HospitalDetailBundle(List.of(), List.of(), List.of(), Optional.empty(), List.of());
    }
}
