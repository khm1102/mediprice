package com.khm1102.mediprice.global.exception.business;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class PriceNotFoundException extends BusinessException {

    public PriceNotFoundException() {
        super(ErrorCode.PRICE_NOT_FOUND);
    }
}
