package com.khm1102.mediprice.global.exception.auth;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class LoginFailedException extends AuthenticationException {

    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
