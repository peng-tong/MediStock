package com.medistock.common.exception;

import lombok.Getter;

/** 业务异常：由 GlobalExceptionHandler 统一转为 Result */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail);
        this.code = errorCode.getCode();
    }
}
