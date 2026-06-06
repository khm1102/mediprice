package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * 심평원 API 공통 응답 본문 — 페이징 메타데이터 + items 컬렉션 + 상태.
 * <p>
 * record로 작성 시 Jackson XmlMapper의 {@code @JacksonXmlProperty(localName = "item")}이
 * record component 이름을 {@code item}으로 해석하면서 {@code items} 매개변수와 충돌해
 * {@code Could not find creator property with name 'items'} 에러 발생.
 * 따라서 일반 class + setter 패턴으로 매핑한다.
 * <p>
 * {@link Status}는 호출처(client/배치)가 "정상 응답인데 실제로 데이터 없음"과
 * "외부 호출 실패(트래픽 초과/HTTP 오류/파싱 실패 등)"를 구분할 수 있도록 도입.
 * 예전에는 둘 다 빈 body로 평탄화되어 배치가 실패를 NODATA로 오인했다.
 *
 * @param <T> item 타입 (HospBasisItem, NonPayCodeItem 등)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiraBody<T> {

    public enum Status {
        /** resultCode "00" — items가 비어 있어도 응답 자체는 정상. */
        NORMAL,
        /** resultCode "03" — HIRA가 명시한 "데이터 없음". */
        NODATA,
        /** body null, 트래픽 초과/인증/서버 오류, XML 파싱 실패, timeout 등. */
        FAILED
    }

    /** 정상 응답 + items 비어 있는 케이스를 명시할 때 사용. */
    public static <T> HiraBody<T> noData(int pageNo) {
        HiraBody<T> body = new HiraBody<>();
        body.setItems(List.of());
        body.setPageNo(pageNo);
        body.setStatus(Status.NODATA);
        return body;
    }

    /** 외부 호출/파싱 실패. 호출처에서 NODATA와 다르게 처리(실패 카운트, 재적재 등). */
    public static <T> HiraBody<T> failed(int pageNo) {
        HiraBody<T> body = new HiraBody<>();
        body.setItems(List.of());
        body.setPageNo(pageNo);
        body.setStatus(Status.FAILED);
        return body;
    }

    /** @deprecated NODATA와 FAILED를 구분하지 않는 옛 팩토리. 신규 코드는 {@link #noData}/{@link #failed} 사용. */
    @Deprecated
    public static <T> HiraBody<T> empty(int pageNo) {
        return failed(pageNo);
    }

    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    private List<T> items;

    private int numOfRows;
    private int pageNo;
    private int totalCount;

    /** 기본값 NORMAL — XML 역직렬화 시 setter가 따로 호출되지 않으면 정상 응답으로 본다. */
    private Status status = Status.NORMAL;

    public List<T> safeItems() {
        return items == null ? List.of() : items;
    }

    public boolean isNormal() {
        return status == Status.NORMAL;
    }

    public boolean isNoData() {
        return status == Status.NODATA;
    }

    public boolean isFailed() {
        return status == Status.FAILED;
    }
}
