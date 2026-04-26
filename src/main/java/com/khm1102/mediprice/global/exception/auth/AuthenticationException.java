package com.khm1102.mediprice.global.exception.auth;

import com.khm1102.mediprice.global.exception.ErrorCode;
import com.khm1102.mediprice.global.exception.MediPriceException;

public class AuthenticationException extends MediPriceException {

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
