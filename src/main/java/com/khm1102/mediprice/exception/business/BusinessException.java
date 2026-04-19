package com.khm1102.mediprice.exception.business;

import com.khm1102.mediprice.exception.ErrorCode;
import com.khm1102.mediprice.exception.MediPriceException;

public class BusinessException extends MediPriceException {

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
