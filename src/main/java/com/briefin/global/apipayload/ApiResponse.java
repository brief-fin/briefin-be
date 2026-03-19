package com.briefin.global.apipayload;

import com.briefin.global.apipayload.code.BaseCode;
import com.briefin.global.apipayload.code.status.ErrorCode;
import com.briefin.global.apipayload.code.status.SuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        Boolean isSuccess,
        String code,
        String message,
        T result
) {
    public static <T> ApiResponse<T> success(T result) {
        return of(true, SuccessCode.OK, result);
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return of(false, errorCode, null);
    }

    private static <T> ApiResponse<T> of(Boolean isSuccess, BaseCode code, T result) {
        return new ApiResponse<>(isSuccess, code.getCode(), code.getMessage(), result);
    }
}
