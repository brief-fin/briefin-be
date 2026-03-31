package com.briefin.global.apipayload.code.status;

import com.briefin.global.apipayload.code.BaseCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode {
    OK(200, "COMMON200", "성공입니다.");

    private final int httpStatus;
    private final String code;
    private final String message;
}
