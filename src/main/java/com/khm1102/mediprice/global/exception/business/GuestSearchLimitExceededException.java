package com.khm1102.mediprice.global.exception.business;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class GuestSearchLimitExceededException extends BusinessException {

    public GuestSearchLimitExceededException() {
        super(ErrorCode.GUEST_SEARCH_LIMIT);
    }
}
