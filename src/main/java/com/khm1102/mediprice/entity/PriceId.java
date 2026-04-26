package com.khm1102.mediprice.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * {@link Price} 복합키. JPA {@code @IdClass}용. record는 기본 생성자가 없어 부적합하므로 일반 클래스.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PriceId implements Serializable {

    private String ykiho;
    private String npayCd;
}
