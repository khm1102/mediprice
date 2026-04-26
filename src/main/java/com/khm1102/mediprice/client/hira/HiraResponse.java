package com.khm1102.mediprice.client.hira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 심평원 API 공통 응답 래퍼.
 * <pre>
 * &lt;response&gt;
 *   &lt;header&gt;...&lt;/header&gt;
 *   &lt;body&gt;...&lt;/body&gt;
 * &lt;/response&gt;
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "response")
public record HiraResponse<T>(HiraHeader header, HiraBody<T> body) {
}
