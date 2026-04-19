package com.khm1102.mediprice.exception.business;

import com.khm1102.mediprice.exception.ErrorCode;

public class GuestSearchLimitExceededException extends BusinessException {

    public GuestSearchLimitExceededException() {
        super(ErrorCode.GUEST_SEARCH_LIMIT);
    }
}
