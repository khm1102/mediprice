package com.khm1102.mediprice.global.exception.auth;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException() {
        super(ErrorCode.TOKEN_EXPIRED);
    }
}
