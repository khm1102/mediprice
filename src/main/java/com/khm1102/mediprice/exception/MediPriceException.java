package com.khm1102.mediprice.exception;

import lombok.Getter;

@Getter
public abstract class MediPriceException extends RuntimeException {

    private final ErrorCode errorCode;

    protected MediPriceException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected MediPriceException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
}
