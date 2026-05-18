package com.khm1102.mediprice.global.exception.business;

import com.khm1102.mediprice.global.exception.ErrorCode;

public class FavoriteAlreadyExistsException extends BusinessException {

    public FavoriteAlreadyExistsException() {
        super(ErrorCode.FAVORITE_ALREADY_EXISTS);
    }
}
