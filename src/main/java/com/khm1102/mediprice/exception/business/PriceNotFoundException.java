package com.khm1102.mediprice.exception.business;

import com.khm1102.mediprice.exception.ErrorCode;

public class PriceNotFoundException extends BusinessException {

    public PriceNotFoundException() {
        super(ErrorCode.PRICE_NOT_FOUND);
    }
}
