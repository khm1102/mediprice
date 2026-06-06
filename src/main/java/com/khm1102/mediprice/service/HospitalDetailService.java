package com.khm1102.mediprice.service;

import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.repository.HospitalRepository;
import com.khm1102.mediprice.dto.HospitalDetailBasicsDto;
import com.khm1102.mediprice.dto.HospitalDetailDto;
import com.khm1102.mediprice.dto.HospitalDetailExtrasDto;


import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import com.khm1102.mediprice.client.hira.DgsbjtItem;
import com.khm1102.mediprice.client.hira.DtlInfoItem;
import com.khm1102.mediprice.client.hira.MedOftItem;
import com.khm1102.mediprice.client.hira.NonPayDtlItem;
import com.khm1102.mediprice.client.hira.SpclDiagItem;
import com.khm1102.mediprice.client.hira.TrnsprtItem;
import com.khm1102.mediprice.entity.Price;
import com.khm1102.mediprice.global.exception.business.HospitalNotFoundException;
import com.khm1102.mediprice.repository.PriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 병원 상세 — DB(Hospital + Price + NonPayItem 매핑) + 의료기관 상세 5개 API 병합.
 * 5개 API는 {@link HiraDetailClient#fetchAll}로 병렬 호출.
 */
@Service
@Transactional(readOnly = true)
public class HospitalDetailService {

    private final HospitalRepository hospitalRepository;
    private final PriceRepository priceRepository;
    private final NonPayItemService nonPayItemService;
    private final HospitalDetailHiraCache hiraCache;

    public HospitalDetailService(HospitalRepository hospitalRepository,
                                 PriceRepository priceRepository,
                                 NonPayItemService nonPayItemService,
                                 HospitalDetailHiraCache hiraCache) {
        this.hospitalRepository = hospitalRepository;
        this.priceRepository = priceRepository;
        this.nonPayItemService = nonPayItemService;
        this.hiraCache = hiraCache;
    }

    /**
     * 옛 combined 응답. 프론트는 {@link #lookupBasics}/{@link #lookupExtras}를 따로 호출해 progressive rendering을 한다.
     * 본 메서드는 backward compat을 위해 유지한다.
     */
    public HospitalDetailDto lookupDetail(String ykiho) {
        HospitalDetailBasicsDto basics = lookupBasics(ykiho);
        HospitalDetailExtrasDto extras = lookupExtras(ykiho);
        return new HospitalDetailDto(
                basics.ykiho(),
                basics.yadmNm(),
                basics.addr(),
                basics.telNo(),
                basics.clCdNm(),
                basics.hospUrl(),
                basics.drTotCnt(),
                basics.prices(),
                extras.dgsbjtList(),
                extras.medOftList(),
                extras.transitList(),
                extras.parkingInfo(),
                extras.operatingInfo(),
                extras.spclDiagList()
        );
    }

    /** DB only — 병원 기본 + 비급여 가격. 외부 호출 없음. */
    public HospitalDetailBasicsDto lookupBasics(String ykiho) {
        Hospital hospital = hospitalRepository.findById(ykiho)
                .orElseThrow(HospitalNotFoundException::new);

        List<Price> activePrices = priceRepository
                .findAllByYkihoAndAdtEndDd(ykiho, NonPayDtlItem.ACTIVE_END_DATE);

        Map<String, String> nameByCode = nonPayItemService.lookupNamesByCodes(
                activePrices.stream().map(Price::getNpayCd).toList());

        List<HospitalDetailDto.PriceItem> priceItems = activePrices.stream()
                .map(p -> new HospitalDetailDto.PriceItem(
                        p.getNpayCd(),
                        nameByCode.getOrDefault(p.getNpayCd(), p.getNpayCd()),
                        p.getCurAmt()))
                .toList();

        return new HospitalDetailBasicsDto(
                hospital.getYkiho(),
                hospital.getYadmNm(),
                hospital.getAddr(),
                hospital.getTelNo(),
                hospital.getClCdNm(),
                hospital.getHospUrl(),
                hospital.getDrTotCnt(),
                priceItems
        );
    }

    /** HIRA 5종(진료과목/장비/교통/주차·운영/특수진료) — {@link HospitalDetailHiraCache} 캐시 활용. */
    public HospitalDetailExtrasDto lookupExtras(String ykiho) {
        // /extras도 ykiho 유효성을 보장(없는 ykiho면 404).
        if (!hospitalRepository.existsById(ykiho)) {
            throw new HospitalNotFoundException();
        }
        HospitalDetailBundle bundle = hiraCache.lookupBundle(ykiho);
        return new HospitalDetailExtrasDto(
                ykiho,
                toDgsbjtNames(bundle.dgsbjtList()),
                toMedOftNames(bundle.medOftList()),
                toTransitList(bundle.trnsprtList()),
                toParkingInfo(bundle.dtlInfo()),
                toOperatingInfo(bundle.dtlInfo()),
                toSpclDiagNames(bundle.spclDiagList())
        );
    }

    private List<String> toDgsbjtNames(List<DgsbjtItem> items) {
        return items.stream()
                .map(DgsbjtItem::dgsbjtCdNm)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> toMedOftNames(List<MedOftItem> items) {
        return items.stream()
                .map(MedOftItem::oftCdNm)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> toSpclDiagNames(List<SpclDiagItem> items) {
        return items.stream()
                .map(SpclDiagItem::srchCdNm)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<HospitalDetailDto.TransitItem> toTransitList(List<TrnsprtItem> items) {
        return items.stream()
                .map(t -> new HospitalDetailDto.TransitItem(
                        t.trafNm(), t.lineNo(), t.arivPlc(), t.dir(), t.dist()))
                .collect(Collectors.toList());
    }

    private HospitalDetailDto.ParkingInfo toParkingInfo(Optional<DtlInfoItem> opt) {
        return opt.map(d -> {
            if (d.parkQty() == null && d.parkXpnsYn() == null && d.parkEtc() == null) {
                return null;
            }
            return new HospitalDetailDto.ParkingInfo(d.parkQty(), d.parkXpnsYn(), d.parkEtc());
        }).orElse(null);
    }

    private HospitalDetailDto.OperatingInfo toOperatingInfo(Optional<DtlInfoItem> opt) {
        return opt.map(d -> {
            if (d.rcvWeek() == null && d.rcvSat() == null && d.lunchWeek() == null
                    && d.noTrmtSun() == null && d.noTrmtHoli() == null
                    && d.emyDayYn() == null && d.emyNgtYn() == null) {
                return null;
            }
            return new HospitalDetailDto.OperatingInfo(
                    d.rcvWeek(), d.rcvSat(), d.lunchWeek(),
                    d.noTrmtSun(), d.noTrmtHoli(),
                    d.emyDayYn(), d.emyNgtYn());
        }).orElse(null);
    }
}
