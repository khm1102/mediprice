package com.khm1102.mediprice.exception.auth;

import com.khm1102.mediprice.exception.ErrorCode;

public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}
