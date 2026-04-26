package com.khm1102.mediprice.global.exception.auth;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class TokenInvalidException extends AuthenticationException {

    public TokenInvalidException() {
        super(ErrorCode.TOKEN_INVALID);
    }
}
