package com.khm1102.mediprice.exception.auth;

import com.khm1102.mediprice.exception.ErrorCode;

public class LoginFailedException extends AuthenticationException {

    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
