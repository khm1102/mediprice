package com.khm1102.mediprice.service;

import com.khm1102.mediprice.entity.Hospital;
import com.khm1102.mediprice.repository.HospitalRepository;
import com.khm1102.mediprice.dto.HospitalDetailDto;


import com.khm1102.mediprice.client.HiraDetailClient;
import com.khm1102.mediprice.client.HiraDetailClient.HospitalDetailBundle;
import com.khm1102.mediprice.client.hira.DgsbjtItem;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 병원 상세 — DB(Hospital + Price + NonPayItem 매핑) + 의료기관 상세 4개 API 병합.
 * 4개 API는 {@link HiraDetailClient#fetchAll}로 병렬 호출.
 */
@Service
@Transactional(readOnly = true)
public class HospitalDetailService {

    private final HospitalRepository hospitalRepository;
    private final PriceRepository priceRepository;
    private final NonPayItemService nonPayItemService;
    private final HiraDetailClient detailClient;

    public HospitalDetailService(HospitalRepository hospitalRepository,
                                 PriceRepository priceRepository,
                                 NonPayItemService nonPayItemService,
                                 HiraDetailClient detailClient) {
        this.hospitalRepository = hospitalRepository;
        this.priceRepository = priceRepository;
        this.nonPayItemService = nonPayItemService;
        this.detailClient = detailClient;
    }

    public HospitalDetailDto lookupDetail(String ykiho) {
        Hospital hospital = hospitalRepository.findById(ykiho)
                .orElseThrow(HospitalNotFoundException::new);

        List<Price> activePrices = priceRepository.findAllByYkiho(ykiho).stream()
                .filter(p -> NonPayDtlItem.ACTIVE_END_DATE.equals(p.getAdtEndDd()))
                .toList();

        Map<String, String> nameByCode = nonPayItemService.lookupNamesByCodes(
                activePrices.stream().map(Price::getNpayCd).toList());

        List<HospitalDetailDto.PriceItem> priceItems = activePrices.stream()
                .map(p -> new HospitalDetailDto.PriceItem(
                        p.getNpayCd(),
                        nameByCode.getOrDefault(p.getNpayCd(), p.getNpayCd()),
                        p.getCurAmt()))
                .toList();

        HospitalDetailBundle bundle = detailClient.fetchAll(ykiho);

        return new HospitalDetailDto(
                hospital.getYkiho(),
                hospital.getYadmNm(),
                hospital.getAddr(),
                hospital.getTelNo(),
                hospital.getClCdNm(),
                hospital.getHospUrl(),
                hospital.getDrTotCnt(),
                priceItems,
                toDgsbjtNames(bundle.dgsbjtList()),
                toMedOftNames(bundle.medOftList()),
                toTrnsprtInfo(bundle.trnsprt()),
                toSpclDiagNames(bundle.spclDiagList())
        );
    }

    private List<String> toDgsbjtNames(List<DgsbjtItem> items) {
        return items.stream()
                .map(DgsbjtItem::dgsbjtCdNm)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> toMedOftNames(List<MedOftItem> items) {
        return items.stream()
                .map(MedOftItem::medOftCdNm)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> toSpclDiagNames(List<SpclDiagItem> items) {
        return items.stream()
                .map(SpclDiagItem::srvTpCdNm)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    private HospitalDetailDto.TrnsprtInfo toTrnsprtInfo(Optional<TrnsprtItem> opt) {
        return opt.map(t -> new HospitalDetailDto.TrnsprtInfo(
                t.parkYn(), t.parkXpnsYn(), t.parkEtc(), t.trafInfo(), t.parkQty()
        )).orElse(null);
    }
}
