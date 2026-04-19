package com.khm1102.mediprice.exception.auth;

import com.khm1102.mediprice.exception.ErrorCode;

public class TokenInvalidException extends AuthenticationException {

    public TokenInvalidException() {
        super(ErrorCode.TOKEN_INVALID);
    }
}
