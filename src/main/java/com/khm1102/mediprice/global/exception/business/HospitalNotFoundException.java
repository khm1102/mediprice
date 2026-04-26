package com.khm1102.mediprice.global.exception.business;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class HospitalNotFoundException extends BusinessException {

    public HospitalNotFoundException() {
        super(ErrorCode.HOSPITAL_NOT_FOUND);
    }
}
