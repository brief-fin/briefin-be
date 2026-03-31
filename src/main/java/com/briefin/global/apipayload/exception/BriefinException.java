package com.briefin.global.apipayload.exception;

import com.briefin.global.apipayload.code.status.ErrorCode;

import lombok.Getter;

@Getter
public class BriefinException extends RuntimeException {

    private final ErrorCode errorCode;

    public BriefinException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
