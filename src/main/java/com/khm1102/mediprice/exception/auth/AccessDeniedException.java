package com.khm1102.mediprice.exception.auth;

import com.khm1102.mediprice.exception.ErrorCode;

public class AccessDeniedException extends AuthenticationException {

    public AccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }
}
