package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.NonPayItemGroupDto;
import com.khm1102.mediprice.entity.NonPayItem;
import com.khm1102.mediprice.repository.NonPayItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NonPayItemServiceTest {

    @Mock NonPayItemRepository repository;
    @InjectMocks NonPayItemService service;

    /** 만료/분류없는 항목은 빼고, 중분류로 묶어서 가나다순으로 내려와야 함. */
    @Test
    void groupsActiveItemsByMidCategoryAndSortsAlphabetically() {
        NonPayItem skin1 = item("S001", "박피술", "S", "피부", "99991231");
        NonPayItem skin2 = item("S002", "레이저토닝", "S", "피부", "99991231");
        NonPayItem inactive = item("S003", "단종항목", "S", "피부", "20240101");
        NonPayItem dental = item("D001", "임플란트", "D", "치과", "99991231");
        NonPayItem nullMdiv = item("X001", "분류없음", null, null, "99991231");

        when(repository.findAll()).thenReturn(List.of(skin1, skin2, inactive, dental, nullMdiv));

        List<NonPayItemGroupDto> result = service.searchGroupedItems();

        assertThat(result).extracting(NonPayItemGroupDto::mdivCdNm)
                .containsExactly("치과", "피부");
        assertThat(result.get(1).items())
                .extracting(NonPayItemGroupDto.Item::npayKorNm)
                .containsExactly("레이저토닝", "박피술");
    }

    /** 가격 옆에 항목명 붙일 때 쓰는 코드→이름 맵. HospitalDetailService에서 호출. */
    @Test
    void returnsCodeToNameMapForGivenCodes() {
        NonPayItem a = item("A001", "에이", "M", "분류", "99991231");
        NonPayItem b = item("B002", "비", "M", "분류", "99991231");
        when(repository.findAllById(List.of("A001", "B002"))).thenReturn(List.of(a, b));

        var names = service.lookupNamesByCodes(List.of("A001", "B002"));

        assertThat(names).containsEntry("A001", "에이").containsEntry("B002", "비").hasSize(2);
    }

    /** 빈 코드 리스트 들어오면 DB 한 번 더 안 갈겨야 — 빈 맵으로 즉시 반환. */
    @Test
    void returnsEmptyMapForEmptyCodeList() {
        when(repository.findAllById(List.of())).thenReturn(List.of());

        assertThat(service.lookupNamesByCodes(List.of())).isEmpty();
    }

    /** 일부 코드만 DB에 있어도 누락 없이 그 부분만 매핑됨 (HospitalDetailService가 fallback 처리). */
    @Test
    void omitsCodesNotFoundInDatabase() {
        NonPayItem only = item("A001", "에이", "M", "분류", "99991231");
        when(repository.findAllById(List.of("A001", "MISSING"))).thenReturn(List.of(only));

        var names = service.lookupNamesByCodes(List.of("A001", "MISSING"));

        assertThat(names).containsOnly(java.util.Map.entry("A001", "에이"));
    }

    private NonPayItem item(String code, String name, String mdivCd, String mdivNm, String endDd) {
        return NonPayItem.builder()
                .npayCd(code)
                .npayKorNm(name)
                .npayMdivCd(mdivCd)
                .npayMdivCdNm(mdivNm)
                .adtEndDd(endDd)
                .build();
    }
}
