package com.khm1102.mediprice.exception.auth;

import com.khm1102.mediprice.exception.ErrorCode;
import com.khm1102.mediprice.exception.MediPriceException;

public class AuthenticationException extends MediPriceException {

    public AuthenticationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public AuthenticationException(ErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
