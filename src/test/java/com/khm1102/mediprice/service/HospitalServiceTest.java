package com.khm1102.mediprice.service;

import com.khm1102.mediprice.dto.HospitalSummaryDto;
import com.khm1102.mediprice.repository.HospitalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    @Mock HospitalRepository repository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private HospitalService service;

    void setUp() {
        service = new HospitalService(repository, jsonMapper);
    }

    /** 프로시저가 정상 JSON 배열 반환하면 DTO 리스트로 그대로 매핑. */
    @Test
    void returnsParsedListWhenJsonValid() {
        setUp();
        String json = """
                [
                  {"ykiho":"YK1","yadmNm":"A병원","addr":"서울","clCdNm":"의원","telNo":"02-1","curAmt":50000,"distance":150.5,"lat":37.5,"lng":127.0},
                  {"ykiho":"YK2","yadmNm":"B병원","addr":"부산","clCdNm":"종합병원","telNo":"051-1","curAmt":80000,"distance":300.0,"lat":35.1,"lng":129.0}
                ]
                """;
        when(repository.searchNearbyJson(37.5, 127.0, "N001", 2000)).thenReturn(json);

        List<HospitalSummaryDto> result = service.searchNearby(37.5, 127.0, "N001", 2000);

        assertThat(result).extracting(HospitalSummaryDto::ykiho).containsExactly("YK1", "YK2");
        assertThat(result.get(0).curAmt()).isEqualTo(50_000L);
        assertThat(result.get(0).distance()).isEqualTo(150.5);
    }

    /** 프로시저가 null 반환(반경 내 결과 0건 등)이어도 빈 리스트로 안전 처리. */
    @Test
    void returnsEmptyWhenJsonNull() {
        setUp();
        when(repository.searchNearbyJson(37.5, 127.0, "N001", 2000)).thenReturn(null);

        assertThat(service.searchNearby(37.5, 127.0, "N001", 2000)).isEmpty();
    }

    /** 빈 문자열도 동일하게 빈 리스트. */
    @Test
    void returnsEmptyWhenJsonBlank() {
        setUp();
        when(repository.searchNearbyJson(37.5, 127.0, "N001", 2000)).thenReturn("   ");

        assertThat(service.searchNearby(37.5, 127.0, "N001", 2000)).isEmpty();
    }

    /** 프로시저 응답이 깨졌어도 예외 안 던지고 빈 리스트 + 로그 (장애 격리). */
    @Test
    void returnsEmptyWhenParsingFails() {
        setUp();
        when(repository.searchNearbyJson(37.5, 127.0, "N001", 2000))
                .thenReturn("{not valid json");

        assertThat(service.searchNearby(37.5, 127.0, "N001", 2000)).isEmpty();
    }
}
