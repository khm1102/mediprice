package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/**
 * 심평원 API 공통 응답 본문 — 페이징 메타데이터 + items 컬렉션.
 * <p>
 * record로 작성 시 Jackson XmlMapper의 {@code @JacksonXmlProperty(localName = "item")}이
 * record component 이름을 {@code item}으로 해석하면서 {@code items} 매개변수와 충돌해
 * {@code Could not find creator property with name 'items'} 에러 발생.
 * 따라서 일반 class + setter 패턴으로 매핑한다.
 *
 * @param <T> item 타입 (HospBasisItem, NonPayCodeItem 등)
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HiraBody<T> {

    public static <T> HiraBody<T> empty(int pageNo) {
        HiraBody<T> body = new HiraBody<>();
        body.setItems(List.of());
        body.setPageNo(pageNo);
        return body;
    }

    @JacksonXmlElementWrapper(localName = "items")
    @JacksonXmlProperty(localName = "item")
    private List<T> items;

    private int numOfRows;
    private int pageNo;
    private int totalCount;

    public List<T> safeItems() {
        return items == null ? List.of() : items;
    }
}
