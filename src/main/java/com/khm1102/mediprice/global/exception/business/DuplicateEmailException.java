package com.khm1102.mediprice.global.exception.business;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
